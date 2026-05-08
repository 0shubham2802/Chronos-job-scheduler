package com.chronos.chronos.service;

import com.chronos.chronos.repository.ExecutionLogRepository;
import com.chronos.chronos.repository.JobRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetricsService {

    private final MeterRegistry meterRegistry;
    private final JobRepository jobRepository;
    private final ExecutionLogRepository executionLogRepository;

    // Counters increment monotonically — good for tracking totals
    private Counter jobsCreatedCounter;
    private Counter jobsCompletedCounter;
    private Counter jobsFailedCounter;
    private Counter retriesCounter;
    private Counter notificationsSentCounter;

    // Timer tracks how long job executions take
    private Timer jobExecutionTimer;

    // Atomic values for gauges — gauges show current state (not cumulative)
    private final AtomicLong activeJobsCount = new AtomicLong(0);

    // @PostConstruct runs after Spring creates this bean
    // We register all our custom metrics here
    @PostConstruct
    public void initMetrics() {
        // Counters — monotonically increasing values
        jobsCreatedCounter = Counter.builder("chronos_jobs_created_total")
                .description("Total jobs created by users")
                .register(meterRegistry);

        jobsCompletedCounter = Counter.builder("chronos_jobs_completed_total")
                .description("Total jobs completed successfully")
                .register(meterRegistry);

        jobsFailedCounter = Counter.builder("chronos_jobs_failed_total")
                .description("Total jobs that failed permanently")
                .register(meterRegistry);

        retriesCounter = Counter.builder("chronos_job_retries_total")
                .description("Total retry attempts across all jobs")
                .register(meterRegistry);

        notificationsSentCounter = Counter.builder("chronos_notifications_sent_total")
                .description("Total failure notifications sent")
                .register(meterRegistry);

        // Timer — tracks p50/p95/p99 latency of job executions
        jobExecutionTimer = Timer.builder("chronos_job_execution_duration")
                .description("Time taken to execute a job")
                .register(meterRegistry);

        // Gauge — shows current number of RUNNING jobs
        // Gauge uses a supplier lambda so it always reflects the current value
        Gauge.builder("chronos_jobs_running", activeJobsCount, AtomicLong::get)
                .description("Number of jobs currently running")
                .register(meterRegistry);
    }

    // Called by JobService when a job is created
    public void recordJobCreated() {
        jobsCreatedCounter.increment();
    }

    // Called by JobExecutionService on success
    public void recordJobCompleted(long durationMs) {
        jobsCompletedCounter.increment();
        jobExecutionTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    // Called by RetryService on permanent failure
    public void recordJobFailed() {
        jobsFailedCounter.increment();
    }

    // Called by RetryService on each retry attempt
    public void recordRetry() {
        retriesCounter.increment();
    }

    // Called by NotificationService
    public void recordNotificationSent() {
        notificationsSentCounter.increment();
    }

    public void incrementRunningJobs() {
        activeJobsCount.incrementAndGet();
    }

    public void decrementRunningJobs() {
        activeJobsCount.decrementAndGet();
    }
}
