package com.gradproject.taskmanager.modules.notification.service;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.notification.domain.Notification;
import com.gradproject.taskmanager.modules.notification.domain.NotificationType;
import com.gradproject.taskmanager.modules.notification.dto.NotificationResponse;
import com.gradproject.taskmanager.modules.notification.repository.NotificationRepository;
import com.gradproject.taskmanager.modules.project.domain.TaskStatus;
import com.gradproject.taskmanager.modules.task.domain.Comment;
import com.gradproject.taskmanager.modules.task.domain.Task;
import com.gradproject.taskmanager.modules.task.domain.TaskPriority;
import com.gradproject.taskmanager.modules.task.domain.TaskWatcher;
import com.gradproject.taskmanager.modules.task.repository.TaskWatcherRepository;
import com.gradproject.taskmanager.shared.dto.PageResponse;
import com.gradproject.taskmanager.shared.dto.UserSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepo;
    private final TaskWatcherRepository watcherRepo;
    private final UserRepository userRepo;

    

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getUserNotifications(Integer userId, Pageable pageable) {
        Page<Notification> notificationPage = notificationRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return mapToPageResponse(notificationPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getUnreadNotifications(Integer userId, Pageable pageable) {
        Page<Notification> notificationPage = notificationRepo.findUnreadByUserId(userId, pageable);
        return mapToPageResponse(notificationPage);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Integer userId) {
        return notificationRepo.countUnreadByUserId(userId);
    }

    

    @Override
    public void markAsRead(Long notificationId, Integer userId) {
        notificationRepo.markAsRead(notificationId, userId, LocalDateTime.now());
        log.debug("Marked notification {} as read for user {}", notificationId, userId);
    }

    @Override
    public void markAllAsRead(Integer userId) {
        notificationRepo.markAllAsRead(userId, LocalDateTime.now());
        log.info("Marked all notifications as read for user {}", userId);
    }

    

    @Override
    public void notifyTaskCreated(Task task, User creator) {
        String title = "New task created";
        String message = String.format("%s created task %s", creator.getUsername(), task.getKey());

        notifyWatchers(task, NotificationType.TASK_CREATED, title, message, creator, creator.getId());
    }

    @Override
    public void notifyStatusChanged(Task task, TaskStatus oldStatus, TaskStatus newStatus, User actor) {
        String title = "Task status changed";
        String message = String.format("%s changed %s status from %s to %s",
                actor.getUsername(), task.getKey(), oldStatus.getName(), newStatus.getName());

        notifyWatchers(task, NotificationType.STATUS_CHANGED, title, message, actor, actor.getId());
    }

    @Override
    public void notifyTaskAssigned(Task task, User assignee, User actor) {
        String title = "Task assigned to you";
        String message = String.format("%s assigned %s to you", actor.getUsername(), task.getKey());

        
        createNotification(task, assignee, NotificationType.TASK_ASSIGNED, title, message, actor);

        
        String watcherTitle = "Task assigned";
        String watcherMessage = String.format("%s assigned %s to %s",
                actor.getUsername(), task.getKey(), assignee.getUsername());
        notifyWatchers(task, NotificationType.TASK_ASSIGNED, watcherTitle, watcherMessage, actor, assignee.getId());
    }

    @Override
    public void notifyTaskUnassigned(Task task, User previousAssignee, User actor) {
        String title = "Task unassigned";
        String message = String.format("%s unassigned %s", actor.getUsername(), task.getKey());

        notifyWatchers(task, NotificationType.TASK_UNASSIGNED, title, message, actor, actor.getId());
    }

    @Override
    public void notifyPriorityChanged(Task task, TaskPriority oldPriority, TaskPriority newPriority, User actor) {
        String title = "Task priority changed";
        String message = String.format("%s changed %s priority from %s to %s",
                actor.getUsername(), task.getKey(), oldPriority.name(), newPriority.name());

        notifyWatchers(task, NotificationType.PRIORITY_CHANGED, title, message, actor, actor.getId());
    }

    @Override
    public void notifyDueDateChanged(Task task, LocalDate oldDueDate, LocalDate newDueDate, User actor) {
        String title = "Task due date changed";
        String oldDate = oldDueDate != null ? oldDueDate.toString() : "none";
        String newDate = newDueDate != null ? newDueDate.toString() : "none";
        String message = String.format("%s changed %s due date from %s to %s",
                actor.getUsername(), task.getKey(), oldDate, newDate);

        notifyWatchers(task, NotificationType.DUE_DATE_CHANGED, title, message, actor, actor.getId());
    }

    @Override
    public void notifyCommentAdded(Task task, Comment comment, User commenter) {
        String title = comment.isReply() ? "New reply to comment" : "New comment added";
        String truncatedContent = truncateContent(comment.getContent(), 100);
        String message = String.format("%s commented on %s: %s",
                commenter.getUsername(), task.getKey(), truncatedContent);

        notifyWatchers(task, NotificationType.COMMENT_ADDED, title, message, commenter, commenter.getId());
    }

    @Override
    public void notifyMentioned(Task task, Comment comment, Set<String> mentionedUsernames, User commenter) {
        String title = "You were mentioned";
        String truncatedContent = truncateContent(comment.getContent(), 100);
        String message = String.format("%s mentioned you in %s: %s",
                commenter.getUsername(), task.getKey(), truncatedContent);

        
        for (String username : mentionedUsernames) {
            userRepo.findByUsername(username).ifPresent(mentionedUser -> {
                
                if (!mentionedUser.getId().equals(commenter.getId())) {
                    createNotification(task, mentionedUser, NotificationType.MENTIONED, title, message, commenter);
                }
            });
        }
    }

    @Override
    public void notifyWatcherAdded(Task task, User watcher, User addedBy) {
        
        if (!watcher.getId().equals(addedBy.getId())) {
            String title = "Added as watcher";
            String message = String.format("%s added you as a watcher to %s",
                    addedBy.getUsername(), task.getKey());

            createNotification(task, watcher, NotificationType.WATCHER_ADDED, title, message, addedBy);
        }
    }

    

    @Override
    public int cleanupOldNotifications(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        int deleted = notificationRepo.deleteReadNotificationsOlderThan(cutoffDate);
        log.info("Cleaned up {} old read notifications (older than {} days)", deleted, daysOld);
        return deleted;
    }

    

    
    private void notifyWatchers(Task task, NotificationType type, String title, String message,
                                User actor, Integer excludeUserId) {
        List<TaskWatcher> watchers = watcherRepo.findByTaskId(task.getId());

        for (TaskWatcher watcher : watchers) {
            
            if (!watcher.getUser().getId().equals(actor.getId()) &&
                (excludeUserId == null || !watcher.getUser().getId().equals(excludeUserId))) {
                createNotification(task, watcher.getUser(), type, title, message, actor);
            }
        }

        log.debug("Notified {} watchers for task {} (type: {})", watchers.size(), task.getKey(), type);
    }

    
    private void createNotification(Task task, User recipient, NotificationType type,
                                     String title, String message, User actor) {
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

        notificationRepo.save(notification);
        log.debug("Created notification for user {}: {} (type: {})", recipient.getUsername(), title, type);
    }

    
    private String truncateContent(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }

    
    private PageResponse<NotificationResponse> mapToPageResponse(Page<Notification> page) {
        List<NotificationResponse> responses = page.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(
                responses,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    
    private NotificationResponse mapToResponse(Notification notification) {
        UserSummary actorSummary = notification.getActor() != null ?
                new UserSummary(
                        notification.getActor().getId(),
                        notification.getActor().getUsername(),
                        notification.getActor().getEmail(),
                        notification.getActor().getFirstName(),
                        notification.getActor().getLastName(),
                        notification.getActor().getAvatarUrl()
                ) : null;

        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                actorSummary,
                notification.getTask() != null ? notification.getTask().getId() : null,
                notification.getTask() != null ? notification.getTask().getKey() : null,
                notification.getRelatedEntityType(),
                notification.getRelatedEntityId(),
                notification.getIsRead(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }
}
