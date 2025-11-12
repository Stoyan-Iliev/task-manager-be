package com.gradproject.taskmanager.modules.analytics.dto;


public record ProjectMetricsResponse(
    Long projectId,
    String projectName,
    Long totalTasks,
    Long completedTasks,
    Long inProgressTasks,
    Long todoTasks,
    Long overdueTasks,
    Double completionRate,
    Long totalMembers,
    Long activeReleases
) {}
