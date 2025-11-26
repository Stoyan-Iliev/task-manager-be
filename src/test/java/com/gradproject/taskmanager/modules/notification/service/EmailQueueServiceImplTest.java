package com.gradproject.taskmanager.modules.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.notification.domain.EmailNotificationQueue;
import com.gradproject.taskmanager.modules.notification.domain.Notification;
import com.gradproject.taskmanager.modules.notification.domain.NotificationType;
import com.gradproject.taskmanager.modules.notification.repository.EmailNotificationQueueRepository;
import com.gradproject.taskmanager.modules.organization.domain.Organization;
import com.gradproject.taskmanager.modules.project.domain.Project;
import com.gradproject.taskmanager.modules.project.domain.TaskStatus;
import com.gradproject.taskmanager.modules.task.domain.Task;
import com.gradproject.taskmanager.modules.task.domain.TaskWatcher;
import com.gradproject.taskmanager.modules.task.repository.TaskWatcherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailQueueServiceImplTest {

    @Mock
    private EmailNotificationQueueRepository emailQueueRepository;

    @Mock
    private TaskWatcherRepository taskWatcherRepository;

    @Mock
    private EmailPreferenceService emailPreferenceService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private EmailQueueServiceImpl emailQueueService;

    private Organization organization;
    private Project project;
    private TaskStatus taskStatus;
    private Task task;
    private User assignee;
    private User reporter;
    private User watcher;
    private User actor;

    @BeforeEach
    void setUp() {
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

        reporter = new User();
        reporter.setId(2);
        reporter.setUsername("reporter");
        reporter.setEmail("reporter@example.com");

        watcher = new User();
        watcher.setId(3);
        watcher.setUsername("watcher");
        watcher.setEmail("watcher@example.com");

        actor = new User();
        actor.setId(4);
        actor.setUsername("actor");
        actor.setEmail("actor@example.com");

        task = new Task();
        task.setId(1L);
        task.setKey("PROJ-123");
        task.setTitle("Test Task");
        task.setOrganization(organization);
        task.setProject(project);
        task.setStatus(taskStatus);
        task.setAssignee(assignee);
        task.setReporter(reporter);
    }

    @Test
    void determineRecipients_includesAssignee() {
        // Arrange
        when(taskWatcherRepository.findByTaskId(1L)).thenReturn(Collections.emptyList());

        // Act
        Set<User> recipients = emailQueueService.determineRecipients(task, NotificationType.STATUS_CHANGED, actor);

        // Assert
        assertThat(recipients).contains(assignee);
    }

    @Test
    void determineRecipients_includesReporter() {
        // Arrange
        when(taskWatcherRepository.findByTaskId(1L)).thenReturn(Collections.emptyList());

        // Act
        Set<User> recipients = emailQueueService.determineRecipients(task, NotificationType.STATUS_CHANGED, actor);

        // Assert
        assertThat(recipients).contains(reporter);
    }

    @Test
    void determineRecipients_includesWatchers() {
        // Arrange
        TaskWatcher taskWatcher = TaskWatcher.builder()
            .id(1L)
            .task(task)
            .user(watcher)
            .addedBy(4)
            .addedAt(LocalDateTime.now())
            .build();
        when(taskWatcherRepository.findByTaskId(1L)).thenReturn(List.of(taskWatcher));

        // Act
        Set<User> recipients = emailQueueService.determineRecipients(task, NotificationType.STATUS_CHANGED, actor);

        // Assert
        assertThat(recipients).contains(watcher);
    }

    @Test
    void determineRecipients_excludesActor() {
        // Arrange - actor is the assignee
        task.setAssignee(actor);
        when(taskWatcherRepository.findByTaskId(1L)).thenReturn(Collections.emptyList());

        // Act
        Set<User> recipients = emailQueueService.determineRecipients(task, NotificationType.STATUS_CHANGED, actor);

        // Assert
        assertThat(recipients).doesNotContain(actor);
        assertThat(recipients).contains(reporter); // Reporter should still be included
    }

    @Test
    void determineRecipients_excludesActorFromWatchers() {
        // Arrange - actor is also a watcher
        TaskWatcher actorWatcher = TaskWatcher.builder()
            .id(1L)
            .task(task)
            .user(actor)
            .addedBy(4)
            .addedAt(LocalDateTime.now())
            .build();
        when(taskWatcherRepository.findByTaskId(1L)).thenReturn(List.of(actorWatcher));

        // Act
        Set<User> recipients = emailQueueService.determineRecipients(task, NotificationType.STATUS_CHANGED, actor);

        // Assert
        assertThat(recipients).doesNotContain(actor);
    }

    @Test
    void determineRecipients_handlesNullAssignee() {
        // Arrange
        task.setAssignee(null);
        when(taskWatcherRepository.findByTaskId(1L)).thenReturn(Collections.emptyList());

        // Act
        Set<User> recipients = emailQueueService.determineRecipients(task, NotificationType.STATUS_CHANGED, actor);

        // Assert - should not throw, and reporter should still be included
        assertThat(recipients).contains(reporter);
        assertThat(recipients).hasSize(1);
    }

    @Test
    void determineRecipients_handlesNullReporter() {
        // Arrange
        task.setReporter(null);
        when(taskWatcherRepository.findByTaskId(1L)).thenReturn(Collections.emptyList());

        // Act
        Set<User> recipients = emailQueueService.determineRecipients(task, NotificationType.STATUS_CHANGED, actor);

        // Assert - should not throw, and assignee should still be included
        assertThat(recipients).contains(assignee);
        assertThat(recipients).hasSize(1);
    }

    @Test
    void determineRecipients_noDuplicatesWhenUserHasMultipleRoles() {
        // Arrange - assignee is also a watcher
        TaskWatcher assigneeWatcher = TaskWatcher.builder()
            .id(1L)
            .task(task)
            .user(assignee)
            .addedBy(4)
            .addedAt(LocalDateTime.now())
            .build();
        when(taskWatcherRepository.findByTaskId(1L)).thenReturn(List.of(assigneeWatcher));

        // Act
        Set<User> recipients = emailQueueService.determineRecipients(task, NotificationType.STATUS_CHANGED, actor);

        // Assert - assignee should appear only once
        long assigneeCount = recipients.stream().filter(u -> u.equals(assignee)).count();
        assertThat(assigneeCount).isEqualTo(1);
    }

    @Test
    void determineRecipients_emptyWhenAllUsersAreActor() {
        // Arrange - actor is assignee and reporter, no watchers
        task.setAssignee(actor);
        task.setReporter(actor);
        when(taskWatcherRepository.findByTaskId(1L)).thenReturn(Collections.emptyList());

        // Act
        Set<User> recipients = emailQueueService.determineRecipients(task, NotificationType.STATUS_CHANGED, actor);

        // Assert
        assertThat(recipients).isEmpty();
    }

    @Test
    void queueEmailNotification_savesQueueEntry() {
        // Arrange
        when(emailPreferenceService.isNotificationTypeEnabled(assignee, NotificationType.TASK_CREATED))
            .thenReturn(true);

        Notification notification = Notification.builder()
            .id(1L)
            .organization(organization)
            .project(project)
            .task(task)
            .user(assignee)
            .type(NotificationType.TASK_CREATED)
            .title("Task Created")
            .message("Test message")
            .actor(actor)
            .relatedEntityType("TASK")
            .relatedEntityId(1L)
            .isRead(false)
            .createdAt(LocalDateTime.now())
            .build();

        // Act
        emailQueueService.queueEmailNotification(task, assignee, notification);

        // Assert
        ArgumentCaptor<EmailNotificationQueue> captor = ArgumentCaptor.forClass(EmailNotificationQueue.class);
        verify(emailQueueRepository).save(captor.capture());

        EmailNotificationQueue saved = captor.getValue();
        assertThat(saved.getTask()).isEqualTo(task);
        assertThat(saved.getRecipient()).isEqualTo(assignee);
        assertThat(saved.getNotificationType()).isEqualTo(NotificationType.TASK_CREATED);
        assertThat(saved.getStatus()).isEqualTo(EmailNotificationQueue.EmailQueueStatus.PENDING);
    }

    @Test
    void queueEmailNotification_serializesNotificationData() {
        // Arrange
        when(emailPreferenceService.isNotificationTypeEnabled(assignee, NotificationType.TASK_CREATED))
            .thenReturn(true);

        Notification notification = Notification.builder()
            .id(1L)
            .organization(organization)
            .project(project)
            .task(task)
            .user(assignee)
            .type(NotificationType.TASK_CREATED)
            .title("Task Created")
            .message("Test message")
            .actor(actor)
            .relatedEntityType("TASK")
            .relatedEntityId(1L)
            .isRead(false)
            .createdAt(LocalDateTime.now())
            .build();

        // Act
        emailQueueService.queueEmailNotification(task, assignee, notification);

        // Assert
        ArgumentCaptor<EmailNotificationQueue> captor = ArgumentCaptor.forClass(EmailNotificationQueue.class);
        verify(emailQueueRepository).save(captor.capture());

        EmailNotificationQueue saved = captor.getValue();
        assertThat(saved.getNotificationData()).isNotNull();
        assertThat(saved.getNotificationData()).contains("message");
    }

    @Test
    void queueEmailNotification_handlesSerializationError() throws Exception {
        // Arrange - This test verifies that serialization errors are caught
        when(emailPreferenceService.isNotificationTypeEnabled(assignee, NotificationType.TASK_CREATED))
            .thenReturn(true);

        Notification notification = Notification.builder()
            .id(1L)
            .organization(organization)
            .project(project)
            .task(task)
            .user(assignee)
            .type(NotificationType.TASK_CREATED)
            .title("Task Created")
            .message("Test message")
            .actor(actor)
            .relatedEntityType("TASK")
            .relatedEntityId(1L)
            .isRead(false)
            .createdAt(LocalDateTime.now())
            .build();

        // Force the ObjectMapper to throw an exception
        doThrow(new RuntimeException("Serialization error")).when(objectMapper).writeValueAsString(any());

        // Act - should not throw
        emailQueueService.queueEmailNotification(task, assignee, notification);

        // Assert - save should not have been called due to error
        verify(emailQueueRepository, never()).save(any());
    }

    @Test
    void queueEmailNotification_skipsWhenPreferencesDisabled() {
        // Arrange
        when(emailPreferenceService.isNotificationTypeEnabled(assignee, NotificationType.TASK_CREATED))
            .thenReturn(false);

        Notification notification = Notification.builder()
            .id(1L)
            .organization(organization)
            .project(project)
            .task(task)
            .user(assignee)
            .type(NotificationType.TASK_CREATED)
            .title("Task Created")
            .message("Test message")
            .actor(actor)
            .relatedEntityType("TASK")
            .relatedEntityId(1L)
            .isRead(false)
            .createdAt(LocalDateTime.now())
            .build();

        // Act
        emailQueueService.queueEmailNotification(task, assignee, notification);

        // Assert - save should NOT have been called due to disabled preferences
        verify(emailQueueRepository, never()).save(any());
    }
}
