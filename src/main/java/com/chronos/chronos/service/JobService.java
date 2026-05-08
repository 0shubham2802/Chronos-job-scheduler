package com.chronos.chronos.service;

import com.chronos.chronos.dto.request.JobRequest;
import com.chronos.chronos.dto.response.JobResponse;
import com.chronos.chronos.dto.response.PagedResponse;
import com.chronos.chronos.entity.Job;
import com.chronos.chronos.entity.User;
import com.chronos.chronos.exception.ResourceNotFoundException;
import com.chronos.chronos.repository.JobRepository;
import com.chronos.chronos.scheduler.SchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;



@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

    private final JobRepository jobRepository;
    // Default page size — max jobs returned per request
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final SchedulerService schedulerService;
    private final MetricsService metricsService;

    // Creates a new job and saves it to the database
    // @Transactional = if anything fails, the entire operation rolls back
    @Transactional
    public JobResponse createJob(JobRequest request, User currentUser) {
        // Validate based on job type
        if (request.getType() == Job.Type.ONE_TIME && request.getScheduledAt() == null) {
            throw new IllegalArgumentException(
                    "scheduledAt is required for ONE_TIME jobs"
            );
        }
        if (request.getType() == Job.Type.RECURRING && request.getCronExpression() == null) {
            throw new IllegalArgumentException(
                    "cronExpression is required for RECURRING jobs"
            );
        }

        // Build the job entity from the request DTO
        Job job = Job.builder()
                .user(currentUser)
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .status(Job.Status.PENDING)
                .payload(request.getPayload())
                .cronExpression(request.getCronExpression())
                .scheduledAt(request.getScheduledAt())
                .timezone(request.getTimezone() != null ? request.getTimezone() : "UTC")
                .maxRetries(request.getMaxRetries() != null ? request.getMaxRetries() : 3)
                .retryCount(0)
                .build();


        Job saved = jobRepository.save(job);
        schedulerService.scheduleJob(saved);
        metricsService.recordJobCreated();
        log.info("Job created: {} for user: {}", saved.getId(), currentUser.getEmail());
        return JobResponse.from(saved);
    }

    // Returns a paginated list of jobs for the current user
    // cursor = the ID of the last job from the previous page
    // If cursor is null this is the first page
    public PagedResponse<JobResponse> getJobs(User currentUser, String cursor, int limit) {
        // Cap the limit so clients can't request 10,000 jobs at once
        int pageSize = Math.min(limit, 50);

        List<Job> jobs;
        if (cursor == null) {
            // First page — get the most recent jobs
            jobs = jobRepository.findByUserOrderByCreatedAtDesc(
                    currentUser,
                    PageRequest.of(0, pageSize + 1) // fetch one extra to check hasMore
            );
        } else {
            // Subsequent page — get jobs after the cursor
            jobs = jobRepository.findByUserAfterCursor(
                    currentUser,
                    UUID.fromString(cursor),
                    PageRequest.of(0, pageSize + 1)
            );
        }

        // If we got pageSize+1 items, there are more pages
        boolean hasMore = jobs.size() > pageSize;
        if (hasMore) {
            jobs = jobs.subList(0, pageSize); // trim the extra item
        }

        // The cursor for the next page is the ID of the last item
        String nextCursor = hasMore
                ? jobs.get(jobs.size() - 1).getId().toString()
                : null;

        List<JobResponse> items = jobs.stream()
                .map(JobResponse::from)
                .toList();

        return PagedResponse.<JobResponse>builder()
                .items(items)
                .nextCursor(nextCursor)
                .count(items.size())
                .hasMore(hasMore)
                .build();
    }

    // Gets a single job by ID — throws 404 if not found or not owned by user
    public JobResponse getJob(UUID jobId, User currentUser) {
        Job job = jobRepository.findByIdAndUser(jobId, currentUser)
                .orElseThrow(() -> ResourceNotFoundException.job(jobId));
        return JobResponse.from(job);
    }

    // Updates job fields — only updates fields that are provided (not null)
    @Transactional
    public JobResponse updateJob(UUID jobId, JobRequest request, User currentUser) {
        Job job = jobRepository.findByIdAndUser(jobId, currentUser)
                .orElseThrow(() -> ResourceNotFoundException.job(jobId));

        // Cannot update a job that is currently running
        if (job.getStatus() == Job.Status.RUNNING) {
            throw new IllegalArgumentException(
                    "Cannot update a job that is currently RUNNING"
            );
        }

        // Only update fields that were provided in the request
        if (request.getName() != null) job.setName(request.getName());
        if (request.getDescription() != null) job.setDescription(request.getDescription());
        if (request.getPayload() != null) job.setPayload(request.getPayload());
        if (request.getScheduledAt() != null) job.setScheduledAt(request.getScheduledAt());
        if (request.getCronExpression() != null) job.setCronExpression(request.getCronExpression());
        if (request.getTimezone() != null) job.setTimezone(request.getTimezone());
        if (request.getMaxRetries() != null) job.setMaxRetries(request.getMaxRetries());

        // Reset to PENDING after update so the scheduler picks it up again
        job.setStatus(Job.Status.PENDING);

        Job updated = jobRepository.save(job);
        log.info("Job updated: {}", updated.getId());
        return JobResponse.from(updated);
    }

    // Deletes a job — cascade delete removes execution logs too (set in migration)
    @Transactional
    public void deleteJob(UUID jobId, User currentUser) {
        Job job = jobRepository.findByIdAndUser(jobId, currentUser)
                .orElseThrow(() -> ResourceNotFoundException.job(jobId));
        schedulerService.unscheduleJob(job);

        jobRepository.delete(job);
        log.info("Job deleted: {}", jobId);
    }

    // Pauses a recurring job — sets status to PAUSED
    @Transactional
    public JobResponse pauseJob(UUID jobId, User currentUser) {
        Job job = jobRepository.findByIdAndUser(jobId, currentUser)
                .orElseThrow(() -> ResourceNotFoundException.job(jobId));

        if (job.getType() != Job.Type.RECURRING) {
            throw new IllegalArgumentException("Only RECURRING jobs can be paused");
        }
        if (job.getStatus() == Job.Status.PAUSED) {
            throw new IllegalArgumentException("Job is already paused");
        }

        schedulerService.pauseJob(job);

        job.setStatus(Job.Status.PAUSED);
        return JobResponse.from(jobRepository.save(job));
    }

    // Resumes a paused job — sets status back to PENDING
    @Transactional
    public JobResponse resumeJob(UUID jobId, User currentUser) {
        Job job = jobRepository.findByIdAndUser(jobId, currentUser)
                .orElseThrow(() -> ResourceNotFoundException.job(jobId));

        if (job.getStatus() != Job.Status.PAUSED) {
            throw new IllegalArgumentException("Job is not paused");
        }

        schedulerService.resumeJob(job);
        job.setStatus(Job.Status.PENDING);
        return JobResponse.from(jobRepository.save(job));
    }

    // Manually triggers a job — fires it immediately regardless of schedule
    // We just set it to PENDING with scheduledAt = now
    // The scheduler will pick it up in the next poll cycle
    @Transactional
    public JobResponse triggerJob(UUID jobId, User currentUser) {
        Job job = jobRepository.findByIdAndUser(jobId, currentUser)
                .orElseThrow(() -> ResourceNotFoundException.job(jobId));

        if (job.getStatus() == Job.Status.RUNNING) {
            throw new IllegalArgumentException("Job is already running");
        }

        schedulerService.triggerJobNow(job);

        job.setStatus(Job.Status.PENDING);
        job.setScheduledAt(java.time.LocalDateTime.now());
        return JobResponse.from(jobRepository.save(job));
    }
}
