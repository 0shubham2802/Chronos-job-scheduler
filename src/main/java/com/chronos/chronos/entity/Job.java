package com.chronos.chronos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    // These enums match the PostgreSQL ENUM types we created in V2 migration
    // They must have the exact same values
    public enum Type { ONE_TIME, RECURRING }
    public enum Status { PENDING, RUNNING, COMPLETED, FAILED, CANCELLED, PAUSED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    // @ManyToOne = many jobs can belong to one user
    // @JoinColumn tells JPA which column is the foreign key
    // LAZY means: don't load the User object from DB unless we explicitly ask for it
    // This improves performance — most job queries don't need user details
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // @Enumerated(STRING) stores the enum as text ("ONE_TIME") in the DB
    // not as a number (0, 1) — much more readable and safer
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.PENDING;

    // @JdbcTypeCode(SqlTypes.JSON) tells Hibernate this is a JSON column
    // Map<String, Object> can hold any JSON structure
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "cron_expression", length = 100)
    private String cronExpression;

    @Column(name = "timezone", nullable = false, length = 64)
    @Builder.Default
    private String timezone = "UTC";

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "max_retries", nullable = false)
    @Builder.Default
    private Integer maxRetries = 3;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
