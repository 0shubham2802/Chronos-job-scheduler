package com.chronos.chronos.dto.response;

import com.chronos.chronos.entity.Job;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

// This is what we send BACK to the client
// Notice we never expose the full User object — just the userId
// This prevents leaking sensitive user data in job responses
@Data
@Builder
public class JobResponse {

    private UUID id;
    private UUID userId;
    private String name;
    private String description;
    private Job.Type type;
    private Job.Status status;
    private Map<String, Object> payload;
    private String cronExpression;
    private LocalDateTime scheduledAt;
    private String timezone;
    private Integer maxRetries;
    private Integer retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Static factory — converts a Job entity into a JobResponse DTO
    // We use this in the service layer to avoid exposing entities directly
    public static JobResponse from(Job job) {
        return JobResponse.builder()
                .id(job.getId())
                .userId(job.getUser().getId())
                .name(job.getName())
                .description(job.getDescription())
                .type(job.getType())
                .status(job.getStatus())
                .payload(job.getPayload())
                .cronExpression(job.getCronExpression())
                .scheduledAt(job.getScheduledAt())
                .timezone(job.getTimezone())
                .maxRetries(job.getMaxRetries())
                .retryCount(job.getRetryCount())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }
}
