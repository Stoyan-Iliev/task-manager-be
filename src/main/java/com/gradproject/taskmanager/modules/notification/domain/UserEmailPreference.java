package com.gradproject.taskmanager.modules.notification.domain;

import com.gradproject.taskmanager.modules.auth.domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * User email notification preferences.
 *
 * <p>Controls which types of email notifications a user receives.
 * Each user has one preference record that determines their email
 * notification settings for all 11 notification types.
 *
 * <p>By default, all notification types are enabled (opt-out model).
 * Users can disable specific types they don't want to receive.
 */
@Entity
@Table(name = "user_email_preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEmailPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user these preferences belong to.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // === Task Lifecycle Notifications ===

    /**
     * Receive emails when a task is created.
     */
    @Column(name = "task_created", nullable = false)
    @Builder.Default
    private boolean taskCreated = true;

    /**
     * Receive emails when a task status changes.
     */
    @Column(name = "status_changed", nullable = false)
    @Builder.Default
    private boolean statusChanged = true;

    /**
     * Receive emails when task priority changes.
     */
    @Column(name = "priority_changed", nullable = false)
    @Builder.Default
    private boolean priorityChanged = true;

    /**
     * Receive emails when task due date changes.
     */
    @Column(name = "due_date_changed", nullable = false)
    @Builder.Default
    private boolean dueDateChanged = true;

    // === Assignment Notifications ===

    /**
     * Receive emails when assigned to a task.
     */
    @Column(name = "task_assigned", nullable = false)
    @Builder.Default
    private boolean taskAssigned = true;

    /**
     * Receive emails when unassigned from a task.
     */
    @Column(name = "task_unassigned", nullable = false)
    @Builder.Default
    private boolean taskUnassigned = true;

    /**
     * Receive emails when mentioned in a task or comment.
     */
    @Column(name = "mentioned", nullable = false)
    @Builder.Default
    private boolean mentioned = true;

    // === Collaboration Notifications ===

    /**
     * Receive emails when a comment is added.
     */
    @Column(name = "comment_added", nullable = false)
    @Builder.Default
    private boolean commentAdded = true;

    /**
     * Receive emails when someone replies to a comment.
     */
    @Column(name = "comment_reply", nullable = false)
    @Builder.Default
    private boolean commentReply = true;

    /**
     * Receive emails when an attachment is added.
     */
    @Column(name = "attachment_added", nullable = false)
    @Builder.Default
    private boolean attachmentAdded = true;

    /**
     * Receive emails when added as a watcher.
     */
    @Column(name = "watcher_added", nullable = false)
    @Builder.Default
    private boolean watcherAdded = true;

    // === Global Settings ===

    /**
     * Master toggle to disable all email notifications.
     * When false, no emails are sent regardless of individual settings.
     */
    @Column(name = "email_enabled", nullable = false)
    @Builder.Default
    private boolean emailEnabled = true;

    // === Audit Fields ===

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if a specific notification type is enabled for email.
     *
     * @param type the notification type to check
     * @return true if emails should be sent for this type
     */
    public boolean isNotificationTypeEnabled(NotificationType type) {
        if (!emailEnabled) {
            return false;
        }

        return switch (type) {
            case TASK_CREATED -> taskCreated;
            case STATUS_CHANGED -> statusChanged;
            case PRIORITY_CHANGED -> priorityChanged;
            case DUE_DATE_CHANGED -> dueDateChanged;
            case TASK_ASSIGNED -> taskAssigned;
            case TASK_UNASSIGNED -> taskUnassigned;
            case MENTIONED -> mentioned;
            case COMMENT_ADDED -> commentAdded;
            case COMMENT_REPLY -> commentReply;
            case ATTACHMENT_ADDED -> attachmentAdded;
            case WATCHER_ADDED -> watcherAdded;
        };
    }

    /**
     * Create default preferences for a user with all notifications enabled.
     *
     * @param user the user to create preferences for
     * @return new UserEmailPreference with default settings
     */
    public static UserEmailPreference createDefault(User user) {
        return UserEmailPreference.builder()
            .user(user)
            .emailEnabled(true)
            .taskCreated(true)
            .statusChanged(true)
            .priorityChanged(true)
            .dueDateChanged(true)
            .taskAssigned(true)
            .taskUnassigned(true)
            .mentioned(true)
            .commentAdded(true)
            .commentReply(true)
            .attachmentAdded(true)
            .watcherAdded(true)
            .build();
    }
}
