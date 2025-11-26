package com.gradproject.taskmanager.modules.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.notification.domain.EmailNotificationQueue;
import com.gradproject.taskmanager.modules.notification.domain.Notification;
import com.gradproject.taskmanager.modules.notification.domain.NotificationType;
import com.gradproject.taskmanager.modules.notification.dto.EmailNotificationData;
import com.gradproject.taskmanager.modules.notification.repository.EmailNotificationQueueRepository;
import com.gradproject.taskmanager.modules.task.domain.Task;
import com.gradproject.taskmanager.modules.task.domain.TaskWatcher;
import com.gradproject.taskmanager.modules.task.repository.TaskWatcherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementation of EmailQueueService.
 *
 * <p>Manages the email notification queue and recipient determination logic.
 * Respects user email preferences when queuing notifications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailQueueServiceImpl implements EmailQueueService {

    private final EmailNotificationQueueRepository emailQueueRepository;
    private final TaskWatcherRepository taskWatcherRepository;
    private final EmailPreferenceService emailPreferenceService;
    private final ObjectMapper objectMapper;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Override
    @Transactional
    public void queueEmailNotification(Task task, User recipient, Notification notification) {
        try {
            // Check if user has email notifications enabled for this type
            if (!emailPreferenceService.isNotificationTypeEnabled(recipient, notification.getType())) {
                log.debug("Email notification skipped for user {} - notification type {} is disabled",
                    recipient.getUsername(), notification.getType());
                return;
            }

            // Create notification data from the in-app notification
            EmailNotificationData data = EmailNotificationData.fromNotification(notification, frontendBaseUrl);

            // Serialize to JSON string for JSONB storage
            String jsonData = objectMapper.writeValueAsString(data.toMap());

            // Create queue entry
            EmailNotificationQueue queueEntry = EmailNotificationQueue.builder()
                .task(task)
                .recipient(recipient)
                .notification(notification)
                .notificationType(notification.getType())
                .notificationData(jsonData)
                .status(EmailNotificationQueue.EmailQueueStatus.PENDING)
                .build();

            emailQueueRepository.save(queueEntry);

            log.debug("Queued email notification for task {} to user {} (type: {})",
                task.getKey(), recipient.getUsername(), notification.getType());

        } catch (Exception e) {
            log.error("Failed to queue email notification for task {} to user {}: {}",
                task.getKey(), recipient.getUsername(), e.getMessage(), e);
            // Don't throw - email queue failures shouldn't break the main flow
        }
    }

    @Override
    public Set<User> determineRecipients(Task task, NotificationType type, User actor) {
        Set<User> recipients = new HashSet<>();

        // Add assignee (if not the actor)
        if (task.getAssignee() != null && !task.getAssignee().equals(actor)) {
            recipients.add(task.getAssignee());
            log.trace("Added assignee {} as recipient", task.getAssignee().getUsername());
        }

        // Add reporter (if not the actor)
        if (task.getReporter() != null && !task.getReporter().equals(actor)) {
            recipients.add(task.getReporter());
            log.trace("Added reporter {} as recipient", task.getReporter().getUsername());
        }

        // Add all watchers (excluding the actor)
        List<TaskWatcher> watchers = taskWatcherRepository.findByTaskIdWithUser(task.getId());
        int watcherCount = 0;
        for (TaskWatcher watcher : watchers) {
            User watcherUser = watcher.getUser();
            if (!watcherUser.equals(actor)) {
                recipients.add(watcherUser);
                watcherCount++;
            }
        }
        if (watcherCount > 0) {
            log.trace("Added {} watchers as recipients", watcherCount);
        }

        // Note: Mentioned users are handled separately by the NotificationService
        // which creates individual notifications for each mentioned user.
        // Those notifications will result in individual queue entries via
        // the EmailNotificationListener.

        log.debug("Determined {} recipients for task {} (type: {}, actor: {})",
            recipients.size(), task.getKey(), type,
            actor != null ? actor.getUsername() : "system");

        return recipients;
    }
}
