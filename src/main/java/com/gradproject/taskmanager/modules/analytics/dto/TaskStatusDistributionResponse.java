package com.gradproject.taskmanager.modules.analytics.dto;

import java.util.Map;


public record TaskStatusDistributionResponse(
    Long projectId,
    String projectName,
    Map<String, Long> statusDistribution
) {}
