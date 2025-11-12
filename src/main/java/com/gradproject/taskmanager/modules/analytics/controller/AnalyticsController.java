package com.gradproject.taskmanager.modules.analytics.controller;

import com.gradproject.taskmanager.modules.analytics.dto.*;
import com.gradproject.taskmanager.modules.analytics.service.AnalyticsService;
import com.gradproject.taskmanager.shared.dto.ApiResponse;
import com.gradproject.taskmanager.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;


@RestController
@RequestMapping("/api/secure/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    
    @GetMapping("/projects/{projectId}")
    public ResponseEntity<ApiResponse<ProjectMetricsResponse>> getProjectMetrics(@PathVariable Long projectId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        ProjectMetricsResponse metrics = analyticsService.getProjectMetrics(projectId, userId);
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }

    
    @GetMapping("/organizations/{organizationId}/projects")
    public ResponseEntity<ApiResponse<List<ProjectMetricsResponse>>> getOrganizationProjectsMetrics(
            @PathVariable Long organizationId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        List<ProjectMetricsResponse> metrics = analyticsService.getOrganizationProjectsMetrics(organizationId, userId);
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }

    
    @GetMapping("/organizations/{organizationId}")
    public ResponseEntity<ApiResponse<OrganizationMetricsResponse>> getOrganizationMetrics(
            @PathVariable Long organizationId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        OrganizationMetricsResponse metrics = analyticsService.getOrganizationMetrics(organizationId, userId);
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }

    
    @GetMapping("/organizations/{organizationId}/users/{targetUserId}")
    public ResponseEntity<ApiResponse<UserActivityResponse>> getUserActivity(
            @PathVariable Long organizationId,
            @PathVariable Integer targetUserId) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        UserActivityResponse activity = analyticsService.getUserActivity(targetUserId, organizationId, requestingUserId);
        return ResponseEntity.ok(ApiResponse.success(activity));
    }

    
    @GetMapping("/projects/{projectId}/status-distribution")
    public ResponseEntity<ApiResponse<TaskStatusDistributionResponse>> getTaskStatusDistribution(
            @PathVariable Long projectId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        TaskStatusDistributionResponse distribution = analyticsService.getTaskStatusDistribution(projectId, userId);
        return ResponseEntity.ok(ApiResponse.success(distribution));
    }

    
    @GetMapping("/organizations/{organizationId}/time-range")
    public ResponseEntity<ApiResponse<TimeRangeMetricsResponse>> getTimeRangeMetrics(
            @PathVariable Long organizationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Integer userId = SecurityUtils.getCurrentUserId();
        TimeRangeMetricsResponse metrics = analyticsService.getTimeRangeMetrics(organizationId, startDate, endDate, userId);
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }
}
