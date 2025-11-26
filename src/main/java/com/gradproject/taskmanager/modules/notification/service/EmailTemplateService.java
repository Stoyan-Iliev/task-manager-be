package com.gradproject.taskmanager.modules.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gradproject.taskmanager.modules.notification.domain.EmailNotificationQueue;
import com.gradproject.taskmanager.modules.notification.domain.NotificationType;
import com.gradproject.taskmanager.modules.task.domain.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service for generating HTML email templates.
 *
 * <p>Creates professional-looking HTML emails with:
 * <ul>
 *   <li>Task header (key and title)</li>
 *   <li>Task context (project, assignee, status)</li>
 *   <li>Notification list with icons and messages</li>
 *   <li>Call-to-action button linking to task</li>
 *   <li>Responsive design for mobile/desktop</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTemplateService {

    private final ObjectMapper objectMapper;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    /**
     * Generate a digest email for a task with multiple notifications.
     *
     * @param task the task related to the notifications
     * @param notifications list of notifications to include in the digest
     * @return HTML email content
     */
    public String generateTaskDigestEmail(Task task, List<EmailNotificationQueue> notifications) {
        StringBuilder html = new StringBuilder();

        // Email header
        html.append("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Task Update</title>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                        line-height: 1.6;
                        color: #333;
                        margin: 0;
                        padding: 0;
                        background-color: #f5f5f5;
                    }
                    .container {
                        max-width: 600px;
                        margin: 20px auto;
                        background: white;
                        border-radius: 8px;
                        overflow: hidden;
                        box-shadow: 0 2px 8px rgba(0,0,0,0.1);
                    }
                    .header {
                        background: linear-gradient(135deg, #1976d2 0%, #1565c0 100%);
                        color: white;
                        padding: 24px 20px;
                    }
                    .task-key {
                        font-size: 13px;
                        opacity: 0.9;
                        font-weight: 500;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                    }
                    .task-title {
                        font-size: 22px;
                        font-weight: 600;
                        margin-top: 8px;
                        line-height: 1.3;
                    }
                    .content {
                        padding: 24px 20px;
                    }
                    .task-info {
                        background: #f8f9fa;
                        padding: 16px;
                        border-radius: 6px;
                        margin-bottom: 24px;
                        border-left: 4px solid #1976d2;
                    }
                    .task-info-item {
                        margin: 6px 0;
                        font-size: 14px;
                    }
                    .label {
                        font-weight: 600;
                        color: #555;
                        display: inline-block;
                        min-width: 80px;
                    }
                    .notifications-header {
                        font-size: 18px;
                        font-weight: 600;
                        margin: 0 0 16px 0;
                        color: #333;
                    }
                    .notification {
                        background: white;
                        padding: 16px;
                        margin-bottom: 12px;
                        border-radius: 6px;
                        border: 1px solid #e0e0e0;
                        border-left: 4px solid #1976d2;
                    }
                    .notification-type {
                        font-weight: 600;
                        color: #1976d2;
                        font-size: 13px;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                        margin-bottom: 6px;
                    }
                    .notification-icon {
                        display: inline-block;
                        margin-right: 6px;
                        font-size: 16px;
                    }
                    .notification-message {
                        margin: 8px 0 6px 0;
                        color: #444;
                        font-size: 14px;
                        line-height: 1.5;
                    }
                    .notification-actor {
                        font-size: 12px;
                        color: #777;
                        margin-top: 6px;
                    }
                    .footer {
                        background: #fafafa;
                        padding: 24px 20px;
                        text-align: center;
                        border-top: 1px solid #e0e0e0;
                    }
                    .button {
                        display: inline-block;
                        padding: 12px 28px;
                        background: #1976d2;
                        color: white !important;
                        text-decoration: none;
                        border-radius: 6px;
                        font-weight: 600;
                        font-size: 14px;
                        transition: background 0.2s;
                    }
                    .button:hover {
                        background: #1565c0;
                    }
                    .footer-text {
                        margin-top: 20px;
                        font-size: 12px;
                        color: #888;
                        line-height: 1.5;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="task-key">""");

        // Task key and title
        html.append(escapeHtml(task.getKey()));
        html.append("""
                        </div>
                        <div class="task-title">""");
        html.append(escapeHtml(task.getTitle()));
        html.append("""
                        </div>
                    </div>
                    <div class="content">
                        <div class="task-info">
                            <div class="task-info-item">
                                <span class="label">Project:</span>
                                <span>""");
        html.append(escapeHtml(task.getProject().getName()));
        html.append("</span></div>");

        // Assignee
        if (task.getAssignee() != null) {
            html.append("<div class=\"task-info-item\"><span class=\"label\">Assignee:</span><span>");
            html.append(escapeHtml(task.getAssignee().getUsername()));
            html.append("</span></div>");
        }

        // Status
        if (task.getStatus() != null) {
            html.append("<div class=\"task-info-item\"><span class=\"label\">Status:</span><span>");
            html.append(escapeHtml(task.getStatus().getName()));
            html.append("</span></div>");
        }

        // Priority
        if (task.getPriority() != null) {
            html.append("<div class=\"task-info-item\"><span class=\"label\">Priority:</span><span>");
            html.append(escapeHtml(task.getPriority().name()));
            html.append("</span></div>");
        }

        html.append("</div>");

        // Notifications header
        if (notifications.size() == 1) {
            html.append("<h3 class='notifications-header'>Update</h3>");
        } else {
            html.append("<h3 class='notifications-header'>").append(notifications.size()).append(" Updates</h3>");
        }

        // Render each notification
        for (EmailNotificationQueue notification : notifications) {
            html.append("<div class=\"notification\">");
            html.append("<div class=\"notification-type\">");
            html.append("<span class=\"notification-icon\">").append(getNotificationIcon(notification.getNotificationType())).append("</span>");
            html.append(getNotificationTypeLabel(notification.getNotificationType()));
            html.append("</div>");

            // Parse notification data
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = objectMapper.readValue(notification.getNotificationData(), Map.class);
                String message = (String) data.get("message");
                String actorName = (String) data.get("actorName");

                html.append("<div class=\"notification-message\">");
                html.append(escapeHtml(message));
                html.append("</div>");

                if (actorName != null) {
                    html.append("<div class=\"notification-actor\">by ");
                    html.append(escapeHtml(actorName));
                    html.append("</div>");
                }
            } catch (Exception e) {
                log.error("Failed to parse notification data: {}", e.getMessage());
                html.append("<div class=\"notification-message\">Update details unavailable</div>");
            }

            html.append("</div>");
        }

        // Footer with CTA button
        html.append("""
                    </div>
                    <div class="footer">
                        <a href=\"""");
        html.append(frontendBaseUrl).append("/tasks/").append(task.getKey());
        html.append("""
                " class="button">View Task</a>
                        <p class="footer-text">
                            You received this email because you are watching this task, assigned to it, or were mentioned.<br>
                            Task Manager Notifications
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """);

        return html.toString();
    }

    /**
     * Get emoji icon for notification type.
     */
    private String getNotificationIcon(NotificationType type) {
        return switch (type) {
            case TASK_CREATED -> "✨";
            case TASK_ASSIGNED -> "👤";
            case TASK_UNASSIGNED -> "👥";
            case STATUS_CHANGED -> "🔄";
            case PRIORITY_CHANGED -> "⚡";
            case DUE_DATE_CHANGED -> "📅";
            case COMMENT_ADDED -> "💬";
            case COMMENT_REPLY -> "↩️";
            case MENTIONED -> "@";
            case ATTACHMENT_ADDED -> "📎";
            case WATCHER_ADDED -> "👁️";
        };
    }

    /**
     * Get human-readable label for notification type.
     */
    private String getNotificationTypeLabel(NotificationType type) {
        return switch (type) {
            case TASK_CREATED -> "Task Created";
            case TASK_ASSIGNED -> "Task Assigned";
            case TASK_UNASSIGNED -> "Task Unassigned";
            case STATUS_CHANGED -> "Status Changed";
            case PRIORITY_CHANGED -> "Priority Changed";
            case DUE_DATE_CHANGED -> "Due Date Changed";
            case COMMENT_ADDED -> "Comment Added";
            case COMMENT_REPLY -> "Comment Reply";
            case MENTIONED -> "Mentioned";
            case ATTACHMENT_ADDED -> "Attachment Added";
            case WATCHER_ADDED -> "Watcher Added";
        };
    }

    /**
     * Escape HTML special characters to prevent XSS.
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
