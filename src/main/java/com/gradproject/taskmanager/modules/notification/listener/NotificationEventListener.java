package com.gradproject.taskmanager.modules.notification.listener;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.notification.domain.Notification;
import com.gradproject.taskmanager.modules.notification.event.*;
import com.gradproject.taskmanager.modules.notification.repository.NotificationRepository;
import com.gradproject.taskmanager.modules.task.domain.Task;
import com.gradproject.taskmanager.modules.task.domain.TaskWatcher;
import com.gradproject.taskmanager.modules.task.repository.TaskRepository;
import com.gradproject.taskmanager.modules.task.repository.TaskWatcherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;


@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationRepository notificationRepo;
    private final TaskRepository taskRepo;
    private final TaskWatcherRepository watcherRepo;
    private final UserRepository userRepo;
    private final SimpMessagingTemplate messagingTemplate;

    

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTaskCreated(TaskCreatedEvent event) {
        log.debug("Handling TaskCreatedEvent for task ID: {}", event.getTask().getId());

        notifyWatchers(event);
    }

    

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTaskAssigned(TaskAssignedEvent event) {
        // Re-fetch entities with eager loading to avoid LazyInitializationException
        Long taskId = event.getTask().getId();
        Integer actorId = event.getActor().getId();
        Integer assigneeId = event.getAssignee().getId();

        Task task = taskRepo.findByIdWithAssociations(taskId).orElse(null);
        User actor = userRepo.findById(actorId).orElse(null);
        User assignee = userRepo.findById(assigneeId).orElse(null);

        if (task == null || actor == null || assignee == null) {
            log.warn("Task {}, actor {}, or assignee {} not found", taskId, actorId, assigneeId);
            return;
        }

        log.debug("Handling TaskAssignedEvent for task: {}", task.getKey());


        Notification assigneeNotif = createNotification(
                task,
                assignee,
                event.getType(),
                "Task Assigned to You",
                String.format("%s assigned %s to you",
                        actor.getUsername(),
                        task.getKey()),
                actor
        );
        sendWebSocketNotification(assigneeNotif);


        List<TaskWatcher> watchers = watcherRepo.findByTaskIdWithUser(taskId);
        for (TaskWatcher watcher : watchers) {
            if (!watcher.getUser().getId().equals(actorId) &&
                !watcher.getUser().getId().equals(assigneeId)) {

                Notification watcherNotif = createNotification(
                        task,
                        watcher.getUser(),
                        event.getType(),
                        "Task Assigned",
                        event.getMessage(),
                        actor
                );
                sendWebSocketNotification(watcherNotif);
            }
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTaskUnassigned(TaskUnassignedEvent event) {
        log.debug("Handling TaskUnassignedEvent for task ID: {}", event.getTask().getId());

        notifyWatchers(event);
    }

    

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTaskStatusChanged(TaskStatusChangedEvent event) {
        log.debug("Handling TaskStatusChangedEvent for task ID: {}", event.getTask().getId());

        notifyWatchers(event);
    }

    

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTaskPriorityChanged(TaskPriorityChangedEvent event) {
        log.debug("Handling TaskPriorityChangedEvent for task ID: {}", event.getTask().getId());

        notifyWatchers(event);
    }

    

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTaskDueDateChanged(TaskDueDateChangedEvent event) {
        log.debug("Handling TaskDueDateChangedEvent for task ID: {}", event.getTask().getId());

        notifyWatchers(event);
    }

    

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentAdded(CommentAddedEvent event) {
        log.debug("Handling CommentAddedEvent for task ID: {}", event.getTask().getId());

        notifyWatchers(event);
    }

    

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMentioned(MentionedEvent event) {
        // Re-fetch entities with eager loading to avoid LazyInitializationException
        Long taskId = event.getTask().getId();
        Integer actorId = event.getActor().getId();

        Task task = taskRepo.findByIdWithAssociations(taskId).orElse(null);
        User actor = userRepo.findById(actorId).orElse(null);

        if (task == null || actor == null) {
            log.warn("Task {} or actor {} not found when handling mention", taskId, actorId);
            return;
        }

        log.debug("Handling MentionedEvent for task: {} (mentioned users: {})",
                task.getKey(),
                event.getMentionedUsernames());


        for (String username : event.getMentionedUsernames()) {
            userRepo.findByUsername(username).ifPresent(mentionedUser -> {

                if (!mentionedUser.getId().equals(actorId)) {
                    Notification notification = createNotification(
                            task,
                            mentionedUser,
                            event.getType(),
                            event.getTitle(),
                            event.getMessage(),
                            actor
                    );
                    sendWebSocketNotification(notification);
                }
            });
        }
    }

    

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleWatcherAdded(WatcherAddedEvent event) {
        // Re-fetch entities with eager loading to avoid LazyInitializationException
        Long taskId = event.getTask().getId();
        Integer actorId = event.getActor().getId();
        Integer watcherId = event.getWatcher().getId();

        Task task = taskRepo.findByIdWithAssociations(taskId).orElse(null);
        User actor = userRepo.findById(actorId).orElse(null);
        User watcher = userRepo.findById(watcherId).orElse(null);

        if (task == null || actor == null || watcher == null) {
            log.warn("Task {}, actor {}, or watcher {} not found", taskId, actorId, watcherId);
            return;
        }

        log.debug("Handling WatcherAddedEvent for task: {} (watcher: {})",
                task.getKey(),
                watcher.getUsername());


        if (!watcherId.equals(actorId)) {
            Notification notification = createNotification(
                    task,
                    watcher,
                    event.getType(),
                    event.getTitle(),
                    event.getMessage(),
                    actor
            );
            sendWebSocketNotification(notification);
        }
    }

    


    private void notifyWatchers(NotificationEvent event) {
        // Re-fetch entities with eager loading to avoid LazyInitializationException
        Long taskId = event.getTask().getId();
        Integer actorId = event.getActor().getId();

        Task task = taskRepo.findByIdWithAssociations(taskId).orElse(null);
        User actor = userRepo.findById(actorId).orElse(null);

        if (task == null || actor == null) {
            log.warn("Task {} or actor {} not found when notifying watchers", taskId, actorId);
            return;
        }

        // Track notified users to avoid duplicates
        java.util.Set<Integer> notifiedUserIds = new java.util.HashSet<>();
        int notificationCount = 0;

        // Notify assignee (if not the actor)
        if (task.getAssignee() != null && !task.getAssignee().getId().equals(actorId)) {
            Notification notification = createNotification(
                    task,
                    task.getAssignee(),
                    event.getType(),
                    event.getTitle(),
                    event.getMessage(),
                    actor
            );
            sendWebSocketNotification(notification);
            notifiedUserIds.add(task.getAssignee().getId());
            notificationCount++;
        }

        // Notify reporter (if not the actor and not already notified)
        if (task.getReporter() != null &&
            !task.getReporter().getId().equals(actorId) &&
            !notifiedUserIds.contains(task.getReporter().getId())) {
            Notification notification = createNotification(
                    task,
                    task.getReporter(),
                    event.getType(),
                    event.getTitle(),
                    event.getMessage(),
                    actor
            );
            sendWebSocketNotification(notification);
            notifiedUserIds.add(task.getReporter().getId());
            notificationCount++;
        }

        // Notify watchers (excluding actor and already notified users)
        List<TaskWatcher> watchers = watcherRepo.findByTaskIdWithUser(taskId);
        for (TaskWatcher watcher : watchers) {
            Integer watcherUserId = watcher.getUser().getId();
            if (!watcherUserId.equals(actorId) && !notifiedUserIds.contains(watcherUserId)) {
                Notification notification = createNotification(
                        task,
                        watcher.getUser(),
                        event.getType(),
                        event.getTitle(),
                        event.getMessage(),
                        actor
                );
                sendWebSocketNotification(notification);
                notifiedUserIds.add(watcherUserId);
                notificationCount++;
            }
        }

        log.debug("Notified {} users for task {} (type: {})",
                notificationCount,
                task.getKey(),
                event.getType());
    }

    
    private Notification createNotification(
            com.gradproject.taskmanager.modules.task.domain.Task task,
            User recipient,
            com.gradproject.taskmanager.modules.notification.domain.NotificationType type,
            String title,
            String message,
            User actor) {

        Notification notification = Notification.builder()
                .organization(task.getOrganization())
                .project(task.getProject())
                .task(task)
                .user(recipient)
                .type(type)
                .title(title)
                .message(message)
                .actor(actor)
                .relatedEntityType("TASK")
                .relatedEntityId(task.getId())
                .build();

        Notification saved = notificationRepo.save(notification);
        log.debug("Created notification for user {}: {} (type: {})",
                recipient.getUsername(),
                title,
                type);

        return saved;
    }

    
    private void sendWebSocketNotification(Notification notification) {
        try {
            
            NotificationMessage message = new NotificationMessage(
                    notification.getTask().getId(),
                    notification.getTask().getKey(),
                    notification.getMessage(),
                    notification.getType().name(),
                    notification.getCreatedAt().toString()
            );

            
            String destination = "/user/" + notification.getUser().getUsername() + "/queue/notifications";
            messagingTemplate.convertAndSend(destination, message);

            log.debug("Sent WebSocket notification to user {} at destination {}",
                    notification.getUser().getUsername(),
                    destination);
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification to user {}: {}",
                    notification.getUser().getUsername(),
                    e.getMessage(), e);
        }
    }

    
    private record NotificationMessage(
            Long taskId,
            String taskKey,
            String message,
            String type,
            String timestamp
    ) {}
}
