package com.chronos.chronos.scheduler;

import com.chronos.chronos.entity.Job;
import com.chronos.chronos.repository.JobRepository;
import com.chronos.chronos.service.JobExecutionService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.util.UUID;

// QuartzJobBean is Spring's wrapper around Quartz's Job interface
// It allows Spring to inject dependencies into Quartz job instances
// which Quartz normally can't do on its own
@Slf4j
@Component
public class ChronosJobExecutor extends QuartzJobBean {

    // We can't use constructor injection here because Quartz
    // creates this class itself — so we use field injection
    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobQueueService jobQueueService;

    @Override
    protected void executeInternal(JobExecutionContext context)
            throws JobExecutionException {

        // JobDataMap is how Quartz passes data to us when the trigger fires
        // We stored the jobId when we created the trigger — now we read it back
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        String jobIdStr = dataMap.getString("jobId");

        log.info("Quartz fired trigger for job: {}", jobIdStr);

        try {
            UUID jobId = UUID.fromString(jobIdStr);

            // Load the job from our database
            Job job = jobRepository.findById(jobId).orElse(null);

            if (job == null) {
                log.warn("Job not found in DB: {} — skipping", jobIdStr);
                return;
            }

            // Skip if job was cancelled or already running
            if (job.getStatus() == Job.Status.CANCELLED ||
                    job.getStatus() == Job.Status.RUNNING) {
                log.info("Job {} is {} — skipping execution", jobIdStr, job.getStatus());
                return;
            }

            // Hand off to execution service
            // This pushes the job to Redis queue (Module 5)
            // For now it will execute directly
            jobQueueService.enqueue(job);

        } catch (Exception e) {
            log.error("Error executing job {}: {}", jobIdStr, e.getMessage(), e);
            throw new JobExecutionException(e);
        }
    }
}
