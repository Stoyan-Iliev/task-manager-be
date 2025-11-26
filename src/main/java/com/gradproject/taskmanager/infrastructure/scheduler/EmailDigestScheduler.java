package com.gradproject.taskmanager.infrastructure.scheduler;

import com.gradproject.taskmanager.modules.notification.service.EmailDigestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job for processing email notification queue.
 *
 * <p>Runs every 60 seconds (1 minute) to process pending email notifications.
 * All notifications queued during the previous minute for the same task+recipient
 * are batched into a single digest email.
 *
 * <p>Can be disabled by setting:
 * <pre>
 * app.email.digest.enabled=false
 * </pre>
 *
 * <p>Scheduling details:
 * <ul>
 *   <li>Fixed delay: 60000ms (1 minute) between job completions</li>
 *   <li>Initial delay: 30000ms (30 seconds) after application startup</li>
 *   <li>Async execution: Configured via @EnableAsync in AsyncConfig</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    prefix = "app.email.digest",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true // Enabled by default
)
public class EmailDigestScheduler {

    private final EmailDigestService emailDigestService;

    /**
     * Process pending email notifications every 60 seconds.
     *
     * <p>This method:
     * <ol>
     *   <li>Queries all PENDING notifications in the queue</li>
     *   <li>Groups them by task and recipient</li>
     *   <li>Generates digest emails for each group</li>
     *   <li>Sends emails via SendGrid</li>
     *   <li>Marks notifications as SENT or FAILED</li>
     * </ol>
     *
     * <p>Errors are logged but don't prevent future executions.
     * Failed emails are marked as FAILED with error messages for retry.
     */
    @Scheduled(
        fixedDelay = 60000,      // 60 seconds between job completions
        initialDelay = 30000     // 30 seconds after startup
    )
    public void processEmailQueue() {
        log.debug("Starting scheduled email digest processing");

        try {
            emailDigestService.processQueuedNotifications();
        } catch (Exception e) {
            // Log error but don't throw - scheduler should continue
            log.error("Error processing email queue: {}", e.getMessage(), e);
        }

        log.debug("Completed scheduled email digest processing");
    }
}
