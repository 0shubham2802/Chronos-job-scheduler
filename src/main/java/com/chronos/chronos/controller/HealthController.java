package com.chronos.chronos.controller;

import com.chronos.chronos.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Slf4j
public class HealthController {

    private final DataSource dataSource;
    private final RedisTemplate<String, Object> redisTemplate;
    private final JobRepository jobRepository;

    // The startup time — used to calculate uptime
    private static final LocalDateTime START_TIME = LocalDateTime.now();

    // GET /api/health — basic liveness check
    // Returns 200 if the app is running, 503 if any dependency is down
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        boolean allHealthy = true;

        // Check PostgreSQL
        Map<String, Object> dbStatus = checkDatabase();
        response.put("database", dbStatus);
        if (!"UP".equals(dbStatus.get("status"))) allHealthy = false;

        // Check Redis
        Map<String, Object> redisStatus = checkRedis();
        response.put("redis", redisStatus);
        if (!"UP".equals(redisStatus.get("status"))) allHealthy = false;

        // App info
        response.put("status", allHealthy ? "UP" : "DEGRADED");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("uptime", calculateUptime());
        response.put("version", "1.0.0");

        // Return 200 if healthy, 503 if degraded
        return allHealthy
                ? ResponseEntity.ok(response)
                : ResponseEntity.status(503).body(response);
    }

    // GET /api/health/ready — readiness check (are we ready to serve traffic?)
    // Used by Kubernetes/load balancers to decide if this instance gets traffic
    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> ready() {
        Map<String, Object> response = new HashMap<>();
        try {
            // Try a simple DB query
            jobRepository.count();
            response.put("status", "READY");
            response.put("timestamp", LocalDateTime.now().toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "NOT_READY");
            response.put("reason", e.getMessage());
            return ResponseEntity.status(503).body(response);
        }
    }

    private Map<String, Object> checkDatabase() {
        Map<String, Object> status = new HashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            conn.isValid(2); // 2 second timeout
            status.put("status", "UP");
            status.put("type", "PostgreSQL");
        } catch (Exception e) {
            status.put("status", "DOWN");
            status.put("error", e.getMessage());
            log.error("Database health check failed: {}", e.getMessage());
        }
        return status;
    }

    private Map<String, Object> checkRedis() {
        Map<String, Object> status = new HashMap<>();
        try {
            String pong = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();
            status.put("status", "PONG".equals(pong) ? "UP" : "DOWN");
            status.put("type", "Redis");
        } catch (Exception e) {
            status.put("status", "DOWN");
            status.put("error", e.getMessage());
            log.error("Redis health check failed: {}", e.getMessage());
        }
        return status;
    }

    private String calculateUptime() {
        java.time.Duration uptime = java.time.Duration.between(
                START_TIME, LocalDateTime.now()
        );
        long hours = uptime.toHours();
        long minutes = uptime.toMinutesPart();
        long seconds = uptime.toSecondsPart();
        return String.format("%dh %dm %ds", hours, minutes, seconds);
    }
}
