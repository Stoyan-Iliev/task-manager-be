package com.gradproject.taskmanager.modules.notification.service;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.modules.notification.domain.NotificationType;
import com.gradproject.taskmanager.modules.notification.domain.UserEmailPreference;
import com.gradproject.taskmanager.modules.notification.dto.EmailPreferencesResponse;
import com.gradproject.taskmanager.modules.notification.dto.EmailPreferencesUpdateRequest;
import com.gradproject.taskmanager.modules.notification.repository.UserEmailPreferenceRepository;
import com.gradproject.taskmanager.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of EmailPreferenceService.
 *
 * <p>Manages user email notification preferences with an opt-out model.
 * Users have all notifications enabled by default and can selectively
 * disable specific types.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailPreferenceServiceImpl implements EmailPreferenceService {

    private final UserEmailPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public EmailPreferencesResponse getPreferences(Integer userId) {
        User user = findUserById(userId);
        UserEmailPreference prefs = preferenceRepository.findByUser(user)
            .orElse(UserEmailPreference.createDefault(user));
        return toResponse(prefs);
    }

    @Override
    @Transactional
    public EmailPreferencesResponse updatePreferences(Integer userId, EmailPreferencesUpdateRequest request) {
        User user = findUserById(userId);
        UserEmailPreference prefs = preferenceRepository.findByUser(user)
            .orElseGet(() -> {
                log.info("Creating email preferences for user: {}", user.getUsername());
                return UserEmailPreference.createDefault(user);
            });

        // Update only non-null fields
        if (request.emailEnabled() != null) {
            prefs.setEmailEnabled(request.emailEnabled());
        }
        if (request.taskCreated() != null) {
            prefs.setTaskCreated(request.taskCreated());
        }
        if (request.statusChanged() != null) {
            prefs.setStatusChanged(request.statusChanged());
        }
        if (request.priorityChanged() != null) {
            prefs.setPriorityChanged(request.priorityChanged());
        }
        if (request.dueDateChanged() != null) {
            prefs.setDueDateChanged(request.dueDateChanged());
        }
        if (request.taskAssigned() != null) {
            prefs.setTaskAssigned(request.taskAssigned());
        }
        if (request.taskUnassigned() != null) {
            prefs.setTaskUnassigned(request.taskUnassigned());
        }
        if (request.mentioned() != null) {
            prefs.setMentioned(request.mentioned());
        }
        if (request.commentAdded() != null) {
            prefs.setCommentAdded(request.commentAdded());
        }
        if (request.commentReply() != null) {
            prefs.setCommentReply(request.commentReply());
        }
        if (request.attachmentAdded() != null) {
            prefs.setAttachmentAdded(request.attachmentAdded());
        }
        if (request.watcherAdded() != null) {
            prefs.setWatcherAdded(request.watcherAdded());
        }

        prefs = preferenceRepository.save(prefs);
        log.debug("Updated email preferences for user: {}", user.getUsername());

        return toResponse(prefs);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isNotificationTypeEnabled(User user, NotificationType notificationType) {
        return preferenceRepository.findByUser(user)
            .map(prefs -> prefs.isNotificationTypeEnabled(notificationType))
            .orElse(true); // Default to enabled if no preferences exist
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmailEnabled(User user) {
        return preferenceRepository.findByUser(user)
            .map(UserEmailPreference::isEmailEnabled)
            .orElse(true); // Default to enabled if no preferences exist
    }

    /**
     * Convert entity to response DTO.
     */
    private EmailPreferencesResponse toResponse(UserEmailPreference prefs) {
        return new EmailPreferencesResponse(
            prefs.isEmailEnabled(),
            prefs.isTaskCreated(),
            prefs.isStatusChanged(),
            prefs.isPriorityChanged(),
            prefs.isDueDateChanged(),
            prefs.isTaskAssigned(),
            prefs.isTaskUnassigned(),
            prefs.isMentioned(),
            prefs.isCommentAdded(),
            prefs.isCommentReply(),
            prefs.isAttachmentAdded(),
            prefs.isWatcherAdded()
        );
    }

    /**
     * Find user by ID or throw exception.
     */
    private User findUserById(Integer userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
