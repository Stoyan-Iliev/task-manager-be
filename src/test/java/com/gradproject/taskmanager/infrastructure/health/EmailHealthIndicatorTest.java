package com.gradproject.taskmanager.infrastructure.health;

import com.gradproject.taskmanager.modules.notification.service.EmailSenderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailHealthIndicatorTest {

    @Mock
    private EmailSenderService emailSenderService;

    @InjectMocks
    private EmailHealthIndicator emailHealthIndicator;

    @Test
    void health_whenEmailConfigured_returnsUp() {
        // Arrange
        when(emailSenderService.isEmailConfigured()).thenReturn(true);
        when(emailSenderService.getConfigurationStatus()).thenReturn("CONFIGURED: SendGrid ready");

        // Act
        Health health = emailHealthIndicator.health();

        // Assert
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("email", "SendGrid configured");
        assertThat(health.getDetails()).containsEntry("status", "CONFIGURED: SendGrid ready");
    }

    @Test
    void health_whenEmailNotConfigured_returnsDown() {
        // Arrange
        when(emailSenderService.isEmailConfigured()).thenReturn(false);
        when(emailSenderService.getConfigurationStatus()).thenReturn("NOT_CONFIGURED: No API key set");

        // Act
        Health health = emailHealthIndicator.health();

        // Assert
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("email", "Not configured");
        assertThat(health.getDetails()).containsEntry("status", "NOT_CONFIGURED: No API key set");
        assertThat(health.getDetails()).containsEntry("action", "Set SENDGRID_API_KEY environment variable");
    }

    @Test
    void health_includesConfigurationStatusInDetails() {
        // Arrange
        when(emailSenderService.isEmailConfigured()).thenReturn(true);
        when(emailSenderService.getConfigurationStatus()).thenReturn("CONFIGURED: SendGrid ready (from: test@example.com)");

        // Act
        Health health = emailHealthIndicator.health();

        // Assert
        assertThat(health.getDetails().get("status").toString()).contains("SendGrid ready");
    }
}
