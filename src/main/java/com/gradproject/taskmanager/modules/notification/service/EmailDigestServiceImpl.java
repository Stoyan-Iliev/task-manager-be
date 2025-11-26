package com.gradproject.taskmanager.modules.notification.service;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.notification.domain.EmailNotificationQueue;
import com.gradproject.taskmanager.modules.notification.domain.NotificationType;
import com.gradproject.taskmanager.modules.notification.repository.EmailNotificationQueueRepository;
import com.gradproject.taskmanager.modules.task.domain.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of EmailDigestService.
 *
 * <p>Processes the email notification queue every minute, batching
 * multiple notifications for the same task+recipient into a single
 * digest email.
 *
 * <p>Supports exponential backoff retry for failed emails:
 * <ul>
 *   <li>First retry: after initialDelayMinutes (default 5 min)</li>
 *   <li>Second retry: after initialDelayMinutes * backoffMultiplier (default 10 min)</li>
 *   <li>Third retry: after initialDelayMinutes * backoffMultiplier^2 (default 20 min)</li>
 *   <li>After maxAttempts: marked as PERMANENTLY_FAILED</li>
 * </ul>
 */
@Service
@Slf4j
public class EmailDigestServiceImpl implements EmailDigestService {

    private final EmailNotificationQueueRepository emailQueueRepository;
    private final EmailSenderService emailSenderService;
    private final EmailTemplateService emailTemplateService;

    private final int maxRetryAttempts;
    private final int initialDelayMinutes;
    private final int backoffMultiplier;

    public EmailDigestServiceImpl(
        EmailNotificationQueueRepository emailQueueRepository,
        EmailSenderService emailSenderService,
        EmailTemplateService emailTemplateService,
        @Value("${app.email.retry.max-attempts:3}") int maxRetryAttempts,
        @Value("${app.email.retry.initial-delay-minutes:5}") int initialDelayMinutes,
        @Value("${app.email.retry.backoff-multiplier:2}") int backoffMultiplier
    ) {
        this.emailQueueRepository = emailQueueRepository;
        this.emailSenderService = emailSenderService;
        this.emailTemplateService = emailTemplateService;
        this.maxRetryAttempts = maxRetryAttempts;
        this.initialDelayMinutes = initialDelayMinutes;
        this.backoffMultiplier = backoffMultiplier;
    }

    @Override
    @Transactional
    public void processQueuedNotifications() {
        LocalDateTime cutoffTime = LocalDateTime.now();
        List<EmailNotificationQueue> pendingNotifications =
            emailQueueRepository.findPendingNotificationsForProcessing(cutoffTime);

        if (pendingNotifications.isEmpty()) {
            log.debug("No pending email notifications to process");
            return;
        }

        log.info("Processing {} pending email notifications", pendingNotifications.size());

        // Group by task and recipient
        Map<TaskRecipientKey, List<EmailNotificationQueue>> groupedNotifications =
            pendingNotifications.stream()
                .collect(Collectors.groupingBy(
                    e -> new TaskRecipientKey(e.getTask().getId(), e.getRecipient().getId())
                ));

        log.info("Grouped into {} unique task-recipient combinations", groupedNotifications.size());

        int successCount = 0;
        int failureCount = 0;

        // Process each group
        for (Map.Entry<TaskRecipientKey, List<EmailNotificationQueue>> entry : groupedNotifications.entrySet()) {
            try {
                processDigestEmail(entry.getValue());
                successCount++;
            } catch (Exception e) {
                log.error("Failed to process digest email for task {} recipient {}: {}",
                    entry.getKey().taskId(), entry.getKey().recipientId(), e.getMessage(), e);
                failureCount++;
            }
        }

        log.info("Email digest processing complete: {} successful, {} failed", successCount, failureCount);
    }

    /**
     * Process a batch of notifications for a single task+recipient combination.
     *
     * @param notifications list of notifications for same task and recipient
     */
    private void processDigestEmail(List<EmailNotificationQueue> notifications) {
        if (notifications.isEmpty()) {
            return;
        }

        EmailNotificationQueue first = notifications.get(0);
        Task task = first.getTask();
        User recipient = first.getRecipient();

        try {
            // Generate email subject and content
            String subject = createEmailSubject(task, notifications);
            String htmlContent = emailTemplateService.generateTaskDigestEmail(task, notifications);

            // Send email via SendGrid
            emailSenderService.sendEmail(recipient.getEmail(), subject, htmlContent);

            // Mark all as sent
            List<Long> ids = notifications.stream()
                .map(EmailNotificationQueue::getId)
                .toList();
            emailQueueRepository.markAsSent(ids, LocalDateTime.now());

            log.info("Sent digest email for task {} to {} ({} notifications)",
                task.getKey(), recipient.getUsername(), notifications.size());

        } catch (Exception e) {
            log.error("Failed to send email for task {} to {}: {}",
                task.getKey(), recipient.getUsername(), e.getMessage());

            List<Long> ids = notifications.stream()
                .map(EmailNotificationQueue::getId)
                .toList();
            String errorMessage = truncateErrorMessage(e.getMessage());

            // Check if max retries will be exceeded after this failure
            // Note: retryCount is incremented by markAsFailed, so we check current count + 1
            int currentRetryCount = first.getRetryCount() == null ? 0 : first.getRetryCount();
            int nextRetryCount = currentRetryCount + 1;

            if (nextRetryCount >= maxRetryAttempts) {
                // Max retries exhausted - mark as permanently failed
                emailQueueRepository.markAsPermanentlyFailed(ids, errorMessage, LocalDateTime.now());
                log.warn("Email for task {} to {} permanently failed after {} attempts",
                    task.getKey(), recipient.getUsername(), nextRetryCount);
            } else {
                // Calculate next retry time with exponential backoff
                LocalDateTime nextRetryAt = calculateNextRetryAt(currentRetryCount);
                emailQueueRepository.markAsFailed(ids, errorMessage, LocalDateTime.now(), nextRetryAt);
                log.info("Email for task {} to {} scheduled for retry #{} at {}",
                    task.getKey(), recipient.getUsername(), nextRetryCount, nextRetryAt);
            }

            // Re-throw to be caught by caller
            throw new RuntimeException("Email send failed", e);
        }
    }

    /**
     * Calculate next retry time using exponential backoff.
     *
     * @param currentRetryCount the current retry count (before this failure)
     * @return the calculated next retry time
     */
    private LocalDateTime calculateNextRetryAt(int currentRetryCount) {
        // Exponential backoff: initialDelay * (multiplier ^ retryCount)
        // e.g., with initial=5, multiplier=2: 5, 10, 20 minutes
        long delayMinutes = (long) (initialDelayMinutes * Math.pow(backoffMultiplier, currentRetryCount));
        return LocalDateTime.now().plusMinutes(delayMinutes);
    }

    /**
     * Create email subject line based on notification count and type.
     */
    private String createEmailSubject(Task task, List<EmailNotificationQueue> notifications) {
        if (notifications.size() == 1) {
            EmailNotificationQueue notification = notifications.get(0);
            String typeDescription = getNotificationTypeDescription(notification.getNotificationType());
            return String.format("[%s] %s - %s", task.getKey(), task.getTitle(), typeDescription);
        } else {
            return String.format("[%s] %s - %d updates", task.getKey(), task.getTitle(), notifications.size());
        }
    }

    /**
     * Get human-readable description of notification type.
     */
    private String getNotificationTypeDescription(NotificationType type) {
        return switch (type) {
            case TASK_CREATED -> "New task";
            case TASK_ASSIGNED -> "Assigned to you";
            case TASK_UNASSIGNED -> "Unassigned";
            case STATUS_CHANGED -> "Status changed";
            case PRIORITY_CHANGED -> "Priority changed";
            case DUE_DATE_CHANGED -> "Due date changed";
            case COMMENT_ADDED -> "New comment";
            case COMMENT_REPLY -> "Comment reply";
            case MENTIONED -> "You were mentioned";
            case ATTACHMENT_ADDED -> "Attachment added";
            case WATCHER_ADDED -> "Added as watcher";
        };
    }

    /**
     * Truncate error messages to fit in database column (avoid overflow).
     */
    private String truncateErrorMessage(String message) {
        if (message == null) {
            return "Unknown error";
        }
        if (message.length() <= 500) {
            return message;
        }
        return message.substring(0, 497) + "...";
    }

    /**
     * Key for grouping notifications by task and recipient.
     */
    private record TaskRecipientKey(Long taskId, Integer recipientId) {}
}
