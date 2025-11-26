package com.gradproject.taskmanager.modules.notification.service;

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
class EmailDigestServiceImplTest {

    @Mock
    private EmailNotificationQueueRepository emailQueueRepository;

    @Mock
    private EmailSenderService emailSenderService;

    @Mock
    private EmailTemplateService emailTemplateService;

    private EmailDigestServiceImpl emailDigestService;

    private Organization organization;
    private Project project;
    private TaskStatus taskStatus;
    private Task task;
    private User recipient;
    private User actor;

    // Retry configuration values
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int INITIAL_DELAY_MINUTES = 5;
    private static final int BACKOFF_MULTIPLIER = 2;

    @BeforeEach
    void setUp() {
        // Create service with retry configuration
        emailDigestService = new EmailDigestServiceImpl(
            emailQueueRepository,
            emailSenderService,
            emailTemplateService,
            MAX_RETRY_ATTEMPTS,
            INITIAL_DELAY_MINUTES,
            BACKOFF_MULTIPLIER
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

        actor = new User();
        actor.setId(2);
        actor.setUsername("actor");
        actor.setEmail("actor@example.com");
    }

    @Test
    void processQueuedNotifications_emptyQueue_doesNothing() throws Exception {
        // Arrange
        when(emailQueueRepository.findPendingNotificationsForProcessing(any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());

        // Act
        emailDigestService.processQueuedNotifications();

        // Assert
        verify(emailSenderService, never()).sendEmail(anyString(), anyString(), anyString());
        verify(emailQueueRepository, never()).markAsSent(anyList(), any(LocalDateTime.class));
    }

    @Test
    void processQueuedNotifications_singleNotification_sendsEmail() throws Exception {
        // Arrange
        EmailNotificationQueue notification = createNotification(task, recipient, NotificationType.TASK_CREATED, 0);
        when(emailQueueRepository.findPendingNotificationsForProcessing(any(LocalDateTime.class)))
            .thenReturn(List.of(notification));
        when(emailTemplateService.generateTaskDigestEmail(eq(task), anyList()))
            .thenReturn("<html>Email content</html>");

        // Act
        emailDigestService.processQueuedNotifications();

        // Assert
        verify(emailSenderService).sendEmail(eq("recipient@example.com"), anyString(), anyString());
        verify(emailQueueRepository).markAsSent(eq(List.of(1L)), any(LocalDateTime.class));
    }

    @Test
    void processQueuedNotifications_multipleNotificationsSameRecipient_batchesIntoOneEmail() throws Exception {
        // Arrange
        EmailNotificationQueue notification1 = createNotification(task, recipient, NotificationType.TASK_CREATED, 0);
        notification1.setId(1L);
        EmailNotificationQueue notification2 = createNotification(task, recipient, NotificationType.COMMENT_ADDED, 0);
        notification2.setId(2L);

        when(emailQueueRepository.findPendingNotificationsForProcessing(any(LocalDateTime.class)))
            .thenReturn(List.of(notification1, notification2));
        when(emailTemplateService.generateTaskDigestEmail(eq(task), anyList()))
            .thenReturn("<html>Email content</html>");

        // Act
        emailDigestService.processQueuedNotifications();

        // Assert - should send only ONE email for both notifications
        verify(emailSenderService, times(1)).sendEmail(anyString(), anyString(), anyString());

        // Verify both IDs are marked as sent
        ArgumentCaptor<List<Long>> idsCaptor = ArgumentCaptor.forClass(List.class);
        verify(emailQueueRepository).markAsSent(idsCaptor.capture(), any(LocalDateTime.class));
        assertThat(idsCaptor.getValue()).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void processQueuedNotifications_differentRecipients_sendsSeparateEmails() throws Exception {
        // Arrange
        User recipient2 = new User();
        recipient2.setId(3);
        recipient2.setUsername("recipient2");
        recipient2.setEmail("recipient2@example.com");

        EmailNotificationQueue notification1 = createNotification(task, recipient, NotificationType.TASK_CREATED, 0);
        notification1.setId(1L);
        EmailNotificationQueue notification2 = createNotification(task, recipient2, NotificationType.TASK_CREATED, 0);
        notification2.setId(2L);

        when(emailQueueRepository.findPendingNotificationsForProcessing(any(LocalDateTime.class)))
            .thenReturn(List.of(notification1, notification2));
        when(emailTemplateService.generateTaskDigestEmail(eq(task), anyList()))
            .thenReturn("<html>Email content</html>");

        // Act
        emailDigestService.processQueuedNotifications();

        // Assert - should send TWO separate emails
        verify(emailSenderService, times(2)).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void processQueuedNotifications_sendFails_marksAsFailedWithNextRetryTime() throws Exception {
        // Arrange
        EmailNotificationQueue notification = createNotification(task, recipient, NotificationType.TASK_CREATED, 0);
        when(emailQueueRepository.findPendingNotificationsForProcessing(any(LocalDateTime.class)))
            .thenReturn(List.of(notification));
        when(emailTemplateService.generateTaskDigestEmail(eq(task), anyList()))
            .thenReturn("<html>Email content</html>");
        doThrow(new RuntimeException("SendGrid error"))
            .when(emailSenderService).sendEmail(anyString(), anyString(), anyString());

        // Act
        emailDigestService.processQueuedNotifications();

        // Assert
        ArgumentCaptor<LocalDateTime> nextRetryCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(emailQueueRepository).markAsFailed(
            eq(List.of(1L)),
            anyString(),
            any(LocalDateTime.class),
            nextRetryCaptor.capture()
        );

        // First retry should be ~5 minutes from now (initial delay)
        LocalDateTime nextRetry = nextRetryCaptor.getValue();
        assertThat(nextRetry).isAfter(LocalDateTime.now());
        assertThat(nextRetry).isBefore(LocalDateTime.now().plusMinutes(6)); // Allow some tolerance
    }

    @Test
    void processQueuedNotifications_secondFailure_usesExponentialBackoff() throws Exception {
        // Arrange - notification with retryCount=1 (already failed once)
        EmailNotificationQueue notification = createNotification(task, recipient, NotificationType.TASK_CREATED, 1);
        when(emailQueueRepository.findPendingNotificationsForProcessing(any(LocalDateTime.class)))
            .thenReturn(List.of(notification));
        when(emailTemplateService.generateTaskDigestEmail(eq(task), anyList()))
            .thenReturn("<html>Email content</html>");
        doThrow(new RuntimeException("SendGrid error"))
            .when(emailSenderService).sendEmail(anyString(), anyString(), anyString());

        // Act
        emailDigestService.processQueuedNotifications();

        // Assert
        ArgumentCaptor<LocalDateTime> nextRetryCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(emailQueueRepository).markAsFailed(
            eq(List.of(1L)),
            anyString(),
            any(LocalDateTime.class),
            nextRetryCaptor.capture()
        );

        // Second retry should be ~10 minutes from now (5 * 2^1)
        LocalDateTime nextRetry = nextRetryCaptor.getValue();
        assertThat(nextRetry).isAfter(LocalDateTime.now().plusMinutes(9));
        assertThat(nextRetry).isBefore(LocalDateTime.now().plusMinutes(11));
    }

    @Test
    void processQueuedNotifications_maxRetriesExceeded_marksAsPermanentlyFailed() throws Exception {
        // Arrange - notification at max retries (retryCount=2, so next would be 3)
        EmailNotificationQueue notification = createNotification(task, recipient, NotificationType.TASK_CREATED, 2);
        when(emailQueueRepository.findPendingNotificationsForProcessing(any(LocalDateTime.class)))
            .thenReturn(List.of(notification));
        when(emailTemplateService.generateTaskDigestEmail(eq(task), anyList()))
            .thenReturn("<html>Email content</html>");
        doThrow(new RuntimeException("SendGrid error"))
            .when(emailSenderService).sendEmail(anyString(), anyString(), anyString());

        // Act
        emailDigestService.processQueuedNotifications();

        // Assert - should mark as permanently failed, NOT as failed with retry
        verify(emailQueueRepository).markAsPermanentlyFailed(
            eq(List.of(1L)),
            anyString(),
            any(LocalDateTime.class)
        );
        verify(emailQueueRepository, never()).markAsFailed(anyList(), anyString(), any(), any());
    }

    @Test
    void processQueuedNotifications_errorMessageTruncated() throws Exception {
        // Arrange
        EmailNotificationQueue notification = createNotification(task, recipient, NotificationType.TASK_CREATED, 0);
        when(emailQueueRepository.findPendingNotificationsForProcessing(any(LocalDateTime.class)))
            .thenReturn(List.of(notification));
        when(emailTemplateService.generateTaskDigestEmail(eq(task), anyList()))
            .thenReturn("<html>Email content</html>");

        // Create a very long error message
        String longError = "A".repeat(600);
        doThrow(new RuntimeException(longError))
            .when(emailSenderService).sendEmail(anyString(), anyString(), anyString());

        // Act
        emailDigestService.processQueuedNotifications();

        // Assert
        ArgumentCaptor<String> errorCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailQueueRepository).markAsFailed(
            anyList(),
            errorCaptor.capture(),
            any(LocalDateTime.class),
            any(LocalDateTime.class)
        );

        // Error should be truncated to 500 chars
        assertThat(errorCaptor.getValue().length()).isLessThanOrEqualTo(500);
        assertThat(errorCaptor.getValue()).endsWith("...");
    }

    @Test
    void processQueuedNotifications_singleNotification_createsCorrectSubject() throws Exception {
        // Arrange
        EmailNotificationQueue notification = createNotification(task, recipient, NotificationType.STATUS_CHANGED, 0);
        when(emailQueueRepository.findPendingNotificationsForProcessing(any(LocalDateTime.class)))
            .thenReturn(List.of(notification));
        when(emailTemplateService.generateTaskDigestEmail(eq(task), anyList()))
            .thenReturn("<html>Email content</html>");

        // Act
        emailDigestService.processQueuedNotifications();

        // Assert
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSenderService).sendEmail(anyString(), subjectCaptor.capture(), anyString());

        String subject = subjectCaptor.getValue();
        assertThat(subject).contains("[PROJ-123]");
        assertThat(subject).contains("Test Task");
        assertThat(subject).contains("Status changed");
    }

    @Test
    void processQueuedNotifications_multipleNotifications_subjectShowsCount() throws Exception {
        // Arrange
        EmailNotificationQueue notification1 = createNotification(task, recipient, NotificationType.TASK_CREATED, 0);
        notification1.setId(1L);
        EmailNotificationQueue notification2 = createNotification(task, recipient, NotificationType.COMMENT_ADDED, 0);
        notification2.setId(2L);
        EmailNotificationQueue notification3 = createNotification(task, recipient, NotificationType.STATUS_CHANGED, 0);
        notification3.setId(3L);

        when(emailQueueRepository.findPendingNotificationsForProcessing(any(LocalDateTime.class)))
            .thenReturn(List.of(notification1, notification2, notification3));
        when(emailTemplateService.generateTaskDigestEmail(eq(task), anyList()))
            .thenReturn("<html>Email content</html>");

        // Act
        emailDigestService.processQueuedNotifications();

        // Assert
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSenderService).sendEmail(anyString(), subjectCaptor.capture(), anyString());

        String subject = subjectCaptor.getValue();
        assertThat(subject).contains("[PROJ-123]");
        assertThat(subject).contains("3 updates");
    }

    // Helper method to create test notifications
    private EmailNotificationQueue createNotification(Task task, User recipient, NotificationType type, int retryCount) {
        return EmailNotificationQueue.builder()
            .id(1L)
            .task(task)
            .recipient(recipient)
            .notificationType(type)
            .notificationData("{}")
            .status(EmailNotificationQueue.EmailQueueStatus.PENDING)
            .retryCount(retryCount)
            .createdAt(LocalDateTime.now())
            .build();
    }
}
