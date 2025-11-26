package com.gradproject.taskmanager.modules.notification.service;

/**
 * Service for processing queued email notifications in batches.
 *
 * <p>This service is called by the EmailDigestScheduler every minute.
 * It groups all pending notifications by task and recipient, then
 * sends a single digest email for each unique task+recipient combination.
 *
 * <p>Batching strategy:
 * <ul>
 *   <li>Process all PENDING notifications created before current time</li>
 *   <li>Group by (task_id, recipient_id)</li>
 *   <li>Generate single digest email per group</li>
 *   <li>Mark as SENT on success or FAILED on error</li>
 * </ul>
 */
public interface EmailDigestService {

    /**
     * Process all pending email notifications in the queue.
     *
     * <p>This method:
     * <ol>
     *   <li>Queries all PENDING notifications created before now</li>
     *   <li>Groups them by task and recipient</li>
     *   <li>For each group, generates a digest email</li>
     *   <li>Sends the email via EmailSenderService</li>
     *   <li>Marks notifications as SENT or FAILED</li>
     * </ol>
     *
     * <p>Called by EmailDigestScheduler every minute.
     */
    void processQueuedNotifications();
}
