package com.gradproject.taskmanager.modules.analytics.dto;


public record UserActivityResponse(
    Integer userId,
    String username,
    Long tasksAssigned,
    Long tasksCompleted,
    Long tasksCreated,
    Long commentsCreated,
    Long tasksWatching
) {}
