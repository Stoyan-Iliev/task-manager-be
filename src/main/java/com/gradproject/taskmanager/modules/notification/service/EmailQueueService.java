package com.gradproject.taskmanager.modules.notification.service;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.notification.domain.Notification;
import com.gradproject.taskmanager.modules.notification.domain.NotificationType;
import com.gradproject.taskmanager.modules.task.domain.Task;

import java.util.Set;

/**
 * Service for managing the email notification queue.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Queue email notifications for batch delivery</li>
 *   <li>Determine recipients based on task relationships</li>
 *   <li>Exclude actors from receiving their own notifications</li>
 * </ul>
 *
 * <p>Recipients include:
 * <ul>
 *   <li>Task assignee</li>
 *   <li>Task reporter (creator)</li>
 *   <li>Task watchers</li>
 *   <li>Mentioned users (for @mention notifications)</li>
 * </ul>
 */
public interface EmailQueueService {

    /**
     * Queue an email notification for a specific recipient.
     *
     * <p>The notification is added to the queue with status PENDING.
     * It will be processed by the EmailDigestScheduler during the
     * next scheduled run (every 60 seconds).
     *
     * @param task the task related to the notification
     * @param recipient the user who should receive the email
     * @param notification the in-app notification containing event details
     */
    void queueEmailNotification(Task task, User recipient, Notification notification);

    /**
     * Determine all users who should receive an email notification.
     *
     * <p>Recipients are determined based on:
     * <ul>
     *   <li>Task assignee (if not the actor)</li>
     *   <li>Task reporter (if not the actor)</li>
     *   <li>All task watchers (if not the actor)</li>
     *   <li>Mentioned users for MENTIONED/COMMENT_REPLY types (handled separately)</li>
     * </ul>
     *
     * <p>The actor (user who triggered the event) is always excluded
     * to avoid self-notifications.
     *
     * @param task the task related to the notification
     * @param type the type of notification
     * @param actor the user who triggered the event (excluded from recipients)
     * @return set of users who should receive email notifications
     */
    Set<User> determineRecipients(Task task, NotificationType type, User actor);
}
