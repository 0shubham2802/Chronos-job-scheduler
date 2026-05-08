package com.chronos.chronos.repository;

import com.chronos.chronos.entity.ExecutionLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExecutionLogRepository extends JpaRepository<ExecutionLog, UUID> {

    // Find all logs for a job ordered by most recent first
    List<ExecutionLog> findByJobIdOrderByStartedAtDesc(UUID jobId, Pageable pageable);

    // Count total executions for a job
    long countByJobId(UUID jobId);
}
