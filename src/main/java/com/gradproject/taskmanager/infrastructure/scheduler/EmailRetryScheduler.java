package com.gradproject.taskmanager.infrastructure.scheduler;

import com.gradproject.taskmanager.modules.notification.domain.EmailNotificationQueue;
import com.gradproject.taskmanager.modules.notification.repository.EmailNotificationQueueRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job for retrying failed email notifications.
 *
 * <p>Runs every 5 minutes to find failed email notifications that are
 * eligible for retry (based on exponential backoff schedule) and resets
 * them to PENDING status so they can be processed by the EmailDigestScheduler.
 *
 * <p>Retry eligibility:
 * <ul>
 *   <li>Status must be FAILED (not PERMANENTLY_FAILED)</li>
 *   <li>Retry count must be less than max attempts</li>
 *   <li>Next retry time must be in the past (exponential backoff)</li>
 * </ul>
 *
 * <p>Can be disabled by setting:
 * <pre>
 * app.email.retry.enabled=false
 * </pre>
 *
 * <p>Scheduling details:
 * <ul>
 *   <li>Fixed delay: 300000ms (5 minutes) between job completions</li>
 *   <li>Initial delay: 60000ms (1 minute) after application startup</li>
 * </ul>
 */
@Component
@Slf4j
@ConditionalOnProperty(
    prefix = "app.email.retry",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true // Enabled by default
)
public class EmailRetryScheduler {

    private final EmailNotificationQueueRepository emailQueueRepository;
    private final int maxRetryAttempts;

    public EmailRetryScheduler(
        EmailNotificationQueueRepository emailQueueRepository,
        @Value("${app.email.retry.max-attempts:3}") int maxRetryAttempts
    ) {
        this.emailQueueRepository = emailQueueRepository;
        this.maxRetryAttempts = maxRetryAttempts;
        log.info("EmailRetryScheduler initialized with max retry attempts: {}", maxRetryAttempts);
    }

    /**
     * Process failed email notifications for retry every 5 minutes.
     *
     * <p>This method:
     * <ol>
     *   <li>Queries all FAILED notifications eligible for retry</li>
     *   <li>Checks that retry count is below max attempts</li>
     *   <li>Checks that next_retry_at has passed (exponential backoff)</li>
     *   <li>Resets eligible notifications to PENDING status</li>
     *   <li>The EmailDigestScheduler will then pick them up for reprocessing</li>
     * </ol>
     *
     * <p>Errors are logged but don't prevent future executions.
     */
    @Scheduled(
        fixedDelay = 300000,     // 5 minutes between job completions
        initialDelay = 60000    // 1 minute after startup
    )
    @Transactional
    public void processFailedNotifications() {
        log.debug("Starting scheduled email retry processing");

        try {
            LocalDateTime now = LocalDateTime.now();
            List<EmailNotificationQueue> failedNotifications =
                emailQueueRepository.findFailedNotificationsForRetry(maxRetryAttempts, now);

            if (failedNotifications.isEmpty()) {
                log.debug("No failed email notifications eligible for retry");
                return;
            }

            log.info("Found {} failed email notifications eligible for retry", failedNotifications.size());

            // Reset all eligible notifications to PENDING
            List<Long> ids = failedNotifications.stream()
                .map(EmailNotificationQueue::getId)
                .toList();
            emailQueueRepository.resetForRetry(ids);

            log.info("Reset {} notifications to PENDING for retry", ids.size());

        } catch (Exception e) {
            // Log error but don't throw - scheduler should continue
            log.error("Error processing failed email notifications for retry: {}", e.getMessage(), e);
        }

        log.debug("Completed scheduled email retry processing");
    }
}
