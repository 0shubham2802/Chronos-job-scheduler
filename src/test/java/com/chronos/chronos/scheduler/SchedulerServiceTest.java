package com.chronos.chronos.scheduler;

import com.chronos.chronos.entity.Job;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.*;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulerServiceTest {

    @Mock private Scheduler scheduler;
    @InjectMocks private SchedulerService schedulerService;

    private Job buildOneTimeJob() {
        return Job.builder()
                .id(UUID.randomUUID())
                .name("Test Job")
                .type(Job.Type.ONE_TIME)
                .status(Job.Status.PENDING)
                .scheduledAt(LocalDateTime.now().plusHours(1))
                .timezone("UTC")
                .maxRetries(3)
                .retryCount(0)
                .build();
    }

    private Job buildRecurringJob() {
        return Job.builder()
                .id(UUID.randomUUID())
                .name("Recurring Job")
                .type(Job.Type.RECURRING)
                .status(Job.Status.PENDING)
                // Replaced Unix cron with a valid 6-part Quartz cron expression
                .cronExpression("0 0 9 * * ?")
                .timezone("Asia/Kolkata")
                .maxRetries(3)
                .retryCount(0)
                .build();
    }

    @Test
    void scheduleOneTimeJob_ShouldCallQuartzScheduler() throws Exception {
        Job job = buildOneTimeJob();

        schedulerService.scheduleJob(job);

        // Verify Quartz was told to schedule this job
        verify(scheduler, times(1)).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }

    @Test
    void scheduleRecurringJob_ShouldCallQuartzScheduler() throws Exception {
        Job job = buildRecurringJob();

        schedulerService.scheduleJob(job);

        verify(scheduler, times(1)).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }

    @Test
    void unscheduleJob_ShouldDeleteFromQuartz() throws Exception {
        Job job = buildOneTimeJob();

        schedulerService.unscheduleJob(job);

        verify(scheduler, times(1)).deleteJob(any(JobKey.class));
    }

    @Test
    void pauseJob_ShouldPauseInQuartz() throws Exception {
        Job job = buildOneTimeJob();

        schedulerService.pauseJob(job);

        verify(scheduler, times(1)).pauseJob(any(JobKey.class));
    }

    @Test
    void resumeJob_ShouldResumeInQuartz() throws Exception {
        Job job = buildOneTimeJob();

        schedulerService.resumeJob(job);

        verify(scheduler, times(1)).resumeJob(any(JobKey.class));
    }

    @Test
    void scheduleJob_ShouldNotThrowOnValidJob() {
        Job job = buildOneTimeJob();

        assertThatCode(() -> schedulerService.scheduleJob(job))
                .doesNotThrowAnyException();
    }
}