package com.chronos.chronos.service;

import com.chronos.chronos.entity.ExecutionLog;
import com.chronos.chronos.entity.Job;
import com.chronos.chronos.repository.ExecutionLogRepository;
import com.chronos.chronos.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobExecutionService {

    private final JobRepository jobRepository;
    private final ExecutionLogRepository executionLogRepository;
    private final MetricsService metricsService;
    private final RetryService retryService;

    @Transactional
    public void executeJob(Job job) {
        log.info("Executing job: {} ({})", job.getName(), job.getId());

        job.setStatus(Job.Status.RUNNING);
        jobRepository.save(job);
        metricsService.incrementRunningJobs();

        LocalDateTime startedAt = LocalDateTime.now();

        try {
            simulateJobExecution(job);

            if (job.getType() == Job.Type.ONE_TIME) {
                job.setStatus(Job.Status.COMPLETED);
            } else {
                job.setStatus(Job.Status.PENDING);
            }
            job.setRetryCount(0);
            jobRepository.save(job);

            long durationMs = java.time.Duration.between(
                    startedAt, LocalDateTime.now()).toMillis();

            writeExecutionLog(job, ExecutionLog.Status.SUCCESS,
                    startedAt, null, job.getRetryCount() + 1);

            metricsService.recordJobCompleted(durationMs);
            metricsService.decrementRunningJobs();

            log.info("Job completed successfully: {}", job.getId());

        } catch (Exception e) {
            log.error("Job failed: {} — {}", job.getId(), e.getMessage());

            metricsService.decrementRunningJobs();

            writeExecutionLog(job, ExecutionLog.Status.FAILED,
                    startedAt, e.getMessage(), job.getRetryCount() + 1);

            retryService.handleFailure(job, e.getMessage());
        }
    }

    private void simulateJobExecution(Job job) throws Exception {
        log.debug("Running job payload: {}", job.getPayload());
        Thread.sleep(100);

        if (job.getPayload() != null &&
                Boolean.TRUE.equals(job.getPayload().get("fail"))) {
            throw new RuntimeException("Simulated job failure for testing");
        }
    }

    private void writeExecutionLog(Job job, ExecutionLog.Status status,
                                   LocalDateTime startedAt, String errorMessage, int attempt) {

        LocalDateTime endedAt = LocalDateTime.now();
        long durationMs = java.time.Duration.between(startedAt, endedAt).toMillis();

        ExecutionLog executionLog = ExecutionLog.builder()
                .job(job)
                .status(status)
                .attempt(attempt)
                .errorMessage(errorMessage)
                .startedAt(startedAt)
                .endedAt(endedAt)
                .durationMs(durationMs)
                .build();

        executionLogRepository.save(executionLog);
    }
}
