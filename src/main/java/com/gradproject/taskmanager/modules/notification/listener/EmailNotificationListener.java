package com.gradproject.taskmanager.modules.notification.listener;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.notification.domain.Notification;
import com.gradproject.taskmanager.modules.notification.domain.NotificationType;
import com.gradproject.taskmanager.modules.notification.event.*;
import com.gradproject.taskmanager.modules.notification.repository.NotificationRepository;
import com.gradproject.taskmanager.modules.notification.service.EmailQueueService;
import com.gradproject.taskmanager.modules.task.domain.Task;
import com.gradproject.taskmanager.modules.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Set;

/**
 * Event listener for email notifications.
 *
 * <p>Listens to the same NotificationEvents as NotificationEventListener,
 * but instead of creating in-app notifications, it queues email notifications
 * for batch delivery via SendGrid.
 *
 * <p>Email notifications are queued (not sent immediately), allowing the
 * EmailDigestScheduler to batch multiple notifications for the same task
 * into a single digest email sent every minute.
 *
 * <p>Recipients include:
 * <ul>
 *   <li>Task assignee (if not the actor)</li>
 *   <li>Task reporter (if not the actor)</li>
 *   <li>All task watchers (if not the actor)</li>
 *   <li>Mentioned users (for @mention events)</li>
 * </ul>
 *
 * <p>Uses @TransactionalEventListener to ensure emails are queued only
 * after the database transaction commits successfully.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationListener {

    private final EmailQueueService emailQueueService;
    private final NotificationRepository notificationRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    // ==================== Task Created ====================

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTaskCreatedEvent(TaskCreatedEvent event) {
        processNotificationEvent(event);
    }

    // ==================== Task Assignment Events ====================

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTaskAssignedEvent(TaskAssignedEvent event) {
        processNotificationEvent(event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTaskUnassignedEvent(TaskUnassignedEvent event) {
        processNotificationEvent(event);
    }

    // ==================== Task Status Changed ====================

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTaskStatusChangedEvent(TaskStatusChangedEvent event) {
        processNotificationEvent(event);
    }

    // ==================== Task Priority Changed ====================

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTaskPriorityChangedEvent(TaskPriorityChangedEvent event) {
        processNotificationEvent(event);
    }

    // ==================== Task Due Date Changed ====================

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTaskDueDateChangedEvent(TaskDueDateChangedEvent event) {
        processNotificationEvent(event);
    }

    // ==================== Comment Events ====================

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentAddedEvent(CommentAddedEvent event) {
        processNotificationEvent(event);
    }

    // Note: CommentReplyEvent doesn't exist in the codebase yet, but including it for completeness
    // If it exists, uncomment this:
    // @Async
    // @Transactional(readOnly = true)
    // @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    // public void handleCommentReplyEvent(CommentReplyEvent event) {
    //     processNotificationEvent(event);
    // }

    // ==================== Mentioned Event ====================

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserMentionedEvent(MentionedEvent event) {
        processNotificationEvent(event);
    }

    // ==================== Attachment Events ====================

    // Note: AttachmentAddedEvent doesn't exist in the codebase yet
    // If it exists, uncomment this:
    // @Async
    // @Transactional(readOnly = true)
    // @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    // public void handleAttachmentAddedEvent(AttachmentAddedEvent event) {
    //     processNotificationEvent(event);
    // }

    // ==================== Watcher Events ====================

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleWatcherAddedEvent(WatcherAddedEvent event) {
        processNotificationEvent(event);
    }

    // ==================== Common Processing Logic ====================

    /**
     * Process a notification event by determining recipients and queuing emails.
     *
     * <p>This method:
     * <ol>
     *   <li>Re-fetches entities to ensure they're attached to current session</li>
     *   <li>Determines who should receive email notifications</li>
     *   <li>For each recipient, finds the corresponding in-app notification</li>
     *   <li>Queues an email notification for batch delivery</li>
     * </ol>
     *
     * @param event the notification event to process
     */
    private void processNotificationEvent(NotificationEvent event) {
        try {
            // Extract IDs from detached entities (IDs are safe to access)
            Long taskId = event.getTask().getId();
            Integer actorId = event.getActor().getId();
            NotificationType type = event.getType();

            // Re-fetch entities with eager loading to avoid LazyInitializationException
            Task task = taskRepository.findByIdWithAssociations(taskId).orElse(null);
            User actor = userRepository.findById(actorId).orElse(null);

            if (task == null) {
                log.warn("Task {} not found when processing email notification", taskId);
                return;
            }

            if (actor == null) {
                log.warn("Actor {} not found when processing email notification", actorId);
                return;
            }

            // Determine recipients (assignee, reporter, watchers, mentioned users)
            // Automatically excludes the actor
            Set<User> recipients = emailQueueService.determineRecipients(task, type, actor);

            if (recipients.isEmpty()) {
                log.debug("No email recipients for task {} event {}", task.getKey(), type);
                return;
            }

            log.info("Queueing email notifications for task {} to {} recipients (type: {})",
                task.getKey(), recipients.size(), type);

            // For each recipient, find the in-app notification and queue email
            int queuedCount = 0;
            for (User recipient : recipients) {
                try {
                    // The NotificationEventListener has already created notifications
                    // Query for the most recent notification for this task, user, and type
                    Notification notification = notificationRepository.findMostRecentByTaskAndUserAndType(
                        task.getId(),
                        recipient.getId(),
                        type
                    );

                    if (notification != null) {
                        emailQueueService.queueEmailNotification(task, recipient, notification);
                        queuedCount++;
                    } else {
                        log.warn("No in-app notification found for recipient {} on task {} (type: {})",
                            recipient.getUsername(), task.getKey(), type);
                    }
                } catch (Exception e) {
                    log.error("Failed to queue email for recipient {} on task {}: {}",
                        recipient.getUsername(), task.getKey(), e.getMessage());
                    // Continue processing other recipients
                }
            }

            log.debug("Successfully queued {} email notifications for task {}", queuedCount, task.getKey());

        } catch (Exception e) {
            log.error("Failed to process email notifications for event {}: {}",
                event.getClass().getSimpleName(), e.getMessage(), e);
            // Don't throw - email queue failures shouldn't break the main flow
        }
    }
}
