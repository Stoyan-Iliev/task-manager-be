package com.gradproject.taskmanager.modules.notification.service;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.notification.domain.NotificationType;
import com.gradproject.taskmanager.modules.notification.dto.EmailPreferencesResponse;
import com.gradproject.taskmanager.modules.notification.dto.EmailPreferencesUpdateRequest;

/**
 * Service for managing user email notification preferences.
 */
public interface EmailPreferenceService {

    /**
     * Get email preferences for a user by ID.
     * Creates default preferences if none exist.
     *
     * @param userId the user ID to get preferences for
     * @return the user's email preferences
     */
    EmailPreferencesResponse getPreferences(Integer userId);

    /**
     * Update email preferences for a user by ID.
     * Creates preferences if none exist.
     *
     * @param userId the user ID to update preferences for
     * @param request the update request with new preference values
     * @return the updated email preferences
     */
    EmailPreferencesResponse updatePreferences(Integer userId, EmailPreferencesUpdateRequest request);

    /**
     * Check if a specific notification type is enabled for a user.
     *
     * @param user the user to check
     * @param notificationType the notification type to check
     * @return true if emails should be sent for this notification type
     */
    boolean isNotificationTypeEnabled(User user, NotificationType notificationType);

    /**
     * Check if emails are globally enabled for a user.
     *
     * @param user the user to check
     * @return true if email notifications are enabled
     */
    boolean isEmailEnabled(User user);
}
