package com.chronos.chronos.service;

import com.chronos.chronos.entity.Job;
import com.chronos.chronos.repository.JobRepository;
import com.chronos.chronos.scheduler.SchedulerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetryServiceTest {

    @Mock private JobRepository jobRepository;
    @Mock private NotificationService notificationService;
    @Mock private SchedulerService schedulerService;
    @Mock private MetricsService metricsService; // Required — RetryService.handleFailure() calls recordRetry()/recordJobFailed()
    @InjectMocks private RetryService retryService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(retryService, "initialDelayMs", 1000L);
        ReflectionTestUtils.setField(retryService, "multiplier", 2.0);
        ReflectionTestUtils.setField(retryService, "maxDelayMs", 60000L);
    }

    private Job buildJob(int retryCount, int maxRetries) {
        return Job.builder()
                .id(UUID.randomUUID())
                .name("Test Job")
                .type(Job.Type.ONE_TIME)
                .status(Job.Status.RUNNING)
                .retryCount(retryCount)
                .maxRetries(maxRetries)
                .timezone("UTC")
                .scheduledAt(java.time.LocalDateTime.now().plusHours(1))
                .build();
    }

    @Test
    void calculateDelay_ShouldDoubleEachRetry() {
        assertThat(retryService.calculateDelay(0)).isEqualTo(1000L); // 1s
        assertThat(retryService.calculateDelay(1)).isEqualTo(2000L); // 2s
        assertThat(retryService.calculateDelay(2)).isEqualTo(4000L); // 4s
        assertThat(retryService.calculateDelay(3)).isEqualTo(8000L); // 8s
    }

    @Test
    void calculateDelay_ShouldCapAtMaxDelay() {
        // Very high retry count — should be capped at 60 seconds
        long delay = retryService.calculateDelay(100);
        assertThat(delay).isEqualTo(60000L);
    }

    @Test
    void handleFailure_ShouldRetryWhenRetriesRemaining() {
        Job job = buildJob(0, 3); // 0 retries used, 3 max
        when(jobRepository.save(any())).thenReturn(job);

        retryService.handleFailure(job, "Connection timeout");

        // Job should be rescheduled, not marked failed
        assertThat(job.getStatus()).isEqualTo(Job.Status.PENDING);
        assertThat(job.getRetryCount()).isEqualTo(1);
        verify(notificationService, never()).notifyJobFailed(any(), any());
        verify(schedulerService, times(1)).scheduleJob(any());
    }

    @Test
    void handleFailure_ShouldMarkFailedWhenRetriesExhausted() {
        Job job = buildJob(3, 3); // 3 retries used, 3 max — exhausted
        when(jobRepository.save(any())).thenReturn(job);

        retryService.handleFailure(job, "Permanent failure");

        // Job should be FAILED and user notified
        assertThat(job.getStatus()).isEqualTo(Job.Status.FAILED);
        verify(notificationService, times(1)).notifyJobFailed(job, "Permanent failure");
        verify(schedulerService, never()).scheduleJob(any());
    }

    @Test
    void handleFailure_ShouldNotRetryWhenMaxRetriesIsZero() {
        Job job = buildJob(0, 0); // 0 max retries — fail immediately
        when(jobRepository.save(any())).thenReturn(job);

        retryService.handleFailure(job, "Instant failure");

        assertThat(job.getStatus()).isEqualTo(Job.Status.FAILED);
        verify(notificationService, times(1)).notifyJobFailed(any(), any());
    }
}
