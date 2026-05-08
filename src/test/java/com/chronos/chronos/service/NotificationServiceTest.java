package com.chronos.chronos.service;

import com.chronos.chronos.entity.Job;
import com.chronos.chronos.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private JavaMailSender mailSender;
    @InjectMocks private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationService, "fromEmail",
                "notifications@chronos.com");
    }

    private Job buildFailedJob() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Shubham Pant")
                .email("shubham@chronos.com")
                .passwordHash("hashed")
                .build();
        return Job.builder()
                .id(UUID.randomUUID())
                .user(user)
                .name("Failed Job")
                .type(Job.Type.ONE_TIME)
                .status(Job.Status.FAILED)
                .retryCount(3)
                .maxRetries(3)
                .build();
    }

    @Test
    void notifyJobFailed_ShouldSendEmailWhenEnabled() {
        ReflectionTestUtils.setField(notificationService,
                "notificationsEnabled", true);
        Job job = buildFailedJob();

        notificationService.notifyJobFailed(job, "Connection refused");

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void notifyJobFailed_ShouldSkipWhenDisabled() {
        ReflectionTestUtils.setField(notificationService,
                "notificationsEnabled", false);
        Job job = buildFailedJob();

        notificationService.notifyJobFailed(job, "Connection refused");

        // No email should be sent
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void notifyJobFailed_ShouldBlockUnsafeWebhookUrls() {
        ReflectionTestUtils.setField(notificationService,
                "notificationsEnabled", true);
        Job job = buildFailedJob();

        // Add an unsafe internal webhook URL to the payload
        job.setPayload(Map.of("webhookUrl", "http://192.168.1.1/internal"));

        notificationService.notifyJobFailed(job, "Error");

        // Should block it — no email or webhook sent
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
}
