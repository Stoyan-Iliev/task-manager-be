package com.gradproject.taskmanager.modules.analytics.dto;

import java.time.LocalDate;


public record TimeRangeMetricsResponse(
    LocalDate startDate,
    LocalDate endDate,
    Long tasksCreated,
    Long tasksCompleted,
    Long commentsCreated,
    Long activitiesLogged
) {}
