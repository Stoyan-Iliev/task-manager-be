package com.gradproject.taskmanager.modules.notification.listener;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.notification.domain.Notification;
import com.gradproject.taskmanager.modules.notification.event.*;
import com.gradproject.taskmanager.modules.notification.repository.NotificationRepository;
import com.gradproject.taskmanager.modules.task.domain.TaskWatcher;
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
    private final TaskWatcherRepository watcherRepo;
    private final UserRepository userRepo;
    private final SimpMessagingTemplate messagingTemplate;

    

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTaskCreated(TaskCreatedEvent event) {
        log.debug("Handling TaskCreatedEvent for task: {}", event.getTask().getKey());

        
        notifyWatchers(event);
    }

    

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTaskAssigned(TaskAssignedEvent event) {
        log.debug("Handling TaskAssignedEvent for task: {}", event.getTask().getKey());

        
        Notification assigneeNotif = createNotification(
                event.getTask(),
                event.getAssignee(),
                event.getType(),
                "Task Assigned to You",
                String.format("%s assigned %s to you",
                        event.getActor().getUsername(),
                        event.getTask().getKey()),
                event.getActor()
        );
        sendWebSocketNotification(assigneeNotif);

        
        List<TaskWatcher> watchers = watcherRepo.findByTaskId(event.getTask().getId());
        for (TaskWatcher watcher : watchers) {
            if (!watcher.getUser().getId().equals(event.getActor().getId()) &&
                !watcher.getUser().getId().equals(event.getAssignee().getId())) {

                Notification watcherNotif = createNotification(
                        event.getTask(),
                        watcher.getUser(),
                        event.getType(),
                        "Task Assigned",
                        event.getMessage(),
                        event.getActor()
                );
                sendWebSocketNotification(watcherNotif);
            }
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTaskUnassigned(TaskUnassignedEvent event) {
        log.debug("Handling TaskUnassignedEvent for task: {}", event.getTask().getKey());

        
        notifyWatchers(event);
    }

    

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTaskStatusChanged(TaskStatusChangedEvent event) {
        log.debug("Handling TaskStatusChangedEvent for task: {} (from {} to {})",
                event.getTask().getKey(),
                event.getOldStatus().getName(),
                event.getNewStatus().getName());

        
        notifyWatchers(event);
    }

    

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTaskPriorityChanged(TaskPriorityChangedEvent event) {
        log.debug("Handling TaskPriorityChangedEvent for task: {} (from {} to {})",
                event.getTask().getKey(),
                event.getOldPriority().name(),
                event.getNewPriority().name());

        
        notifyWatchers(event);
    }

    

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTaskDueDateChanged(TaskDueDateChangedEvent event) {
        log.debug("Handling TaskDueDateChangedEvent for task: {}", event.getTask().getKey());

        
        notifyWatchers(event);
    }

    

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentAdded(CommentAddedEvent event) {
        log.debug("Handling CommentAddedEvent for task: {}", event.getTask().getKey());

        
        notifyWatchers(event);
    }

    

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMentioned(MentionedEvent event) {
        log.debug("Handling MentionedEvent for task: {} (mentioned users: {})",
                event.getTask().getKey(),
                event.getMentionedUsernames());

        
        for (String username : event.getMentionedUsernames()) {
            userRepo.findByUsername(username).ifPresent(mentionedUser -> {
                
                if (!mentionedUser.getId().equals(event.getActor().getId())) {
                    Notification notification = createNotification(
                            event.getTask(),
                            mentionedUser,
                            event.getType(),
                            event.getTitle(),
                            event.getMessage(),
                            event.getActor()
                    );
                    sendWebSocketNotification(notification);
                }
            });
        }
    }

    

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleWatcherAdded(WatcherAddedEvent event) {
        log.debug("Handling WatcherAddedEvent for task: {} (watcher: {})",
                event.getTask().getKey(),
                event.getWatcher().getUsername());

        
        if (!event.getWatcher().getId().equals(event.getActor().getId())) {
            Notification notification = createNotification(
                    event.getTask(),
                    event.getWatcher(),
                    event.getType(),
                    event.getTitle(),
                    event.getMessage(),
                    event.getActor()
            );
            sendWebSocketNotification(notification);
        }
    }

    

    
    private void notifyWatchers(NotificationEvent event) {
        List<TaskWatcher> watchers = watcherRepo.findByTaskId(event.getTask().getId());

        for (TaskWatcher watcher : watchers) {
            
            if (!watcher.getUser().getId().equals(event.getActor().getId())) {
                Notification notification = createNotification(
                        event.getTask(),
                        watcher.getUser(),
                        event.getType(),
                        event.getTitle(),
                        event.getMessage(),
                        event.getActor()
                );
                sendWebSocketNotification(notification);
            }
        }

        log.debug("Notified {} watchers for task {} (type: {})",
                watchers.size(),
                event.getTask().getKey(),
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
