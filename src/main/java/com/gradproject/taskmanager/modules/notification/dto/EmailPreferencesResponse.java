package com.gradproject.taskmanager.modules.notification.dto;

/**
 * Response DTO for user email preferences.
 * Contains all notification type toggles and the global email enabled flag.
 */
public record EmailPreferencesResponse(
    boolean emailEnabled,
    // Task lifecycle
    boolean taskCreated,
    boolean statusChanged,
    boolean priorityChanged,
    boolean dueDateChanged,
    // Assignment
    boolean taskAssigned,
    boolean taskUnassigned,
    boolean mentioned,
    // Collaboration
    boolean commentAdded,
    boolean commentReply,
    boolean attachmentAdded,
    boolean watcherAdded
) {}
