package com.gradproject.taskmanager.modules.notification.domain;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.task.domain.Task;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Email notification queue for batched email delivery via SendGrid.
 *
 * <p>Email notifications are queued rather than sent immediately, allowing
 * the system to batch multiple notifications for the same task+recipient
 * into a single email digest. A scheduled job processes the queue every
 * minute and sends aggregated emails.
 *
 * <p>Queue entry lifecycle:
 * <ul>
 *   <li>PENDING: Notification queued, awaiting processing</li>
 *   <li>SENT: Email successfully delivered via SendGrid</li>
 *   <li>FAILED: Delivery failed after retry attempts</li>
 * </ul>
 *
 * <p>The notification_data JSONB field contains all necessary information
 * to render the email without additional database queries:
 * - taskKey, taskTitle, projectName, organizationName
 * - actorName, actorEmail (user who triggered the event)
 * - message (notification message)
 * - taskUrl (frontend link)
 * - additionalData (type-specific metadata)
 */
@Entity
@Table(name = "email_notification_queue")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"task", "recipient", "notification"})
public class EmailNotificationQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private User recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id")
    private Notification notification;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "notification_data", nullable = false, columnDefinition = "jsonb")
    private String notificationData;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private EmailQueueStatus status = EmailQueueStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = EmailQueueStatus.PENDING;
        }
        if (retryCount == null) {
            retryCount = 0;
        }
    }

    /**
     * Status of email notification in the queue.
     */
    public enum EmailQueueStatus {
        /** Queued and awaiting processing */
        PENDING,
        /** Successfully sent via SendGrid */
        SENT,
        /** Failed to send, eligible for retry */
        FAILED,
        /** Permanently failed after max retry attempts */
        PERMANENTLY_FAILED
    }

    /**
     * Mark as successfully sent.
     */
    public void markAsSent() {
        this.status = EmailQueueStatus.SENT;
        this.sentAt = LocalDateTime.now();
        this.processedAt = LocalDateTime.now();
        this.nextRetryAt = null;
    }

    /**
     * Mark as failed with error message and schedule next retry.
     *
     * @param errorMessage the error message describing why delivery failed
     * @param nextRetryAt when this notification should be retried (null if no more retries)
     */
    public void markAsFailed(String errorMessage, LocalDateTime nextRetryAt) {
        this.status = EmailQueueStatus.FAILED;
        this.errorMessage = errorMessage;
        this.processedAt = LocalDateTime.now();
        this.retryCount = (this.retryCount == null ? 0 : this.retryCount) + 1;
        this.nextRetryAt = nextRetryAt;
    }

    /**
     * Mark as failed with error message (legacy method for backwards compatibility).
     */
    public void markAsFailed(String errorMessage) {
        markAsFailed(errorMessage, null);
    }

    /**
     * Mark as permanently failed after exhausting retry attempts.
     */
    public void markAsPermanentlyFailed(String errorMessage) {
        this.status = EmailQueueStatus.PERMANENTLY_FAILED;
        this.errorMessage = errorMessage;
        this.processedAt = LocalDateTime.now();
        this.nextRetryAt = null;
    }

    /**
     * Reset to pending status for retry.
     */
    public void resetForRetry() {
        this.status = EmailQueueStatus.PENDING;
        this.errorMessage = null;
    }

    /**
     * Calculate next retry time using exponential backoff.
     *
     * @param initialDelayMinutes base delay in minutes
     * @param backoffMultiplier multiplier for each retry (e.g., 2 for doubling)
     * @return the calculated next retry time
     */
    public LocalDateTime calculateNextRetryAt(int initialDelayMinutes, int backoffMultiplier) {
        int currentRetry = this.retryCount == null ? 0 : this.retryCount;
        // Exponential backoff: initialDelay * (multiplier ^ retryCount)
        // e.g., with initial=5, multiplier=2: 5, 10, 20 minutes
        long delayMinutes = (long) (initialDelayMinutes * Math.pow(backoffMultiplier, currentRetry));
        return LocalDateTime.now().plusMinutes(delayMinutes);
    }

    /**
     * Check if this notification can be retried.
     *
     * @param maxAttempts maximum number of retry attempts allowed
     * @return true if retry count is below max attempts
     */
    public boolean canRetry(int maxAttempts) {
        int currentRetry = this.retryCount == null ? 0 : this.retryCount;
        return currentRetry < maxAttempts;
    }
}
