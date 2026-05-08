package com.chronos.chronos.service;

import com.chronos.chronos.entity.Job;
import com.chronos.chronos.repository.JobRepository;
import com.chronos.chronos.scheduler.SchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetryService {

    private final JobRepository jobRepository;
    private final NotificationService notificationService;
    private final SchedulerService schedulerService;
    private final MetricsService metricsService;

    @Value("${chronos.retry.initial-delay-ms:1000}")
    private long initialDelayMs;

    @Value("${chronos.retry.multiplier:2.0}")
    private double multiplier;

    @Value("${chronos.retry.max-delay-ms:60000}")
    private long maxDelayMs;

    // Called when a job fails
    // Decides whether to retry or mark as permanently FAILED
    @Transactional
    public void handleFailure(Job job, String errorMessage) {
        int currentRetries = job.getRetryCount();
        int maxRetries = job.getMaxRetries();

        log.warn("Job {} failed (attempt {}/{}): {}",
                job.getId(), currentRetries + 1, maxRetries, errorMessage);

        if (currentRetries < maxRetries) {
            // Still has retries left — schedule a retry
            long delayMs = calculateDelay(currentRetries);
            log.info("Scheduling retry {} for job {} in {}ms",
                    currentRetries + 1, job.getId(), delayMs);

            job.setRetryCount(currentRetries + 1);
            job.setStatus(Job.Status.PENDING);

            jobRepository.save(job);

            metricsService.recordRetry();

            // Reschedule in Quartz with the delay
            scheduleRetry(job, delayMs);

        } else {
            // Exhausted all retries — mark as FAILED permanently
            log.error("Job {} exhausted all {} retries — marking FAILED",
                    job.getId(), maxRetries);

            job.setStatus(Job.Status.FAILED);
            jobRepository.save(job);
            metricsService.recordJobFailed();

            // Notify the user
            notificationService.notifyJobFailed(job, errorMessage);
        }
    }

    // Exponential backoff: delay = initialDelay × multiplier^retryCount
    // Retry 0: 1000ms × 2^0 = 1s
    // Retry 1: 1000ms × 2^1 = 2s
    // Retry 2: 1000ms × 2^2 = 4s
    // Retry 3: 1000ms × 2^3 = 8s
    public long calculateDelay(int retryCount) {
        long delay = (long) (initialDelayMs * Math.pow(multiplier, retryCount));
        // Cap at maxDelayMs so retries don't wait longer than 60 seconds
        return Math.min(delay, maxDelayMs);
    }

    private void scheduleRetry(Job job, long delayMs) {
        // Schedule the job to run again after the delay
        // We update scheduledAt and let Quartz pick it up
        java.time.LocalDateTime retryAt = java.time.LocalDateTime.now()
                .plusNanos(delayMs * 1_000_000);
        job.setScheduledAt(retryAt);
        jobRepository.save(job);
        schedulerService.scheduleJob(job);
    }
}