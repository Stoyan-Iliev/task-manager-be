package com.gradproject.taskmanager.modules.notification.repository;

import com.gradproject.taskmanager.modules.notification.domain.EmailNotificationQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for email notification queue operations.
 *
 * <p>The queue stores email notifications that are batched and sent
 * every minute by the EmailDigestScheduler. This approach reduces
 * email volume by grouping multiple notifications for the same
 * task+recipient into a single digest email.
 */
@Repository
public interface EmailNotificationQueueRepository extends JpaRepository<EmailNotificationQueue, Long> {

    /**
     * Find all pending notifications ready for processing.
     *
     * <p>Returns notifications that are:
     * <ul>
     *   <li>Status = PENDING</li>
     *   <li>Created at or before the cutoff time</li>
     *   <li>Eagerly fetched with task, recipient, and notification entities</li>
     *   <li>Ordered by task_id, recipient_id, created_at for efficient grouping</li>
     * </ul>
     *
     * <p>The cutoff time is typically the current time, allowing the
     * scheduler to process all notifications that have been queued
     * for at least 1 minute (or less, depending on scheduling).
     *
     * @param cutoffTime only process notifications created before this time
     * @return list of pending notifications ready for batch processing
     */
    @Query("""
        SELECT e FROM EmailNotificationQueue e
        JOIN FETCH e.task t
        JOIN FETCH t.project p
        JOIN FETCH t.organization o
        JOIN FETCH t.status ts
        LEFT JOIN FETCH t.assignee
        JOIN FETCH e.recipient r
        LEFT JOIN FETCH e.notification n
        WHERE e.status = 'PENDING'
        AND e.createdAt <= :cutoffTime
        ORDER BY e.task.id, e.recipient.id, e.createdAt
        """)
    List<EmailNotificationQueue> findPendingNotificationsForProcessing(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Find notifications by status and created before a specific time.
     * Useful for cleanup operations or retry logic.
     *
     * @param status the queue status to filter by
     * @param cutoffTime find notifications created before this time
     * @return list of matching notifications
     */
    @Query("SELECT e FROM EmailNotificationQueue e WHERE e.status = :status AND e.createdAt < :cutoffTime")
    List<EmailNotificationQueue> findByStatusAndCreatedAtBefore(
        @Param("status") EmailNotificationQueue.EmailQueueStatus status,
        @Param("cutoffTime") LocalDateTime cutoffTime
    );

    /**
     * Bulk update to mark notifications as sent.
     *
     * @param ids list of notification queue IDs
     * @param sentAt timestamp when emails were sent
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE EmailNotificationQueue e SET e.status = 'SENT', e.sentAt = :sentAt, e.processedAt = :sentAt WHERE e.id IN :ids")
    void markAsSent(@Param("ids") List<Long> ids, @Param("sentAt") LocalDateTime sentAt);

    /**
     * Bulk update to mark notifications as failed with next retry time.
     *
     * @param ids list of notification queue IDs
     * @param errorMessage the error message describing why delivery failed
     * @param processedAt timestamp when processing was attempted
     * @param nextRetryAt when these notifications should be retried
     */
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE EmailNotificationQueue e
        SET e.status = 'FAILED',
            e.errorMessage = :errorMessage,
            e.processedAt = :processedAt,
            e.retryCount = e.retryCount + 1,
            e.nextRetryAt = :nextRetryAt
        WHERE e.id IN :ids
        """)
    void markAsFailed(
        @Param("ids") List<Long> ids,
        @Param("errorMessage") String errorMessage,
        @Param("processedAt") LocalDateTime processedAt,
        @Param("nextRetryAt") LocalDateTime nextRetryAt
    );

    /**
     * Bulk update to mark notifications as permanently failed.
     * Used after max retry attempts have been exhausted.
     *
     * @param ids list of notification queue IDs
     * @param errorMessage the final error message
     * @param processedAt timestamp when processing was attempted
     */
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE EmailNotificationQueue e
        SET e.status = 'PERMANENTLY_FAILED',
            e.errorMessage = :errorMessage,
            e.processedAt = :processedAt,
            e.nextRetryAt = NULL
        WHERE e.id IN :ids
        """)
    void markAsPermanentlyFailed(
        @Param("ids") List<Long> ids,
        @Param("errorMessage") String errorMessage,
        @Param("processedAt") LocalDateTime processedAt
    );

    /**
     * Reset failed notifications to pending for retry.
     *
     * @param ids list of notification queue IDs to reset
     */
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE EmailNotificationQueue e
        SET e.status = 'PENDING',
            e.errorMessage = NULL
        WHERE e.id IN :ids
        """)
    void resetForRetry(@Param("ids") List<Long> ids);

    /**
     * Count notifications by status.
     * Useful for monitoring queue health and alerting.
     *
     * @param status the queue status to count
     * @return number of notifications with the given status
     */
    long countByStatus(EmailNotificationQueue.EmailQueueStatus status);

    /**
     * Find failed notifications that are eligible for retry.
     * Uses nextRetryAt for exponential backoff scheduling.
     *
     * @param maxRetryCount maximum number of retries before giving up
     * @param currentTime current timestamp to check against nextRetryAt
     * @return list of failed notifications eligible for retry
     */
    @Query("""
        SELECT e FROM EmailNotificationQueue e
        JOIN FETCH e.task t
        JOIN FETCH t.project p
        JOIN FETCH t.organization o
        JOIN FETCH t.status ts
        LEFT JOIN FETCH t.assignee
        JOIN FETCH e.recipient r
        LEFT JOIN FETCH e.notification n
        WHERE e.status = 'FAILED'
        AND e.retryCount < :maxRetryCount
        AND (e.nextRetryAt IS NULL OR e.nextRetryAt <= :currentTime)
        ORDER BY e.createdAt
        """)
    List<EmailNotificationQueue> findFailedNotificationsForRetry(
        @Param("maxRetryCount") int maxRetryCount,
        @Param("currentTime") LocalDateTime currentTime
    );

    /**
     * Count notifications by status (for monitoring).
     *
     * @param status the queue status to count
     * @return number of notifications with the given status
     */
    long countByStatusIn(List<EmailNotificationQueue.EmailQueueStatus> statuses);
}
