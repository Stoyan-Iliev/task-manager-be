package com.gradproject.taskmanager.modules.notification.integration;

import com.gradproject.taskmanager.AbstractIntegrationTest;
import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.notification.domain.EmailNotificationQueue;
import com.gradproject.taskmanager.modules.notification.domain.NotificationType;
import com.gradproject.taskmanager.modules.notification.repository.EmailNotificationQueueRepository;
import com.gradproject.taskmanager.modules.organization.domain.Organization;
import com.gradproject.taskmanager.modules.organization.repository.OrganizationRepository;
import com.gradproject.taskmanager.modules.project.domain.Project;
import com.gradproject.taskmanager.modules.project.domain.StatusCategory;
import com.gradproject.taskmanager.modules.project.domain.TaskStatus;
import com.gradproject.taskmanager.modules.project.repository.ProjectRepository;
import com.gradproject.taskmanager.modules.project.repository.TaskStatusRepository;
import com.gradproject.taskmanager.modules.task.domain.Task;
import com.gradproject.taskmanager.modules.task.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for email notification queue operations.
 * Tests real database operations with Testcontainers PostgreSQL.
 */
class EmailQueueIntegrationIT extends AbstractIntegrationTest {

    @Autowired
    private EmailNotificationQueueRepository emailQueueRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TaskStatusRepository taskStatusRepository;

    private Organization organization;
    private Project project;
    private TaskStatus taskStatus;
    private Task task;
    private User recipient;

    @BeforeEach
    void setUp() {
        // Clean up existing data
        emailQueueRepository.deleteAll();

        // Use short unique suffix to avoid exceeding column limits
        String uniqueSuffix = String.valueOf(System.currentTimeMillis() % 10000);

        // Create organization
        organization = new Organization();
        organization.setName("Test Org " + uniqueSuffix);
        organization.setSlug("test-org-" + uniqueSuffix);
        organization = organizationRepository.save(organization);

        // Create user
        recipient = new User();
        recipient.setUsername("rcpt" + uniqueSuffix);
        recipient.setEmail("recipient" + uniqueSuffix + "@example.com");
        recipient.setPassword("password123");
        recipient = userRepository.save(recipient);

        // Create project
        project = new Project();
        project.setName("Test Project");
        project.setKey("PRJ" + uniqueSuffix);  // Must be <= 10 chars
        project.setOrganization(organization);
        project.setCreatedBy(recipient);  // Required field
        project = projectRepository.save(project);

        // Create task status
        taskStatus = new TaskStatus();
        taskStatus.setName("To Do");
        taskStatus.setProject(project);
        taskStatus.setOrderIndex(1);
        taskStatus.setCategory(StatusCategory.TODO);
        taskStatus.setIsDefault(true);
        taskStatus = taskStatusRepository.save(taskStatus);

        // Create task
        task = new Task();
        task.setKey(project.getKey() + "-1");
        task.setTitle("Test Task");
        task.setOrganization(organization);
        task.setProject(project);
        task.setStatus(taskStatus);
        task.setReporter(recipient);
        task.setCreatedBy(recipient);  // Required field
        task = taskRepository.save(task);
    }

    @Test
    @Transactional
    void saveAndFindPendingNotifications_works() {
        // Arrange
        EmailNotificationQueue notification = EmailNotificationQueue.builder()
            .task(task)
            .recipient(recipient)
            .notificationType(NotificationType.TASK_CREATED)
            .notificationData("{\"message\":\"Test\"}")
            .status(EmailNotificationQueue.EmailQueueStatus.PENDING)
            .build();

        // Act
        EmailNotificationQueue saved = emailQueueRepository.save(notification);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();

        List<EmailNotificationQueue> pending = emailQueueRepository
            .findPendingNotificationsForProcessing(LocalDateTime.now().plusMinutes(1));
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getTask().getKey()).isEqualTo(task.getKey());
    }

    @Test
    @Transactional
    void markAsSent_updatesStatusAndTimestamp() {
        // Arrange
        EmailNotificationQueue notification = EmailNotificationQueue.builder()
            .task(task)
            .recipient(recipient)
            .notificationType(NotificationType.TASK_CREATED)
            .notificationData("{\"message\":\"Test\"}")
            .status(EmailNotificationQueue.EmailQueueStatus.PENDING)
            .build();
        notification = emailQueueRepository.save(notification);
        Long id = notification.getId();

        // Act
        LocalDateTime sentTime = LocalDateTime.now();
        emailQueueRepository.markAsSent(List.of(id), sentTime);
        emailQueueRepository.flush();

        // Assert
        EmailNotificationQueue updated = emailQueueRepository.findById(id).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(EmailNotificationQueue.EmailQueueStatus.SENT);
        assertThat(updated.getSentAt()).isNotNull();
    }

    @Test
    @Transactional
    void markAsFailed_updatesStatusAndSetsNextRetry() {
        // Arrange
        EmailNotificationQueue notification = EmailNotificationQueue.builder()
            .task(task)
            .recipient(recipient)
            .notificationType(NotificationType.TASK_CREATED)
            .notificationData("{\"message\":\"Test\"}")
            .status(EmailNotificationQueue.EmailQueueStatus.PENDING)
            .retryCount(0)
            .build();
        notification = emailQueueRepository.save(notification);
        Long id = notification.getId();

        // Act
        LocalDateTime processedAt = LocalDateTime.now();
        LocalDateTime nextRetry = LocalDateTime.now().plusMinutes(5);
        emailQueueRepository.markAsFailed(List.of(id), "Test error", processedAt, nextRetry);
        emailQueueRepository.flush();

        // Assert
        EmailNotificationQueue updated = emailQueueRepository.findById(id).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(EmailNotificationQueue.EmailQueueStatus.FAILED);
        assertThat(updated.getErrorMessage()).isEqualTo("Test error");
        assertThat(updated.getRetryCount()).isEqualTo(1);
        assertThat(updated.getNextRetryAt()).isNotNull();
    }

    @Test
    @Transactional
    void markAsPermanentlyFailed_setsCorrectStatus() {
        // Arrange
        EmailNotificationQueue notification = EmailNotificationQueue.builder()
            .task(task)
            .recipient(recipient)
            .notificationType(NotificationType.TASK_CREATED)
            .notificationData("{\"message\":\"Test\"}")
            .status(EmailNotificationQueue.EmailQueueStatus.FAILED)
            .retryCount(3)
            .build();
        notification = emailQueueRepository.save(notification);
        Long id = notification.getId();

        // Act
        emailQueueRepository.markAsPermanentlyFailed(List.of(id), "Max retries exceeded", LocalDateTime.now());
        emailQueueRepository.flush();

        // Assert
        EmailNotificationQueue updated = emailQueueRepository.findById(id).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(EmailNotificationQueue.EmailQueueStatus.PERMANENTLY_FAILED);
        assertThat(updated.getErrorMessage()).isEqualTo("Max retries exceeded");
    }

    @Test
    @Transactional
    void resetForRetry_changesStatusToPending() {
        // Arrange
        EmailNotificationQueue notification = EmailNotificationQueue.builder()
            .task(task)
            .recipient(recipient)
            .notificationType(NotificationType.TASK_CREATED)
            .notificationData("{\"message\":\"Test\"}")
            .status(EmailNotificationQueue.EmailQueueStatus.FAILED)
            .retryCount(1)
            .errorMessage("Previous error")
            .nextRetryAt(LocalDateTime.now().minusMinutes(1))
            .build();
        notification = emailQueueRepository.save(notification);
        Long id = notification.getId();

        // Act
        emailQueueRepository.resetForRetry(List.of(id));
        emailQueueRepository.flush();

        // Assert
        EmailNotificationQueue updated = emailQueueRepository.findById(id).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(EmailNotificationQueue.EmailQueueStatus.PENDING);
        assertThat(updated.getErrorMessage()).isNull();
    }

    @Test
    @Transactional
    void findFailedNotificationsForRetry_respectsMaxRetryCount() {
        // Arrange - create notifications with different retry counts
        EmailNotificationQueue eligible = EmailNotificationQueue.builder()
            .task(task)
            .recipient(recipient)
            .notificationType(NotificationType.TASK_CREATED)
            .notificationData("{\"message\":\"Eligible\"}")
            .status(EmailNotificationQueue.EmailQueueStatus.FAILED)
            .retryCount(1)
            .nextRetryAt(LocalDateTime.now().minusMinutes(1))
            .build();
        emailQueueRepository.save(eligible);

        EmailNotificationQueue exhausted = EmailNotificationQueue.builder()
            .task(task)
            .recipient(recipient)
            .notificationType(NotificationType.COMMENT_ADDED)
            .notificationData("{\"message\":\"Exhausted\"}")
            .status(EmailNotificationQueue.EmailQueueStatus.FAILED)
            .retryCount(3)  // At max
            .nextRetryAt(LocalDateTime.now().minusMinutes(1))
            .build();
        emailQueueRepository.save(exhausted);

        // Act
        List<EmailNotificationQueue> forRetry = emailQueueRepository
            .findFailedNotificationsForRetry(3, LocalDateTime.now());

        // Assert - only the eligible one should be returned
        assertThat(forRetry).hasSize(1);
        assertThat(forRetry.get(0).getNotificationData()).contains("Eligible");
    }

    @Test
    @Transactional
    void findFailedNotificationsForRetry_respectsNextRetryTime() {
        // Arrange - create notifications with different nextRetryAt times
        EmailNotificationQueue ready = EmailNotificationQueue.builder()
            .task(task)
            .recipient(recipient)
            .notificationType(NotificationType.TASK_CREATED)
            .notificationData("{\"message\":\"Ready\"}")
            .status(EmailNotificationQueue.EmailQueueStatus.FAILED)
            .retryCount(1)
            .nextRetryAt(LocalDateTime.now().minusMinutes(5))  // Past
            .build();
        emailQueueRepository.save(ready);

        EmailNotificationQueue notYet = EmailNotificationQueue.builder()
            .task(task)
            .recipient(recipient)
            .notificationType(NotificationType.COMMENT_ADDED)
            .notificationData("{\"message\":\"NotYet\"}")
            .status(EmailNotificationQueue.EmailQueueStatus.FAILED)
            .retryCount(1)
            .nextRetryAt(LocalDateTime.now().plusMinutes(10))  // Future
            .build();
        emailQueueRepository.save(notYet);

        // Act
        List<EmailNotificationQueue> forRetry = emailQueueRepository
            .findFailedNotificationsForRetry(3, LocalDateTime.now());

        // Assert - only the ready one should be returned
        assertThat(forRetry).hasSize(1);
        assertThat(forRetry.get(0).getNotificationData()).contains("Ready");
    }

    @Test
    @Transactional
    void countByStatus_returnsCorrectCounts() {
        // Arrange
        emailQueueRepository.save(EmailNotificationQueue.builder()
            .task(task).recipient(recipient).notificationType(NotificationType.TASK_CREATED)
            .notificationData("{}").status(EmailNotificationQueue.EmailQueueStatus.PENDING).build());
        emailQueueRepository.save(EmailNotificationQueue.builder()
            .task(task).recipient(recipient).notificationType(NotificationType.TASK_CREATED)
            .notificationData("{}").status(EmailNotificationQueue.EmailQueueStatus.PENDING).build());
        emailQueueRepository.save(EmailNotificationQueue.builder()
            .task(task).recipient(recipient).notificationType(NotificationType.TASK_CREATED)
            .notificationData("{}").status(EmailNotificationQueue.EmailQueueStatus.SENT).build());
        emailQueueRepository.save(EmailNotificationQueue.builder()
            .task(task).recipient(recipient).notificationType(NotificationType.TASK_CREATED)
            .notificationData("{}").status(EmailNotificationQueue.EmailQueueStatus.FAILED).build());

        // Act & Assert
        assertThat(emailQueueRepository.countByStatus(EmailNotificationQueue.EmailQueueStatus.PENDING)).isEqualTo(2);
        assertThat(emailQueueRepository.countByStatus(EmailNotificationQueue.EmailQueueStatus.SENT)).isEqualTo(1);
        assertThat(emailQueueRepository.countByStatus(EmailNotificationQueue.EmailQueueStatus.FAILED)).isEqualTo(1);
    }
}
