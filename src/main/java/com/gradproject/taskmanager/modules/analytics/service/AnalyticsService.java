package com.gradproject.taskmanager.modules.analytics.service;

import com.gradproject.taskmanager.modules.analytics.dto.*;

import java.time.LocalDate;
import java.util.List;


public interface AnalyticsService {

    
    ProjectMetricsResponse getProjectMetrics(Long projectId, Integer userId);

    
    List<ProjectMetricsResponse> getOrganizationProjectsMetrics(Long organizationId, Integer userId);

    
    OrganizationMetricsResponse getOrganizationMetrics(Long organizationId, Integer userId);

    
    UserActivityResponse getUserActivity(Integer userId, Long organizationId, Integer requestingUserId);

    
    TaskStatusDistributionResponse getTaskStatusDistribution(Long projectId, Integer userId);

    
    TimeRangeMetricsResponse getTimeRangeMetrics(Long organizationId, LocalDate startDate, LocalDate endDate, Integer userId);
}
