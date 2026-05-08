package com.chronos.chronos.service;

import com.chronos.chronos.entity.Job;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;

    @Value("${chronos.notification.from-email}")
    private String fromEmail;

    @Value("${chronos.notification.enabled:false}")
    private boolean notificationsEnabled;

    // Called when a job permanently fails after exhausting all retries
    public void notifyJobFailed(Job job, String errorMessage) {
        if (!notificationsEnabled) {
            log.info("Notifications disabled — skipping email for job {}",
                    job.getId());
            return;
        }

        // Get the user's email from the job
        String userEmail = job.getUser().getEmail();

        // Validate webhook URL if present — SSRF protection
        if (job.getPayload() != null &&
                job.getPayload().containsKey("webhookUrl")) {
            String webhookUrl = job.getPayload().get("webhookUrl").toString();
            if (!isWebhookUrlSafe(webhookUrl)) {
                log.warn("Blocked unsafe webhook URL for job {}: {}",
                        job.getId(), webhookUrl);
                return;
            }
            sendWebhookNotification(webhookUrl, job, errorMessage);
        } else {
            // Send email notification
            sendEmailNotification(userEmail, job, errorMessage);
        }
    }

    private void sendEmailNotification(String to, Job job, String errorMessage) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Chronos — Job Failed: " + job.getName());
            message.setText(buildEmailBody(job, errorMessage));
            mailSender.send(message);
            log.info("Failure notification sent to {} for job {}", to, job.getId());
        } catch (Exception e) {
            // Never let notification failure crash the system
            log.error("Failed to send notification email: {}", e.getMessage());
        }
    }

    private void sendWebhookNotification(String webhookUrl, Job job,
                                         String errorMessage) {
        try {
            // Send HTTP POST to the webhook URL
            // Using Java's built-in HttpClient — no extra dependency needed
            var client = java.net.http.HttpClient.newHttpClient();
            var body = String.format(
                    "{\"jobId\":\"%s\",\"jobName\":\"%s\",\"status\":\"FAILED\"," +
                            "\"error\":\"%s\",\"retries\":%d}",
                    job.getId(), job.getName(),
                    errorMessage.replace("\"", "\\\""),
                    job.getRetryCount()
            );
            var request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                    .timeout(java.time.Duration.ofSeconds(10))
                    .build();
            client.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.discarding());
            log.info("Webhook notification sent to {} for job {}", webhookUrl, job.getId());
        } catch (Exception e) {
            log.error("Failed to send webhook notification: {}", e.getMessage());
        }
    }

    // SSRF Protection — blocks requests to private/internal IP ranges
    // Prevents attackers from using Chronos to probe internal services
    private boolean isWebhookUrlSafe(String url) {
        try {
            java.net.URI uri = java.net.URI.create(url);

            // Only allow HTTPS — never HTTP
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                log.warn("Webhook rejected — must use HTTPS: {}", url);
                return false;
            }

            String host = uri.getHost();
            if (host == null) return false;

            // Block private IP ranges and localhost
            // These are internal network addresses that should never be called
            java.net.InetAddress address = java.net.InetAddress.getByName(host);
            if (address.isLoopbackAddress() ||      // 127.x.x.x
                    address.isSiteLocalAddress() ||      // 10.x, 172.16-31.x, 192.168.x
                    address.isLinkLocalAddress() ||      // 169.254.x
                    address.isAnyLocalAddress()) {       // 0.0.0.0
                log.warn("Webhook rejected — private IP blocked: {}", host);
                return false;
            }

            return true;
        } catch (Exception e) {
            log.warn("Webhook URL validation failed: {}", e.getMessage());
            return false;
        }
    }

    private String buildEmailBody(Job job, String errorMessage) {
        return String.format("""
            Your Chronos job has failed permanently.

            Job Details:
            ─────────────────────────
            Name:        %s
            ID:          %s
            Type:        %s
            Retries:     %d/%d attempted
            Final error: %s

            The job has been marked as FAILED and will not retry again.
            Log in to your Chronos dashboard to review the execution logs
            and reschedule if needed.

            — The Chronos Team
            """,
                job.getName(),
                job.getId(),
                job.getType(),
                job.getRetryCount(),
                job.getMaxRetries(),
                errorMessage
        );
    }
}