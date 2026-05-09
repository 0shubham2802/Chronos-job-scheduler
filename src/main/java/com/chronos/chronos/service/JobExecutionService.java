package com.chronos.chronos.service;

import com.chronos.chronos.entity.ExecutionLog;
import com.chronos.chronos.entity.Job;
import com.chronos.chronos.repository.ExecutionLogRepository;
import com.chronos.chronos.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobExecutionService {

    // These are injected by @RequiredArgsConstructor (Lombok)
    private final JobRepository jobRepository;
    private final ExecutionLogRepository executionLogRepository;
    private final MetricsService metricsService;
    private final RetryService retryService;

    // @Autowired handles this separately — NOT final, so Lombok ignores it
    @Autowired
    private JavaMailSender mailSender;

    // @Value cannot be used on final fields — must be non-final
    @Value("${spring.mail.username}")
    private String fromEmail;

    @Transactional
    public void executeJob(Job job) {
        log.info("Executing job: {} ({})", job.getName(), job.getId());

        job.setStatus(Job.Status.RUNNING);
        jobRepository.save(job);
        metricsService.incrementRunningJobs();

        LocalDateTime startedAt = LocalDateTime.now();

        try {
            // Capture attempt number BEFORE resetting retryCount
            int attemptNumber = job.getRetryCount() + 1;

            simulateJobExecution(job);

            if (job.getType() == Job.Type.ONE_TIME) {
                job.setStatus(Job.Status.COMPLETED);
            } else {
                job.setStatus(Job.Status.PENDING);
            }

            // Reset retry count after success
            job.setRetryCount(0);
            jobRepository.save(job);

            long durationMs = java.time.Duration.between(
                    startedAt, LocalDateTime.now()).toMillis();

            // Use captured attemptNumber (not job.getRetryCount() which is now 0)
            writeExecutionLog(job, ExecutionLog.Status.SUCCESS,
                    startedAt, null, attemptNumber);

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
        log.debug("Executing job payload: {}", job.getPayload());

        // If payload has "to" field — send a real email
        if (job.getPayload() != null && job.getPayload().containsKey("to")) {
            sendEmailFromPayload(job);
            return;
        }

        // Default simulation — sleep 100ms to mimic work
        Thread.sleep(100);

        // If payload has "fail": true — throw to test retry behaviour
        if (job.getPayload() != null &&
                Boolean.TRUE.equals(job.getPayload().get("fail"))) {
            throw new RuntimeException("Simulated failure for testing");
        }
    }

    private void sendEmailFromPayload(Job job) throws Exception {
        Map<String, Object> payload = job.getPayload();

        String to      = (String) payload.get("to");
        String subject = (String) payload.getOrDefault("subject", "Scheduled email from Chronos");
        String body    = (String) payload.getOrDefault("body",
                "This email was sent by your Chronos job: " + job.getName());

        if (to == null || to.isBlank()) {
            throw new RuntimeException("Payload missing 'to' email address");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
        log.info("Email sent to {} for job {}", to, job.getId());
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