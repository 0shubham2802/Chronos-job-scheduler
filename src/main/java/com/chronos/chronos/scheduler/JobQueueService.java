package com.chronos.chronos.scheduler;

import com.chronos.chronos.entity.Job;
import com.chronos.chronos.repository.JobRepository;
import com.chronos.chronos.service.JobExecutionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@Slf4j
public class JobQueueService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ThreadPoolTaskExecutor jobExecutor;
    private final JobRepository jobRepository;

    // @Lazy breaks the circular dependency:
    // JobQueueService → JobExecutionService → RetryService → (back)
    // Spring injects a proxy first, real bean created when first called
    @Lazy
    @Autowired
    private JobExecutionService jobExecutionService;

    private static final String JOB_QUEUE_KEY = "chronos:job:queue";
    private static final String JOB_LOCK_KEY  = "chronos:job:lock:";
    private static final Duration LOCK_TTL    = Duration.ofSeconds(30);

    @Autowired
    public JobQueueService(RedisTemplate<String, Object> redisTemplate,
                           ThreadPoolTaskExecutor jobExecutor,
                           JobRepository jobRepository) {
        this.redisTemplate = redisTemplate;
        this.jobExecutor   = jobExecutor;
        this.jobRepository = jobRepository;
    }

    public void enqueue(Job job) {
        String jobId = job.getId().toString();
        log.info("Enqueueing job {} to Redis queue", jobId);
        redisTemplate.opsForList().leftPush(JOB_QUEUE_KEY, jobId);
        processQueue();
    }

    public void processQueue() {
        Object jobIdObj = redisTemplate.opsForList().rightPop(JOB_QUEUE_KEY);
        if (jobIdObj == null) return;

        String jobId = jobIdObj.toString();

        Boolean lockAcquired = redisTemplate.opsForValue()
                .setIfAbsent(JOB_LOCK_KEY + jobId, "locked", LOCK_TTL);

        if (!Boolean.TRUE.equals(lockAcquired)) {
            log.info("Job {} already locked — skipping", jobId);
            return;
        }

        log.info("Acquired lock for job {} — executing", jobId);

        jobExecutor.submit(() -> {
            try {
                Job job = jobRepository.findById(UUID.fromString(jobId)).orElse(null);
                if (job != null) {
                    jobExecutionService.executeJob(job);
                } else {
                    log.warn("Job {} not found in DB — skipping", jobId);
                }
            } catch (Exception e) {
                log.error("Error executing job {}: {}", jobId, e.getMessage(), e);
            } finally {
                redisTemplate.delete(JOB_LOCK_KEY + jobId);
                log.info("Released lock for job {}", jobId);
            }
        });
    }
}