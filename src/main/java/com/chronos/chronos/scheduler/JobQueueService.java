package com.chronos.chronos.scheduler;

import com.chronos.chronos.entity.Job;
import com.chronos.chronos.service.JobExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import com.chronos.chronos.config.ApplicationContextProvider;


import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobQueueService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final JobExecutionService jobExecutionService;
    private final ThreadPoolTaskExecutor jobExecutor;

    // The Redis key for our job queue
    // Redis List is used as a queue — LPUSH adds to left, BRPOP reads from right
    private static final String JOB_QUEUE_KEY = "chronos:job:queue";

    // Lock key pattern — one lock per job
    private static final String JOB_LOCK_KEY = "chronos:job:lock:";

    // How long a lock lives — 30 seconds
    // If a worker crashes, the lock auto-expires and another worker can pick it up
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);

    // Called by ChronosJobExecutor when Quartz fires a trigger
    // Instead of running the job directly, we push it to the Redis queue
    public void enqueue(Job job) {
        String jobId = job.getId().toString();
        log.info("Enqueueing job {} to Redis queue", jobId);

        // LPUSH adds the jobId to the left of the list
        redisTemplate.opsForList().leftPush(JOB_QUEUE_KEY, jobId);

        // After pushing, immediately try to process it
        // This is a simple approach — in production you'd have dedicated worker threads
        processQueue();
    }

    // Processes jobs from the Redis queue
    public void processQueue() {
        // RPOP takes one item from the right of the list (FIFO queue)
        Object jobIdObj = redisTemplate.opsForList().rightPop(JOB_QUEUE_KEY);

        if (jobIdObj == null) {
            return; // Queue is empty — nothing to do
        }

        String jobId = jobIdObj.toString();

        // Try to acquire the distributed lock for this job
        // SET NX EX — only sets if key doesn't exist, with TTL
        // This is atomic — only ONE worker can acquire it
        Boolean lockAcquired = redisTemplate.opsForValue()
                .setIfAbsent(JOB_LOCK_KEY + jobId, "locked", LOCK_TTL);

        if (!Boolean.TRUE.equals(lockAcquired)) {
            // Another worker already has the lock — skip this job
            log.info("Job {} is already locked by another worker — skipping", jobId);
            return;
        }

        log.info("Acquired lock for job {} — executing", jobId);

        // Submit to thread pool — this runs asynchronously
        // so we don't block the Quartz thread
        jobExecutor.submit(() -> {
            try {
                // Load job from DB and execute
                // We load fresh from DB to get latest status
                com.chronos.chronos.repository.JobRepository jobRepository =
                        ApplicationContextProvider.getBean(
                                com.chronos.chronos.repository.JobRepository.class
                        );
                Job job = jobRepository.findById(UUID.fromString(jobId)).orElse(null);
                if (job != null) {
                    jobExecutionService.executeJob(job);
                }
            } finally {
                // Always release the lock when done
                // Even if the job fails, so other workers don't get stuck
                redisTemplate.delete(JOB_LOCK_KEY + jobId);
                log.info("Released lock for job {}", jobId);
            }
        });
    }
}
