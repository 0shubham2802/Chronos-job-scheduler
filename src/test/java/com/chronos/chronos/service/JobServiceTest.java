package com.chronos.chronos.service;

import com.chronos.chronos.dto.request.JobRequest;
import com.chronos.chronos.dto.response.JobResponse;
import com.chronos.chronos.dto.response.PagedResponse;
import com.chronos.chronos.entity.Job;
import com.chronos.chronos.entity.User;
import com.chronos.chronos.exception.ResourceNotFoundException;
import com.chronos.chronos.repository.JobRepository;
import com.chronos.chronos.scheduler.SchedulerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock private JobRepository jobRepository;
    @Mock private SchedulerService schedulerService;
    @Mock private MetricsService metricsService; // Required — JobService.createJob() calls metricsService.recordJobCreated()
    @InjectMocks private JobService jobService;

    private User testUser;
    private Job testJob;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .name("Shubham Pant")
                .email("shubham@chronos.com")
                .passwordHash("hashed")
                .build();

        testJob = Job.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .name("Test Job")
                .type(Job.Type.ONE_TIME)
                .status(Job.Status.PENDING)
                .scheduledAt(LocalDateTime.now().plusHours(1))
                .timezone("UTC")
                .maxRetries(3)
                .retryCount(0)
                .build();
    }

    @Test
    void createJob_ShouldReturnJobResponse() {
        JobRequest request = new JobRequest();
        request.setName("Test Job");
        request.setType(Job.Type.ONE_TIME);
        request.setScheduledAt(LocalDateTime.now().plusHours(1));

        when(jobRepository.save(any(Job.class))).thenReturn(testJob);

        JobResponse response = jobService.createJob(request, testUser);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Test Job");
        verify(jobRepository, times(1)).save(any(Job.class));
    }

    @Test
    void createJob_ShouldThrowWhenOneTimeJobHasNoScheduledAt() {
        JobRequest request = new JobRequest();
        request.setName("Bad Job");
        request.setType(Job.Type.ONE_TIME);
        // Missing scheduledAt!

        assertThatThrownBy(() -> jobService.createJob(request, testUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scheduledAt is required");
    }

    @Test
    void createJob_ShouldThrowWhenRecurringJobHasNoCronExpression() {
        JobRequest request = new JobRequest();
        request.setName("Bad Recurring");
        request.setType(Job.Type.RECURRING);
        // Missing cronExpression!

        assertThatThrownBy(() -> jobService.createJob(request, testUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cronExpression is required");
    }

    @Test
    void getJob_ShouldReturnJob() {
        when(jobRepository.findByIdAndUser(testJob.getId(), testUser))
                .thenReturn(Optional.of(testJob));

        JobResponse response = jobService.getJob(testJob.getId(), testUser);

        assertThat(response.getId()).isEqualTo(testJob.getId());
    }

    @Test
    void getJob_ShouldThrow404WhenNotFound() {
        when(jobRepository.findByIdAndUser(any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.getJob(UUID.randomUUID(), testUser))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteJob_ShouldDelete() {
        when(jobRepository.findByIdAndUser(testJob.getId(), testUser))
                .thenReturn(Optional.of(testJob));

        jobService.deleteJob(testJob.getId(), testUser);

        verify(jobRepository, times(1)).delete(testJob);
    }

    @Test
    void pauseJob_ShouldThrowForOneTimeJob() {
        when(jobRepository.findByIdAndUser(testJob.getId(), testUser))
                .thenReturn(Optional.of(testJob));
        // testJob is ONE_TIME — cannot be paused

        assertThatThrownBy(() -> jobService.pauseJob(testJob.getId(), testUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only RECURRING");
    }

    @Test
    void resumeJob_ShouldThrowWhenNotPaused() {
        when(jobRepository.findByIdAndUser(testJob.getId(), testUser))
                .thenReturn(Optional.of(testJob));
        // testJob status is PENDING, not PAUSED

        assertThatThrownBy(() -> jobService.resumeJob(testJob.getId(), testUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not paused");
    }
}