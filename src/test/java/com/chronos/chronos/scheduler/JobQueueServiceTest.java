package com.chronos.chronos.scheduler;

import com.chronos.chronos.entity.Job;
import com.chronos.chronos.service.JobExecutionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobQueueServiceTest {

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private JobExecutionService jobExecutionService;
    @Mock private ThreadPoolTaskExecutor jobExecutor;
    @Mock private ListOperations<String, Object> listOps;
    @Mock private ValueOperations<String, Object> valueOps;
    @InjectMocks private JobQueueService jobQueueService;

    private Job buildTestJob() {
        return Job.builder()
                .id(UUID.randomUUID())
                .name("Test Job")
                .type(Job.Type.ONE_TIME)
                .status(Job.Status.PENDING)
                .maxRetries(3)
                .retryCount(0)
                .build();
    }

    @Test
    void enqueue_ShouldPushJobToRedisQueue() {
        Job job = buildTestJob();
        when(redisTemplate.opsForList()).thenReturn(listOps);

        // Removed the unused valueOps and rightPop stubs to satisfy Mockito's strict stubbing

        jobQueueService.enqueue(job);

        // Verify job was pushed to Redis list
        verify(listOps, times(1)).leftPush(eq("chronos:job:queue"),
                eq(job.getId().toString()));
    }

    @Test
    void processQueue_ShouldSkipWhenLockAlreadyHeld() {
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(listOps.rightPop(anyString())).thenReturn("some-job-id");

        // Simulate lock already held by another worker
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(false);

        jobQueueService.processQueue();

        // Worker should NOT execute the job
        verify(jobExecutor, never()).submit(any(Runnable.class));
    }

    @Test
    void processQueue_ShouldDoNothingWhenQueueEmpty() {
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(listOps.rightPop(anyString())).thenReturn(null);

        jobQueueService.processQueue();

        // Nothing should happen
        verify(jobExecutor, never()).submit(any(Runnable.class));
    }
}