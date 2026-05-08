package com.chronos.chronos.scheduler;

import com.chronos.chronos.entity.Job;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.Date;
import java.util.TimeZone;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerService {

    // Quartz's main interface — we use this to schedule, pause, resume, delete triggers
    private final Scheduler scheduler;

    // Called when a new job is created via POST /api/jobs
    public void scheduleJob(Job job) {
        try {
            // JobDetail describes WHAT to run — points to our ChronosJobExecutor class
            // JobDataMap carries the jobId so ChronosJobExecutor knows which job to run
            JobDetail jobDetail = JobBuilder.newJob(ChronosJobExecutor.class)
                    .withIdentity(job.getId().toString(), "chronos-jobs")
                    .usingJobData("jobId", job.getId().toString())
                    .storeDurably()  // keep job even if no triggers
                    .build();

            Trigger trigger = buildTrigger(job);

            // Register with Quartz — it will fire the trigger at the right time
            scheduler.scheduleJob(jobDetail, trigger);
            log.info("Scheduled job {} ({})", job.getName(), job.getId());

        } catch (SchedulerException e) {
            log.error("Failed to schedule job {}: {}", job.getId(), e.getMessage());
            throw new RuntimeException("Failed to schedule job", e);
        }
    }

    // Builds the right type of trigger based on job type
    private Trigger buildTrigger(Job job) {
        TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger()
                .withIdentity(job.getId().toString(), "chronos-triggers");

        if (job.getType() == Job.Type.ONE_TIME) {
            // ONE_TIME — fire once at the scheduled datetime
            // Convert LocalDateTime to Date using the job's timezone
            Date fireAt = Date.from(
                    job.getScheduledAt()
                            .atZone(ZoneId.of(job.getTimezone()))
                            .toInstant()
            );
            return triggerBuilder
                    .startAt(fireAt)
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withRepeatCount(0)) // fire exactly once
                    .build();

        } else {
            // RECURRING — fire on a cron schedule
            // CronScheduleBuilder handles cron expressions like "0 9 * * MON"
            return triggerBuilder
                    .withSchedule(CronScheduleBuilder
                            .cronSchedule(job.getCronExpression())
                            .inTimeZone(TimeZone.getTimeZone(job.getTimezone())))
                    .build();
        }
    }

    // Called when a job is cancelled/deleted
    public void unscheduleJob(Job job) {
        try {
            scheduler.deleteJob(
                    JobKey.jobKey(job.getId().toString(), "chronos-jobs")
            );
            log.info("Unscheduled job {}", job.getId());
        } catch (SchedulerException e) {
            log.warn("Failed to unschedule job {}: {}", job.getId(), e.getMessage());
        }
    }

    // Called when a job is paused
    public void pauseJob(Job job) {
        try {
            scheduler.pauseJob(
                    JobKey.jobKey(job.getId().toString(), "chronos-jobs")
            );
            log.info("Paused job {}", job.getId());
        } catch (SchedulerException e) {
            log.warn("Failed to pause job {}: {}", job.getId(), e.getMessage());
        }
    }

    // Called when a job is resumed
    public void resumeJob(Job job) {
        try {
            scheduler.resumeJob(
                    JobKey.jobKey(job.getId().toString(), "chronos-jobs")
            );
            log.info("Resumed job {}", job.getId());
        } catch (SchedulerException e) {
            log.warn("Failed to resume job {}: {}", job.getId(), e.getMessage());
        }
    }

    // Called when a job is manually triggered
    public void triggerJobNow(Job job) {
        try {
            scheduler.triggerJob(
                    JobKey.jobKey(job.getId().toString(), "chronos-jobs")
            );
            log.info("Manually triggered job {}", job.getId());
        } catch (SchedulerException e) {
            log.warn("Failed to trigger job {}: {}", job.getId(), e.getMessage());
        }
    }
}