package com.gradproject.taskmanager.modules.notification.dto;

/**
 * Request DTO for updating user email preferences.
 * All fields are optional - only provided fields will be updated.
 */
public record EmailPreferencesUpdateRequest(
    Boolean emailEnabled,
    // Task lifecycle
    Boolean taskCreated,
    Boolean statusChanged,
    Boolean priorityChanged,
    Boolean dueDateChanged,
    // Assignment
    Boolean taskAssigned,
    Boolean taskUnassigned,
    Boolean mentioned,
    // Collaboration
    Boolean commentAdded,
    Boolean commentReply,
    Boolean attachmentAdded,
    Boolean watcherAdded
) {}
