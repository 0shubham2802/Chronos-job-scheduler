package com.chronos.chronos.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Rate limiter — prevents a single user from spamming the API
// Each user gets their own "bucket" of tokens
// The bucket refills at a fixed rate — like a token bucket algorithm
@Component
public class RateLimiterConfig {

    // One bucket per user — stored in memory
    // ConcurrentHashMap is thread-safe — safe for concurrent requests
    private final Map<UUID, Bucket> buckets = new ConcurrentHashMap<>();

    // Get or create a bucket for a user
    public Bucket resolveBucket(UUID userId) {
        return buckets.computeIfAbsent(userId, this::newBucket);
    }

    private Bucket newBucket(UUID userId) {
        // Each user can make 30 requests per minute
        // If they exceed this, they get 429 Too Many Requests
        Bandwidth limit = Bandwidth.builder()
                .capacity(30)                     // max 30 tokens in bucket
                .refillGreedy(30, Duration.ofMinutes(1)) // refill 30 per minute
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
