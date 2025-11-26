package com.gradproject.taskmanager.infrastructure.health;

import com.gradproject.taskmanager.modules.notification.service.EmailSenderService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for email (SendGrid) configuration.
 *
 * <p>Reports whether email notifications are properly configured
 * and can send emails. This appears in the /actuator/health endpoint
 * and helps with operational monitoring.
 *
 * <p>Health status:
 * <ul>
 *   <li>UP - Email is configured and ready to send</li>
 *   <li>DOWN - Email is not configured (missing or invalid API key)</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class EmailHealthIndicator implements HealthIndicator {

    private final EmailSenderService emailSenderService;

    @Override
    public Health health() {
        boolean configured = emailSenderService.isEmailConfigured();
        String status = emailSenderService.getConfigurationStatus();

        if (configured) {
            return Health.up()
                .withDetail("email", "SendGrid configured")
                .withDetail("status", status)
                .build();
        } else {
            return Health.down()
                .withDetail("email", "Not configured")
                .withDetail("status", status)
                .withDetail("action", "Set SENDGRID_API_KEY environment variable")
                .build();
        }
    }
}
