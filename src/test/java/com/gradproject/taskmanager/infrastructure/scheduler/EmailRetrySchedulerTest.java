package com.gradproject.taskmanager.infrastructure.scheduler;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.notification.domain.EmailNotificationQueue;
import com.gradproject.taskmanager.modules.notification.domain.NotificationType;
import com.gradproject.taskmanager.modules.notification.repository.EmailNotificationQueueRepository;
import com.gradproject.taskmanager.modules.organization.domain.Organization;
import com.gradproject.taskmanager.modules.project.domain.Project;
import com.gradproject.taskmanager.modules.project.domain.TaskStatus;
import com.gradproject.taskmanager.modules.task.domain.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailRetrySchedulerTest {

    @Mock
    private EmailNotificationQueueRepository emailQueueRepository;

    private EmailRetryScheduler emailRetryScheduler;

    private static final int MAX_RETRY_ATTEMPTS = 3;

    private Organization organization;
    private Project project;
    private TaskStatus taskStatus;
    private Task task;
    private User recipient;

    @BeforeEach
    void setUp() {
        emailRetryScheduler = new EmailRetryScheduler(
            emailQueueRepository,
            MAX_RETRY_ATTEMPTS
        );

        // Set up test data
        organization = new Organization();
        organization.setId(1L);
        organization.setName("Test Org");

        project = new Project();
        project.setId(1L);
        project.setKey("PROJ");
        project.setOrganization(organization);

        taskStatus = new TaskStatus();
        taskStatus.setId(1L);
        taskStatus.setName("To Do");

        task = new Task();
        task.setId(1L);
        task.setKey("PROJ-123");
        task.setTitle("Test Task");
        task.setOrganization(organization);
        task.setProject(project);
        task.setStatus(taskStatus);

        recipient = new User();
        recipient.setId(1);
        recipient.setUsername("recipient");
        recipient.setEmail("recipient@example.com");
    }

    @Test
    void processFailedNotifications_noFailedNotifications_doesNothing() {
        // Arrange
        when(emailQueueRepository.findFailedNotificationsForRetry(anyInt(), any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());

        // Act
        emailRetryScheduler.processFailedNotifications();

        // Assert
        verify(emailQueueRepository, never()).resetForRetry(anyList());
    }

    @Test
    void processFailedNotifications_withEligibleNotifications_resetsToending() {
        // Arrange
        EmailNotificationQueue failed1 = createFailedNotification(1L, 1);
        EmailNotificationQueue failed2 = createFailedNotification(2L, 2);

        when(emailQueueRepository.findFailedNotificationsForRetry(anyInt(), any(LocalDateTime.class)))
            .thenReturn(List.of(failed1, failed2));

        // Act
        emailRetryScheduler.processFailedNotifications();

        // Assert
        ArgumentCaptor<List<Long>> idsCaptor = ArgumentCaptor.forClass(List.class);
        verify(emailQueueRepository).resetForRetry(idsCaptor.capture());
        assertThat(idsCaptor.getValue()).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void processFailedNotifications_usesMaxRetryAttemptsFromConfig() {
        // Arrange
        when(emailQueueRepository.findFailedNotificationsForRetry(anyInt(), any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());

        // Act
        emailRetryScheduler.processFailedNotifications();

        // Assert
        ArgumentCaptor<Integer> maxAttemptsCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(emailQueueRepository).findFailedNotificationsForRetry(
            maxAttemptsCaptor.capture(),
            any(LocalDateTime.class)
        );
        assertThat(maxAttemptsCaptor.getValue()).isEqualTo(MAX_RETRY_ATTEMPTS);
    }

    @Test
    void processFailedNotifications_usesCurrentTimeForQuery() {
        // Arrange
        LocalDateTime beforeCall = LocalDateTime.now();
        when(emailQueueRepository.findFailedNotificationsForRetry(anyInt(), any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());

        // Act
        emailRetryScheduler.processFailedNotifications();
        LocalDateTime afterCall = LocalDateTime.now();

        // Assert
        ArgumentCaptor<LocalDateTime> timeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(emailQueueRepository).findFailedNotificationsForRetry(
            anyInt(),
            timeCaptor.capture()
        );

        LocalDateTime capturedTime = timeCaptor.getValue();
        assertThat(capturedTime).isAfterOrEqualTo(beforeCall);
        assertThat(capturedTime).isBeforeOrEqualTo(afterCall);
    }

    @Test
    void processFailedNotifications_exceptionThrown_continuesWithoutRethrowing() {
        // Arrange
        when(emailQueueRepository.findFailedNotificationsForRetry(anyInt(), any(LocalDateTime.class)))
            .thenThrow(new RuntimeException("Database error"));

        // Act - should not throw
        emailRetryScheduler.processFailedNotifications();

        // Assert - method completed without exception
        verify(emailQueueRepository, never()).resetForRetry(anyList());
    }

    @Test
    void processFailedNotifications_singleNotification_resetsSuccessfully() {
        // Arrange
        EmailNotificationQueue failed = createFailedNotification(1L, 1);
        when(emailQueueRepository.findFailedNotificationsForRetry(anyInt(), any(LocalDateTime.class)))
            .thenReturn(List.of(failed));

        // Act
        emailRetryScheduler.processFailedNotifications();

        // Assert
        verify(emailQueueRepository).resetForRetry(List.of(1L));
    }

    // Helper method
    private EmailNotificationQueue createFailedNotification(Long id, int retryCount) {
        EmailNotificationQueue notification = EmailNotificationQueue.builder()
            .id(id)
            .task(task)
            .recipient(recipient)
            .notificationType(NotificationType.TASK_CREATED)
            .notificationData("{}")
            .status(EmailNotificationQueue.EmailQueueStatus.FAILED)
            .retryCount(retryCount)
            .createdAt(LocalDateTime.now().minusHours(1))
            .processedAt(LocalDateTime.now().minusMinutes(30))
            .nextRetryAt(LocalDateTime.now().minusMinutes(5))
            .errorMessage("Previous failure")
            .build();
        return notification;
    }
}
