package com.gradproject.taskmanager.modules.notification.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.notification.domain.Notification;
import com.gradproject.taskmanager.modules.notification.domain.NotificationType;
import com.gradproject.taskmanager.modules.task.domain.Task;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO containing all data needed to render an email notification.
 *
 * <p>This record is serialized to JSONB and stored in the email queue,
 * allowing emails to be sent without additional database queries.
 * All necessary context (task details, actor info, notification message)
 * is captured at queue time.
 *
 * @param taskKey unique task identifier (e.g., "PROJ-123")
 * @param taskTitle task title
 * @param projectName project name
 * @param organizationName organization name
 * @param actorName username of user who triggered the notification
 * @param actorEmail email of actor (may be null for system notifications)
 * @param notificationType type of notification
 * @param message notification message
 * @param taskUrl frontend URL for viewing the task
 * @param additionalData type-specific metadata (optional)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EmailNotificationData(
    String taskKey,
    String taskTitle,
    String projectName,
    String organizationName,
    String actorName,
    String actorEmail,
    NotificationType notificationType,
    String message,
    String taskUrl,
    Map<String, Object> additionalData
) {

    /**
     * Create EmailNotificationData from a Notification entity.
     *
     * @param notification the notification to convert
     * @param frontendBaseUrl base URL of the frontend application
     * @return EmailNotificationData containing all necessary information
     */
    public static EmailNotificationData fromNotification(Notification notification, String frontendBaseUrl) {
        Task task = notification.getTask();
        User actor = notification.getActor();

        Map<String, Object> additionalData = new HashMap<>();

        // Add metadata if present
        if (notification.getMetadata() != null) {
            additionalData.put("metadata", notification.getMetadata());
        }

        // Add notification title for context
        additionalData.put("title", notification.getTitle());

        // Add related entity information if present
        if (notification.getRelatedEntityType() != null) {
            additionalData.put("relatedEntityType", notification.getRelatedEntityType());
            additionalData.put("relatedEntityId", notification.getRelatedEntityId());
        }

        return new EmailNotificationData(
            task.getKey(),
            task.getTitle(),
            task.getProject().getName(),
            task.getOrganization().getName(),
            actor != null ? actor.getUsername() : "System",
            actor != null ? actor.getEmail() : null,
            notification.getType(),
            notification.getMessage(),
            frontendBaseUrl + "/tasks/" + task.getKey(),
            additionalData
        );
    }

    /**
     * Convert to a simple Map for JSONB storage.
     *
     * @return Map representation suitable for JSONB column
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("taskKey", taskKey);
        map.put("taskTitle", taskTitle);
        map.put("projectName", projectName);
        map.put("organizationName", organizationName);
        map.put("actorName", actorName);
        if (actorEmail != null) {
            map.put("actorEmail", actorEmail);
        }
        map.put("notificationType", notificationType.name());
        map.put("message", message);
        map.put("taskUrl", taskUrl);
        if (additionalData != null && !additionalData.isEmpty()) {
            map.put("additionalData", additionalData);
        }
        return map;
    }

    /**
     * Create EmailNotificationData from a Map (deserialized from JSONB).
     *
     * @param map the map containing notification data
     * @return EmailNotificationData instance
     */
    @SuppressWarnings("unchecked")
    public static EmailNotificationData fromMap(Map<String, Object> map) {
        return new EmailNotificationData(
            (String) map.get("taskKey"),
            (String) map.get("taskTitle"),
            (String) map.get("projectName"),
            (String) map.get("organizationName"),
            (String) map.get("actorName"),
            (String) map.get("actorEmail"),
            NotificationType.valueOf((String) map.get("notificationType")),
            (String) map.get("message"),
            (String) map.get("taskUrl"),
            (Map<String, Object>) map.get("additionalData")
        );
    }
}
