package com.chronos.chronos.repository;

import com.chronos.chronos.entity.Job;
import com.chronos.chronos.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {

    // Find all jobs for a user — used in GET /api/jobs
    // Pageable lets us limit results and sort them
    List<Job> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    // Cursor pagination query — finds jobs created BEFORE a certain ID
    // This is how we implement "give me the next page" without page numbers
    @Query("SELECT j FROM Job j WHERE j.user = :user " +
            "AND j.createdAt < (SELECT j2.createdAt FROM Job j2 WHERE j2.id = :cursor) " +
            "ORDER BY j.createdAt DESC")
    List<Job> findByUserAfterCursor(
            @Param("user") User user,
            @Param("cursor") UUID cursor,
            Pageable pageable
    );

    // Find a specific job — but ONLY if it belongs to this user
    // This is the ownership check — prevents user A from seeing user B's jobs
    Optional<Job> findByIdAndUser(UUID id, User user);

    // Check if a job belongs to a user — used for quick ownership validation
    boolean existsByIdAndUser(UUID id, User user);

    // Count total jobs for a user — useful for dashboard stats
    long countByUser(User user);
}
