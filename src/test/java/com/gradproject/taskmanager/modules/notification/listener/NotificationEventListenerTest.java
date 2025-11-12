package com.gradproject.taskmanager.modules.notification.listener;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.notification.domain.Notification;
import com.gradproject.taskmanager.modules.notification.domain.NotificationType;
import com.gradproject.taskmanager.modules.notification.event.*;
import com.gradproject.taskmanager.modules.notification.repository.NotificationRepository;
import com.gradproject.taskmanager.modules.organization.domain.Organization;
import com.gradproject.taskmanager.modules.project.domain.Project;
import com.gradproject.taskmanager.modules.project.domain.TaskStatus;
import com.gradproject.taskmanager.modules.task.domain.Comment;
import com.gradproject.taskmanager.modules.task.domain.Task;
import com.gradproject.taskmanager.modules.task.domain.TaskPriority;
import com.gradproject.taskmanager.modules.task.domain.TaskWatcher;
import com.gradproject.taskmanager.modules.task.repository.TaskWatcherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class NotificationEventListenerTest {

    @Mock
    private NotificationRepository notificationRepo;

    @Mock
    private TaskWatcherRepository watcherRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationEventListener eventListener;

    private Organization organization;
    private Project project;
    private Task task;
    private User actor;
    private User watcher1;
    private User watcher2;
    private User assignee;

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

        actor = new User();
        actor.setId(1);
        actor.setUsername("actor");
        actor.setEmail("actor@example.com");

        watcher1 = new User();
        watcher1.setId(2);
        watcher1.setUsername("watcher1");
        watcher1.setEmail("watcher1@example.com");

        watcher2 = new User();
        watcher2.setId(3);
        watcher2.setUsername("watcher2");
        watcher2.setEmail("watcher2@example.com");

        assignee = new User();
        assignee.setId(4);
        assignee.setUsername("assignee");
        assignee.setEmail("assignee@example.com");
    }

    

    @Test
    void handleTaskCreated_notifiesWatchers() {
        
        TaskCreatedEvent event = new TaskCreatedEvent(this, task, actor);
        List<TaskWatcher> watchers = List.of(createWatcher(watcher1), createWatcher(watcher2));
        when(watcherRepo.findByTaskId(1L)).thenReturn(watchers);
        when(notificationRepo.save(any(Notification.class))).thenAnswer(i -> {
            Notification notif = i.getArgument(0);
            
            notif.setCreatedAt(LocalDateTime.now());
            return notif;
        });

        
        eventListener.handleTaskCreated(event);

        
        verify(notificationRepo, times(2)).save(any(Notification.class));
        verify(messagingTemplate, times(2)).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void handleTaskCreated_doesNotNotifyActor() {
        
        TaskCreatedEvent event = new TaskCreatedEvent(this, task, actor);
        List<TaskWatcher> watchers = List.of(createWatcher(actor), createWatcher(watcher1));
        when(watcherRepo.findByTaskId(1L)).thenReturn(watchers);
        when(notificationRepo.save(any(Notification.class))).thenAnswer(i -> {
            Notification notif = i.getArgument(0);
            notif.setCreatedAt(LocalDateTime.now());
            return notif;
        });

        
        eventListener.handleTaskCreated(event);

        
        
        verify(notificationRepo, times(1)).save(any(Notification.class));
    }

    

    @Test
    void handleTaskAssigned_notifiesAssignee() {
        
        TaskAssignedEvent event = new TaskAssignedEvent(this, task, assignee, actor);
        when(watcherRepo.findByTaskId(1L)).thenReturn(List.of());
        when(notificationRepo.save(any(Notification.class))).thenAnswer(i -> {
            Notification notif = i.getArgument(0);
            notif.setCreatedAt(LocalDateTime.now());
            return notif;
        });

        
        eventListener.handleTaskAssigned(event);

        
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepo, atLeastOnce()).save(captor.capture());

        Notification assigneeNotification = captor.getAllValues().get(0);
        assertThat(assigneeNotification.getUser()).isEqualTo(assignee);
        assertThat(assigneeNotification.getType()).isEqualTo(NotificationType.TASK_ASSIGNED);
        assertThat(assigneeNotification.getTitle()).contains("Assigned to You");
    }

    @Test
    void handleTaskAssigned_notifiesWatchersExcludingAssigneeAndActor() {
        
        TaskAssignedEvent event = new TaskAssignedEvent(this, task, assignee, actor);
        List<TaskWatcher> watchers = List.of(
                createWatcher(actor),     
                createWatcher(assignee),  
                createWatcher(watcher1)   
        );
        when(watcherRepo.findByTaskId(1L)).thenReturn(watchers);
        when(notificationRepo.save(any(Notification.class))).thenAnswer(i -> {
            Notification notif = i.getArgument(0);
            notif.setCreatedAt(LocalDateTime.now());
            return notif;
        });

        
        eventListener.handleTaskAssigned(event);

        
        
        verify(notificationRepo, times(2)).save(any(Notification.class));
        verify(messagingTemplate, times(2)).convertAndSend(anyString(), any(Object.class));
    }

    

    @Test
    void handleTaskUnassigned_notifiesWatchers() {
        
        TaskUnassignedEvent event = new TaskUnassignedEvent(this, task, assignee, actor);
        List<TaskWatcher> watchers = List.of(createWatcher(watcher1));
        when(watcherRepo.findByTaskId(1L)).thenReturn(watchers);
        when(notificationRepo.save(any(Notification.class))).thenAnswer(i -> {
            Notification notif = i.getArgument(0);
            notif.setCreatedAt(LocalDateTime.now());
            return notif;
        });

        
        eventListener.handleTaskUnassigned(event);

        
        verify(notificationRepo, times(1)).save(any(Notification.class));
        verify(messagingTemplate, times(1)).convertAndSend(anyString(), any(Object.class));
    }

    

    @Test
    void handleTaskStatusChanged_notifiesWatchers() {
        
        TaskStatus oldStatus = createStatus("To Do");
        TaskStatus newStatus = createStatus("In Progress");
        TaskStatusChangedEvent event = new TaskStatusChangedEvent(this, task, oldStatus, newStatus, actor);
        List<TaskWatcher> watchers = List.of(createWatcher(watcher1));
        when(watcherRepo.findByTaskId(1L)).thenReturn(watchers);
        when(notificationRepo.save(any(Notification.class))).thenAnswer(i -> {
            Notification notif = i.getArgument(0);
            notif.setCreatedAt(LocalDateTime.now());
            return notif;
        });

        
        eventListener.handleTaskStatusChanged(event);

        
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepo).save(captor.capture());

        Notification notification = captor.getValue();
        assertThat(notification.getType()).isEqualTo(NotificationType.STATUS_CHANGED);
        assertThat(notification.getMessage()).contains("To Do").contains("In Progress");
    }

    

    @Test
    void handleTaskPriorityChanged_notifiesWatchers() {
        
        TaskPriorityChangedEvent event = new TaskPriorityChangedEvent(
                this, task, TaskPriority.LOW, TaskPriority.HIGH, actor);
        List<TaskWatcher> watchers = List.of(createWatcher(watcher1));
        when(watcherRepo.findByTaskId(1L)).thenReturn(watchers);
        when(notificationRepo.save(any(Notification.class))).thenAnswer(i -> {
            Notification notif = i.getArgument(0);
            notif.setCreatedAt(LocalDateTime.now());
            return notif;
        });

        
        eventListener.handleTaskPriorityChanged(event);

        
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepo).save(captor.capture());

        Notification notification = captor.getValue();
        assertThat(notification.getType()).isEqualTo(NotificationType.PRIORITY_CHANGED);
        assertThat(notification.getMessage()).contains("LOW").contains("HIGH");
    }

    

    @Test
    void handleTaskDueDateChanged_notifiesWatchers() {
        
        LocalDate oldDate = LocalDate.of(2025, 11, 1);
        LocalDate newDate = LocalDate.of(2025, 12, 1);
        TaskDueDateChangedEvent event = new TaskDueDateChangedEvent(this, task, oldDate, newDate, actor);
        List<TaskWatcher> watchers = List.of(createWatcher(watcher1));
        when(watcherRepo.findByTaskId(1L)).thenReturn(watchers);
        when(notificationRepo.save(any(Notification.class))).thenAnswer(i -> {
            Notification notif = i.getArgument(0);
            notif.setCreatedAt(LocalDateTime.now());
            return notif;
        });

        
        eventListener.handleTaskDueDateChanged(event);

        
        verify(notificationRepo, times(1)).save(any(Notification.class));
        verify(messagingTemplate, times(1)).convertAndSend(anyString(), any(Object.class));
    }

    

    @Test
    void handleCommentAdded_notifiesWatchers() {
        
        Comment comment = createComment("Test comment");
        CommentAddedEvent event = new CommentAddedEvent(this, task, comment, actor);
        List<TaskWatcher> watchers = List.of(createWatcher(watcher1));
        when(watcherRepo.findByTaskId(1L)).thenReturn(watchers);
        when(notificationRepo.save(any(Notification.class))).thenAnswer(i -> {
            Notification notif = i.getArgument(0);
            notif.setCreatedAt(LocalDateTime.now());
            return notif;
        });

        
        eventListener.handleCommentAdded(event);

        
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepo).save(captor.capture());

        Notification notification = captor.getValue();
        assertThat(notification.getType()).isEqualTo(NotificationType.COMMENT_ADDED);
        assertThat(notification.getMessage()).contains("Test comment");
    }

    

    @Test
    void handleMentioned_notifiesMentionedUsers() {
        
        Comment comment = createComment("@watcher1 check this out");
        Set<String> mentions = Set.of("watcher1");
        MentionedEvent event = new MentionedEvent(this, task, comment, mentions, actor);
        when(userRepo.findByUsername("watcher1")).thenReturn(Optional.of(watcher1));
        when(notificationRepo.save(any(Notification.class))).thenAnswer(i -> {
            Notification notif = i.getArgument(0);
            notif.setCreatedAt(LocalDateTime.now());
            return notif;
        });

        
        eventListener.handleMentioned(event);

        
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepo).save(captor.capture());

        Notification notification = captor.getValue();
        assertThat(notification.getType()).isEqualTo(NotificationType.MENTIONED);
        assertThat(notification.getUser()).isEqualTo(watcher1);
    }

    @Test
    void handleMentioned_doesNotNotifySelf() {
        
        Comment comment = createComment("@actor mentioning myself");
        Set<String> mentions = Set.of("actor");
        MentionedEvent event = new MentionedEvent(this, task, comment, mentions, actor);
        when(userRepo.findByUsername("actor")).thenReturn(Optional.of(actor));

        
        eventListener.handleMentioned(event);

        
        verify(notificationRepo, never()).save(any(Notification.class));
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void handleMentioned_handlesNonExistentUser() {
        
        Comment comment = createComment("@nonexistent check this");
        Set<String> mentions = Set.of("nonexistent");
        MentionedEvent event = new MentionedEvent(this, task, comment, mentions, actor);
        when(userRepo.findByUsername("nonexistent")).thenReturn(Optional.empty());

        
        eventListener.handleMentioned(event);

        
        verify(notificationRepo, never()).save(any(Notification.class));
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    

    @Test
    void handleWatcherAdded_notifiesWatcher() {
        
        WatcherAddedEvent event = new WatcherAddedEvent(this, task, watcher1, actor);
        when(notificationRepo.save(any(Notification.class))).thenAnswer(i -> {
            Notification notif = i.getArgument(0);
            notif.setCreatedAt(LocalDateTime.now());
            return notif;
        });

        
        eventListener.handleWatcherAdded(event);

        
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepo).save(captor.capture());

        Notification notification = captor.getValue();
        assertThat(notification.getType()).isEqualTo(NotificationType.WATCHER_ADDED);
        assertThat(notification.getUser()).isEqualTo(watcher1);
    }

    @Test
    void handleWatcherAdded_doesNotNotifyIfSelfAdded() {
        
        WatcherAddedEvent event = new WatcherAddedEvent(this, task, watcher1, watcher1);

        
        eventListener.handleWatcherAdded(event);

        
        verify(notificationRepo, never()).save(any(Notification.class));
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    

    @Test
    void sendsWebSocketMessage_toCorrectDestination() {
        
        TaskCreatedEvent event = new TaskCreatedEvent(this, task, actor);
        List<TaskWatcher> watchers = List.of(createWatcher(watcher1));
        when(watcherRepo.findByTaskId(1L)).thenReturn(watchers);
        when(notificationRepo.save(any(Notification.class))).thenAnswer(i -> {
            Notification notif = i.getArgument(0);
            notif.setCreatedAt(LocalDateTime.now());
            return notif;
        });

        
        eventListener.handleTaskCreated(event);

        
        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        verify(messagingTemplate).convertAndSend(destinationCaptor.capture(), any(Object.class));

        String destination = destinationCaptor.getValue();
        assertThat(destination).isEqualTo("/user/watcher1/queue/notifications");
    }

    @Test
    void sendsWebSocketMessage_withCorrectPayload() {
        
        TaskCreatedEvent event = new TaskCreatedEvent(this, task, actor);
        List<TaskWatcher> watchers = List.of(createWatcher(watcher1));
        when(watcherRepo.findByTaskId(1L)).thenReturn(watchers);

        Notification savedNotification = Notification.builder()
                .id(1L)
                .task(task)
                .user(watcher1)
                .type(NotificationType.TASK_CREATED)
                .title("New Task")
                .message("actor created task PROJ-123")
                .actor(actor)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepo.save(any(Notification.class))).thenReturn(savedNotification);

        
        eventListener.handleTaskCreated(event);

        
        verify(messagingTemplate).convertAndSend(
                eq("/user/watcher1/queue/notifications"),
                any(Object.class)
        );
    }

    @Test
    void handlesWebSocketException_gracefully() {
        
        TaskCreatedEvent event = new TaskCreatedEvent(this, task, actor);
        List<TaskWatcher> watchers = List.of(createWatcher(watcher1));
        when(watcherRepo.findByTaskId(1L)).thenReturn(watchers);
        when(notificationRepo.save(any(Notification.class))).thenAnswer(i -> {
            Notification notif = i.getArgument(0);
            notif.setCreatedAt(LocalDateTime.now());
            return notif;
        });
        doThrow(new RuntimeException("WebSocket error")).when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        
        eventListener.handleTaskCreated(event);

        
        verify(notificationRepo).save(any(Notification.class));
    }

    

    private TaskWatcher createWatcher(User user) {
        return TaskWatcher.builder()
                .id(1L)
                .task(task)
                .user(user)
                .addedBy(1)
                .addedAt(LocalDateTime.now())
                .build();
    }

    private TaskStatus createStatus(String name) {
        TaskStatus status = new TaskStatus();
        status.setId(1L);
        status.setName(name);
        status.setProject(project);
        return status;
    }

    private Comment createComment(String content) {
        Comment comment = new Comment();
        comment.setId(1L);
        comment.setContent(content);
        comment.setTask(task);
        comment.setUser(actor);
        comment.setCreatedAt(LocalDateTime.now());
        return comment;
    }
}
