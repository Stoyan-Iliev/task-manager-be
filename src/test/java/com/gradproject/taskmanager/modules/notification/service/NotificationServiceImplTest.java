package com.gradproject.taskmanager.modules.notification.service;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.notification.domain.Notification;
import com.gradproject.taskmanager.modules.notification.domain.NotificationType;
import com.gradproject.taskmanager.modules.notification.dto.NotificationResponse;
import com.gradproject.taskmanager.modules.notification.repository.NotificationRepository;
import com.gradproject.taskmanager.modules.organization.domain.Organization;
import com.gradproject.taskmanager.modules.project.domain.Project;
import com.gradproject.taskmanager.modules.project.domain.TaskStatus;
import com.gradproject.taskmanager.modules.task.domain.Comment;
import com.gradproject.taskmanager.modules.task.domain.Task;
import com.gradproject.taskmanager.modules.task.domain.TaskPriority;
import com.gradproject.taskmanager.modules.task.domain.TaskWatcher;
import com.gradproject.taskmanager.modules.task.repository.TaskWatcherRepository;
import com.gradproject.taskmanager.shared.dto.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepo;

    @Mock
    private TaskWatcherRepository watcherRepo;

    @Mock
    private UserRepository userRepo;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private Organization organization;
    private Project project;
    private Task task;
    private User user1;
    private User user2;
    private User actor;
    private TaskStatus oldStatus;
    private TaskStatus newStatus;

    @BeforeEach
    void setUp() {
        organization = new Organization();
        organization.setId(1L);
        organization.setName("Test Org");

        project = new Project();
        project.setId(1L);
        project.setKey("PROJ");
        project.setOrganization(organization);

        task = new Task();
        task.setId(1L);
        task.setKey("PROJ-123");
        task.setTitle("Test Task");
        task.setOrganization(organization);
        task.setProject(project);

        user1 = new User();
        user1.setId(1);
        user1.setUsername("user1");
        user1.setEmail("user1@example.com");

        user2 = new User();
        user2.setId(2);
        user2.setUsername("user2");
        user2.setEmail("user2@example.com");

        actor = new User();
        actor.setId(3);
        actor.setUsername("actor");
        actor.setEmail("actor@example.com");

        oldStatus = new TaskStatus();
        oldStatus.setId(1L);
        oldStatus.setName("To Do");

        newStatus = new TaskStatus();
        newStatus.setId(2L);
        newStatus.setName("In Progress");

        User reporter = new User();
        reporter.setId(4);
        task.setReporter(reporter);
    }

    

    @Test
    void getUserNotifications_returnsPaginatedResults() {
        
        Notification notification = createNotification(user1, NotificationType.TASK_CREATED);
        Page<Notification> page = new PageImpl<>(List.of(notification), PageRequest.of(0, 10), 1);
        when(notificationRepo.findByUserIdOrderByCreatedAtDesc(eq(1), any(Pageable.class))).thenReturn(page);

        
        PageResponse<NotificationResponse> result = notificationService.getUserNotifications(1, PageRequest.of(0, 10));

        
        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content().get(0).type()).isEqualTo(NotificationType.TASK_CREATED);
    }

    @Test
    void getUnreadNotifications_returnsOnlyUnread() {
        
        Notification notification = createNotification(user1, NotificationType.COMMENT_ADDED);
        Page<Notification> page = new PageImpl<>(List.of(notification), PageRequest.of(0, 10), 1);
        when(notificationRepo.findUnreadByUserId(eq(1), any(Pageable.class))).thenReturn(page);

        
        PageResponse<NotificationResponse> result = notificationService.getUnreadNotifications(1, PageRequest.of(0, 10));

        
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).isRead()).isFalse();
    }

    @Test
    void getUnreadCount_returnsCorrectCount() {
        
        when(notificationRepo.countUnreadByUserId(1)).thenReturn(5L);

        
        long count = notificationService.getUnreadCount(1);

        
        assertThat(count).isEqualTo(5);
    }

    

    @Test
    void markAsRead_updatesNotification() {
        
        notificationService.markAsRead(1L, 1);

        
        verify(notificationRepo).markAsRead(eq(1L), eq(1), any(LocalDateTime.class));
    }

    @Test
    void markAllAsRead_updatesAllNotifications() {
        
        notificationService.markAllAsRead(1);

        
        verify(notificationRepo).markAllAsRead(eq(1), any(LocalDateTime.class));
    }

    

    @Test
    void notifyTaskCreated_notifiesWatchers() {
        
        TaskWatcher watcher1 = createWatcher(user1);
        TaskWatcher watcher2 = createWatcher(user2);
        when(watcherRepo.findByTaskId(1L)).thenReturn(List.of(watcher1, watcher2));

        
        notificationService.notifyTaskCreated(task, actor);

        
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepo, times(2)).save(captor.capture());

        List<Notification> notifications = captor.getAllValues();
        assertThat(notifications).hasSize(2);
        assertThat(notifications.get(0).getType()).isEqualTo(NotificationType.TASK_CREATED);
        assertThat(notifications.get(0).getActor()).isEqualTo(actor);
    }

    @Test
    void notifyTaskCreated_doesNotNotifyActor() {
        
        TaskWatcher actorWatcher = createWatcher(actor);
        TaskWatcher otherWatcher = createWatcher(user1);
        when(watcherRepo.findByTaskId(1L)).thenReturn(List.of(actorWatcher, otherWatcher));

        
        notificationService.notifyTaskCreated(task, actor);

        
        
        verify(notificationRepo, times(1)).save(any(Notification.class));
    }

    @Test
    void notifyStatusChanged_createsCorrectNotification() {
        
        TaskWatcher watcher = createWatcher(user1);
        when(watcherRepo.findByTaskId(1L)).thenReturn(List.of(watcher));

        
        notificationService.notifyStatusChanged(task, oldStatus, newStatus, actor);

        
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepo).save(captor.capture());

        Notification notification = captor.getValue();
        assertThat(notification.getType()).isEqualTo(NotificationType.STATUS_CHANGED);
        assertThat(notification.getMessage()).contains("To Do").contains("In Progress");
    }

    @Test
    void notifyTaskAssigned_notifiesAssigneeAndWatchers() {
        
        TaskWatcher watcher = createWatcher(user1);
        when(watcherRepo.findByTaskId(1L)).thenReturn(List.of(watcher));

        
        notificationService.notifyTaskAssigned(task, user2, actor);

        
        
        verify(notificationRepo, times(2)).save(any(Notification.class));
    }

    @Test
    void notifyTaskUnassigned_notifiesWatchers() {
        
        TaskWatcher watcher = createWatcher(user1);
        when(watcherRepo.findByTaskId(1L)).thenReturn(List.of(watcher));

        
        notificationService.notifyTaskUnassigned(task, user2, actor);

        
        verify(notificationRepo, times(1)).save(any(Notification.class));
    }

    @Test
    void notifyPriorityChanged_notifiesWatchers() {
        
        TaskWatcher watcher = createWatcher(user1);
        when(watcherRepo.findByTaskId(1L)).thenReturn(List.of(watcher));

        
        notificationService.notifyPriorityChanged(task, TaskPriority.LOW, TaskPriority.HIGH, actor);

        
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepo).save(captor.capture());

        Notification notification = captor.getValue();
        assertThat(notification.getType()).isEqualTo(NotificationType.PRIORITY_CHANGED);
        assertThat(notification.getMessage()).contains("LOW").contains("HIGH");
    }

    @Test
    void notifyDueDateChanged_handlesNullDates() {
        
        TaskWatcher watcher = createWatcher(user1);
        when(watcherRepo.findByTaskId(1L)).thenReturn(List.of(watcher));
        LocalDate newDueDate = LocalDate.of(2025, 12, 31);

        
        notificationService.notifyDueDateChanged(task, null, newDueDate, actor);

        
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepo).save(captor.capture());

        Notification notification = captor.getValue();
        assertThat(notification.getType()).isEqualTo(NotificationType.DUE_DATE_CHANGED);
        assertThat(notification.getMessage()).contains("none").contains("2025-12-31");
    }

    @Test
    void notifyCommentAdded_notifiesWatchers() {
        
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setContent("This is a test comment");
        comment.setTask(task);
        comment.setUser(actor);

        TaskWatcher watcher = createWatcher(user1);
        when(watcherRepo.findByTaskId(1L)).thenReturn(List.of(watcher));

        
        notificationService.notifyCommentAdded(task, comment, actor);

        
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepo).save(captor.capture());

        Notification notification = captor.getValue();
        assertThat(notification.getType()).isEqualTo(NotificationType.COMMENT_ADDED);
        assertThat(notification.getMessage()).contains("This is a test comment");
    }

    @Test
    void notifyCommentAdded_truncatesLongContent() {
        
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setContent("A".repeat(150)); 
        comment.setTask(task);
        comment.setUser(actor);

        TaskWatcher watcher = createWatcher(user1);
        when(watcherRepo.findByTaskId(1L)).thenReturn(List.of(watcher));

        
        notificationService.notifyCommentAdded(task, comment, actor);

        
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepo).save(captor.capture());

        Notification notification = captor.getValue();
        assertThat(notification.getMessage()).endsWith("...");
    }

    @Test
    void notifyMentioned_notifiesMentionedUsers() {
        
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setContent("Hey @user1, check this out!");
        comment.setTask(task);
        comment.setUser(actor);

        Set<String> mentions = Set.of("user1");
        when(userRepo.findByUsername("user1")).thenReturn(Optional.of(user1));

        
        notificationService.notifyMentioned(task, comment, mentions, actor);

        
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepo).save(captor.capture());

        Notification notification = captor.getValue();
        assertThat(notification.getType()).isEqualTo(NotificationType.MENTIONED);
        assertThat(notification.getUser()).isEqualTo(user1);
    }

    @Test
    void notifyMentioned_doesNotNotifySelf() {
        
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setContent("I'm @actor mentioning myself");
        comment.setTask(task);
        comment.setUser(actor);

        Set<String> mentions = Set.of("actor");
        when(userRepo.findByUsername("actor")).thenReturn(Optional.of(actor));

        
        notificationService.notifyMentioned(task, comment, mentions, actor);

        
        verify(notificationRepo, never()).save(any(Notification.class));
    }

    @Test
    void notifyMentioned_handlesNonExistentUser() {
        
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setContent("@nonexistent user");
        comment.setTask(task);
        comment.setUser(actor);

        Set<String> mentions = Set.of("nonexistent");
        when(userRepo.findByUsername("nonexistent")).thenReturn(Optional.empty());

        
        notificationService.notifyMentioned(task, comment, mentions, actor);

        
        verify(notificationRepo, never()).save(any(Notification.class));
    }

    @Test
    void notifyWatcherAdded_notifiesWatcherIfAddedByOther() {
        
        notificationService.notifyWatcherAdded(task, user1, actor);

        
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepo).save(captor.capture());

        Notification notification = captor.getValue();
        assertThat(notification.getType()).isEqualTo(NotificationType.WATCHER_ADDED);
        assertThat(notification.getUser()).isEqualTo(user1);
    }

    @Test
    void notifyWatcherAdded_doesNotNotifyIfSelfAdded() {
        
        notificationService.notifyWatcherAdded(task, user1, user1);

        
        verify(notificationRepo, never()).save(any(Notification.class));
    }

    

    @Test
    void cleanupOldNotifications_deletesOldReadNotifications() {
        
        when(notificationRepo.deleteReadNotificationsOlderThan(any(LocalDateTime.class))).thenReturn(10);

        
        int deleted = notificationService.cleanupOldNotifications(30);

        
        assertThat(deleted).isEqualTo(10);
        verify(notificationRepo).deleteReadNotificationsOlderThan(any(LocalDateTime.class));
    }

    

    private Notification createNotification(User user, NotificationType type) {
        return Notification.builder()
                .id(1L)
                .organization(organization)
                .project(project)
                .task(task)
                .user(user)
                .type(type)
                .title("Test Notification")
                .message("Test message")
                .actor(actor)
                .relatedEntityType("TASK")
                .relatedEntityId(1L)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private TaskWatcher createWatcher(User user) {
        return TaskWatcher.builder()
                .id(1L)
                .task(task)
                .user(user)
                .addedBy(3)
                .addedAt(LocalDateTime.now())
                .build();
    }
}
