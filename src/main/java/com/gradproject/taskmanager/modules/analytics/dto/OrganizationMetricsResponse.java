package com.gradproject.taskmanager.modules.analytics.dto;


public record OrganizationMetricsResponse(
    Long organizationId,
    String organizationName,
    Long totalProjects,
    Long totalTasks,
    Long completedTasks,
    Long totalMembers,
    Long activeProjects,
    Double overallCompletionRate
) {}
