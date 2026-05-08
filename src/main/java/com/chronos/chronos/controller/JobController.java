package com.chronos.chronos.controller;

import com.chronos.chronos.config.RateLimiterConfig;
import com.chronos.chronos.dto.request.JobRequest;
import com.chronos.chronos.dto.response.JobResponse;
import com.chronos.chronos.dto.response.PagedResponse;
import com.chronos.chronos.entity.User;
import com.chronos.chronos.security.SecurityUtils;
import com.chronos.chronos.service.JobService;
import io.github.bucket4j.Bucket;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

import com.chronos.chronos.repository.ExecutionLogRepository;
import com.chronos.chronos.entity.ExecutionLog;
import org.springframework.data.domain.PageRequest;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final RateLimiterConfig rateLimiterConfig;
    private final SecurityUtils securityUtils;
    private final ExecutionLogRepository executionLogRepository;

    private ResponseEntity<?> checkRateLimit(User user) {
        Bucket bucket = rateLimiterConfig.resolveBucket(user.getId());
        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many requests. Please slow down."));
        }
        return null;
    }

    @PostMapping
    public ResponseEntity<?> createJob(@Valid @RequestBody JobRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        ResponseEntity<?> rateLimit = checkRateLimit(currentUser);
        if (rateLimit != null) return rateLimit;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jobService.createJob(request, currentUser));
    }

    @GetMapping
    public ResponseEntity<?> getJobs(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        User currentUser = securityUtils.getCurrentUser();
        ResponseEntity<?> rateLimit = checkRateLimit(currentUser);
        if (rateLimit != null) return rateLimit;
        return ResponseEntity.ok(jobService.getJobs(currentUser, cursor, limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getJob(@PathVariable UUID id) {
        User currentUser = securityUtils.getCurrentUser();
        ResponseEntity<?> rateLimit = checkRateLimit(currentUser);
        if (rateLimit != null) return rateLimit;
        return ResponseEntity.ok(jobService.getJob(id, currentUser));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateJob(
            @PathVariable UUID id,
            @RequestBody JobRequest request) {
        User currentUser = securityUtils.getCurrentUser();
        ResponseEntity<?> rateLimit = checkRateLimit(currentUser);
        if (rateLimit != null) return rateLimit;
        return ResponseEntity.ok(jobService.updateJob(id, request, currentUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteJob(@PathVariable UUID id) {
        User currentUser = securityUtils.getCurrentUser();
        ResponseEntity<?> rateLimit = checkRateLimit(currentUser);
        if (rateLimit != null) return rateLimit;
        jobService.deleteJob(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<?> pauseJob(@PathVariable UUID id) {
        User currentUser = securityUtils.getCurrentUser();
        ResponseEntity<?> rateLimit = checkRateLimit(currentUser);
        if (rateLimit != null) return rateLimit;
        return ResponseEntity.ok(jobService.pauseJob(id, currentUser));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<?> resumeJob(@PathVariable UUID id) {
        User currentUser = securityUtils.getCurrentUser();
        ResponseEntity<?> rateLimit = checkRateLimit(currentUser);
        if (rateLimit != null) return rateLimit;
        return ResponseEntity.ok(jobService.resumeJob(id, currentUser));
    }

    @PostMapping("/{id}/trigger")
    public ResponseEntity<?> triggerJob(@PathVariable UUID id) {
        User currentUser = securityUtils.getCurrentUser();
        ResponseEntity<?> rateLimit = checkRateLimit(currentUser);
        if (rateLimit != null) return rateLimit;
        return ResponseEntity.ok(jobService.triggerJob(id, currentUser));
    }


    @GetMapping("/{id}/logs")
    public ResponseEntity<?> getJobLogs(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "20") int limit) {

        User currentUser = securityUtils.getCurrentUser();
        ResponseEntity<?> rateLimit = checkRateLimit(currentUser);
        if (rateLimit != null) return rateLimit;

        // Verify job belongs to this user
        jobService.getJob(id, currentUser); // throws 404 if not found/owned

        // Fetch logs ordered by most recent first
        var logs = executionLogRepository
                .findByJobIdOrderByStartedAtDesc(id, PageRequest.of(0, limit));

        // Map to response format
        var response = logs.stream().map(log -> Map.of(
                "id", log.getId().toString(),
                "status", log.getStatus().toString(),
                "attempt", log.getAttempt(),
                "startedAt", log.getStartedAt() != null ? log.getStartedAt().toString() : "",
                "endedAt", log.getEndedAt() != null ? log.getEndedAt().toString() : "",
                "durationMs", log.getDurationMs() != null ? log.getDurationMs() : 0,
                "errorMessage", log.getErrorMessage() != null ? log.getErrorMessage() : ""
        )).toList();

        return ResponseEntity.ok(Map.of(
                "jobId", id.toString(),
                "logs", response,
                "count", response.size()
        ));
    }
}