package com.gradproject.taskmanager.modules.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.notification.domain.EmailNotificationQueue;
import com.gradproject.taskmanager.modules.notification.domain.NotificationType;
import com.gradproject.taskmanager.modules.organization.domain.Organization;
import com.gradproject.taskmanager.modules.project.domain.Project;
import com.gradproject.taskmanager.modules.project.domain.TaskStatus;
import com.gradproject.taskmanager.modules.task.domain.Task;
import com.gradproject.taskmanager.modules.task.domain.TaskPriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EmailTemplateServiceTest {

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private EmailTemplateService emailTemplateService;

    private Organization organization;
    private Project project;
    private TaskStatus taskStatus;
    private Task task;
    private User assignee;
    private User recipient;

    @BeforeEach
    void setUp() {
        // Set the frontend base URL
        ReflectionTestUtils.setField(emailTemplateService, "frontendBaseUrl", "http://localhost:5173");

        organization = new Organization();
        organization.setId(1L);
        organization.setName("Test Org");

        project = new Project();
        project.setId(1L);
        project.setKey("PROJ");
        project.setName("Test Project");
        project.setOrganization(organization);

        taskStatus = new TaskStatus();
        taskStatus.setId(1L);
        taskStatus.setName("To Do");

        assignee = new User();
        assignee.setId(1);
        assignee.setUsername("assignee");
        assignee.setEmail("assignee@example.com");

        recipient = new User();
        recipient.setId(2);
        recipient.setUsername("recipient");
        recipient.setEmail("recipient@example.com");

        task = new Task();
        task.setId(1L);
        task.setKey("PROJ-123");
        task.setTitle("Test Task");
        task.setOrganization(organization);
        task.setProject(project);
        task.setStatus(taskStatus);
        task.setAssignee(assignee);
        task.setPriority(TaskPriority.HIGH);
    }

    @Test
    void generateTaskDigestEmail_includesTaskKey() {
        // Arrange
        EmailNotificationQueue notification = createNotification(NotificationType.TASK_CREATED, "Task was created");

        // Act
        String html = emailTemplateService.generateTaskDigestEmail(task, List.of(notification));

        // Assert
        assertThat(html).contains("PROJ-123");
    }

    @Test
    void generateTaskDigestEmail_includesTaskTitle() {
        // Arrange
        EmailNotificationQueue notification = createNotification(NotificationType.TASK_CREATED, "Task was created");

        // Act
        String html = emailTemplateService.generateTaskDigestEmail(task, List.of(notification));

        // Assert
        assertThat(html).contains("Test Task");
    }

    @Test
    void generateTaskDigestEmail_includesProjectName() {
        // Arrange
        EmailNotificationQueue notification = createNotification(NotificationType.TASK_CREATED, "Task was created");

        // Act
        String html = emailTemplateService.generateTaskDigestEmail(task, List.of(notification));

        // Assert
        assertThat(html).contains("Test Project");
    }

    @Test
    void generateTaskDigestEmail_includesAssignee() {
        // Arrange
        EmailNotificationQueue notification = createNotification(NotificationType.TASK_CREATED, "Task was created");

        // Act
        String html = emailTemplateService.generateTaskDigestEmail(task, List.of(notification));

        // Assert
        assertThat(html).contains("assignee");
    }

    @Test
    void generateTaskDigestEmail_includesStatus() {
        // Arrange
        EmailNotificationQueue notification = createNotification(NotificationType.TASK_CREATED, "Task was created");

        // Act
        String html = emailTemplateService.generateTaskDigestEmail(task, List.of(notification));

        // Assert
        assertThat(html).contains("To Do");
    }

    @Test
    void generateTaskDigestEmail_includesPriority() {
        // Arrange
        EmailNotificationQueue notification = createNotification(NotificationType.TASK_CREATED, "Task was created");

        // Act
        String html = emailTemplateService.generateTaskDigestEmail(task, List.of(notification));

        // Assert
        assertThat(html).contains("HIGH");
    }

    @Test
    void generateTaskDigestEmail_includesViewTaskLink() {
        // Arrange
        EmailNotificationQueue notification = createNotification(NotificationType.TASK_CREATED, "Task was created");

        // Act
        String html = emailTemplateService.generateTaskDigestEmail(task, List.of(notification));

        // Assert
        assertThat(html).contains("http://localhost:5173/tasks/PROJ-123");
        assertThat(html).contains("View Task");
    }

    @Test
    void generateTaskDigestEmail_singleNotification_showsUpdate() {
        // Arrange
        EmailNotificationQueue notification = createNotification(NotificationType.TASK_CREATED, "Task was created");

        // Act
        String html = emailTemplateService.generateTaskDigestEmail(task, List.of(notification));

        // Assert
        assertThat(html).contains("Update");
        assertThat(html).doesNotContain("Updates");
    }

    @Test
    void generateTaskDigestEmail_multipleNotifications_showsCount() {
        // Arrange
        EmailNotificationQueue notification1 = createNotification(NotificationType.TASK_CREATED, "Task was created");
        EmailNotificationQueue notification2 = createNotification(NotificationType.COMMENT_ADDED, "Comment was added");
        EmailNotificationQueue notification3 = createNotification(NotificationType.STATUS_CHANGED, "Status changed");

        // Act
        String html = emailTemplateService.generateTaskDigestEmail(task, List.of(notification1, notification2, notification3));

        // Assert
        assertThat(html).contains("3 Updates");
    }

    @Test
    void generateTaskDigestEmail_escapesHtmlInTaskTitle() {
        // Arrange
        task.setTitle("<script>alert('xss')</script>");
        EmailNotificationQueue notification = createNotification(NotificationType.TASK_CREATED, "Task was created");

        // Act
        String html = emailTemplateService.generateTaskDigestEmail(task, List.of(notification));

        // Assert - HTML should be escaped
        assertThat(html).doesNotContain("<script>");
        assertThat(html).contains("&lt;script&gt;");
    }

    @Test
    void generateTaskDigestEmail_escapesHtmlInMessage() {
        // Arrange
        EmailNotificationQueue notification = createNotification(NotificationType.TASK_CREATED, "<script>alert('xss')</script>");

        // Act
        String html = emailTemplateService.generateTaskDigestEmail(task, List.of(notification));

        // Assert - HTML should be escaped
        assertThat(html).doesNotContain("<script>alert");
        assertThat(html).contains("&lt;script&gt;");
    }

    @Test
    void generateTaskDigestEmail_includesCorrectIconForTaskCreated() {
        // Arrange
        EmailNotificationQueue notification = createNotification(NotificationType.TASK_CREATED, "Task was created");

        // Act
        String html = emailTemplateService.generateTaskDigestEmail(task, List.of(notification));

        // Assert
        assertThat(html).contains("Task Created");
    }

    @Test
    void generateTaskDigestEmail_includesCorrectIconForCommentAdded() {
        // Arrange
        EmailNotificationQueue notification = createNotification(NotificationType.COMMENT_ADDED, "Comment was added");

        // Act
        String html = emailTemplateService.generateTaskDigestEmail(task, List.of(notification));

        // Assert
        assertThat(html).contains("Comment Added");
    }

    @Test
    void generateTaskDigestEmail_includesCorrectIconForStatusChanged() {
        // Arrange
        EmailNotificationQueue notification = createNotification(NotificationType.STATUS_CHANGED, "Status was changed");

        // Act
        String html = emailTemplateService.generateTaskDigestEmail(task, List.of(notification));

        // Assert
        assertThat(html).contains("Status Changed");
    }

    @Test
    void generateTaskDigestEmail_handlesNullAssignee() {
        // Arrange
        task.setAssignee(null);
        EmailNotificationQueue notification = createNotification(NotificationType.TASK_CREATED, "Task was created");

        // Act
        String html = emailTemplateService.generateTaskDigestEmail(task, List.of(notification));

        // Assert - should not throw and should not contain assignee section
        assertThat(html).isNotNull();
        assertThat(html).contains("PROJ-123");
    }

    @Test
    void generateTaskDigestEmail_handlesNullStatus() {
        // Arrange
        task.setStatus(null);
        EmailNotificationQueue notification = createNotification(NotificationType.TASK_CREATED, "Task was created");

        // Act
        String html = emailTemplateService.generateTaskDigestEmail(task, List.of(notification));

        // Assert - should not throw
        assertThat(html).isNotNull();
        assertThat(html).contains("PROJ-123");
    }

    @Test
    void generateTaskDigestEmail_handlesNullPriority() {
        // Arrange
        task.setPriority(null);
        EmailNotificationQueue notification = createNotification(NotificationType.TASK_CREATED, "Task was created");

        // Act
        String html = emailTemplateService.generateTaskDigestEmail(task, List.of(notification));

        // Assert - should not throw
        assertThat(html).isNotNull();
        assertThat(html).contains("PROJ-123");
    }

    @Test
    void generateTaskDigestEmail_isValidHtml() {
        // Arrange
        EmailNotificationQueue notification = createNotification(NotificationType.TASK_CREATED, "Task was created");

        // Act
        String html = emailTemplateService.generateTaskDigestEmail(task, List.of(notification));

        // Assert - basic HTML structure validation
        assertThat(html).contains("<!DOCTYPE html>");
        assertThat(html).contains("<html");
        assertThat(html).contains("</html>");
        assertThat(html).contains("<head>");
        assertThat(html).contains("</head>");
        assertThat(html).contains("<body>");
        assertThat(html).contains("</body>");
    }

    @Test
    void generateTaskDigestEmail_includesActorName() throws Exception {
        // Arrange
        Map<String, Object> data = Map.of(
            "message", "Task was created",
            "actorName", "John Doe"
        );
        String jsonData = objectMapper.writeValueAsString(data);

        EmailNotificationQueue notification = EmailNotificationQueue.builder()
            .id(1L)
            .task(task)
            .recipient(recipient)
            .notificationType(NotificationType.TASK_CREATED)
            .notificationData(jsonData)
            .status(EmailNotificationQueue.EmailQueueStatus.PENDING)
            .createdAt(LocalDateTime.now())
            .build();

        // Act
        String html = emailTemplateService.generateTaskDigestEmail(task, List.of(notification));

        // Assert
        assertThat(html).contains("John Doe");
    }

    // Helper method
    private EmailNotificationQueue createNotification(NotificationType type, String message) {
        try {
            Map<String, Object> data = Map.of("message", message);
            String jsonData = objectMapper.writeValueAsString(data);

            return EmailNotificationQueue.builder()
                .id(1L)
                .task(task)
                .recipient(recipient)
                .notificationType(type)
                .notificationData(jsonData)
                .status(EmailNotificationQueue.EmailQueueStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
