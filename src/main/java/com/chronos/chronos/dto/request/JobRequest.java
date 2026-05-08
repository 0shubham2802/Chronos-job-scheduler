package com.chronos.chronos.dto.request;

import com.chronos.chronos.entity.Job;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

// This is what the client sends when creating or updating a job
// We validate it here before it ever touches the database
@Data
public class JobRequest {

    // Job must have a name — what is this job called?
    @NotBlank(message = "Job name is required")
    private String name;

    // Optional description
    private String description;

    // ONE_TIME or RECURRING — must be provided
    @NotNull(message = "Job type is required")
    private Job.Type type;

    // For ONE_TIME jobs: when exactly should this run?
    // null for RECURRING jobs
    private LocalDateTime scheduledAt;

    // For RECURRING jobs: a cron expression like "0 9 * * MON"
    // null for ONE_TIME jobs
    private String cronExpression;

    // Timezone for the job — defaults to UTC if not provided
    // Use IANA timezone strings: "Asia/Kolkata", "America/New_York"
    private String timezone = "UTC";

    // Any extra data the job needs when it runs
    // For example: {"url": "https://api.example.com", "method": "POST"}
    private Map<String, Object> payload;

    // How many times to retry if the job fails
    // Defaults to 3 if not provided
    private Integer maxRetries = 3;
}
