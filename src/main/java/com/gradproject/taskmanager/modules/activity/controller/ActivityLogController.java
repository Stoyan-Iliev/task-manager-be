package com.gradproject.taskmanager.modules.activity.controller;

import com.gradproject.taskmanager.modules.activity.domain.ActivityLog;
import com.gradproject.taskmanager.modules.activity.domain.EntityType;
import com.gradproject.taskmanager.modules.activity.dto.ActivityLogResponse;
import com.gradproject.taskmanager.modules.activity.service.ActivityLogService;
import com.gradproject.taskmanager.shared.dto.ApiResponse;
import com.gradproject.taskmanager.shared.mapper.ActivityLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/secure")
@RequiredArgsConstructor
public class ActivityLogController {

    private final ActivityLogService activityLogService;
    private final ActivityLogMapper activityLogMapper;

    
    @GetMapping("/tasks/{taskId}/activity")
    public ResponseEntity<ApiResponse<List<ActivityLogResponse>>> getTaskActivity(
            @PathVariable Long taskId,
            @RequestParam(required = false, defaultValue = "50") Integer limit
    ) {
        
        int cappedLimit = Math.min(limit, 500);

        List<ActivityLog> logs = activityLogService.getTaskActivity(taskId, cappedLimit);
        List<ActivityLogResponse> response = activityLogMapper.toResponseList(logs);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    
    @GetMapping("/projects/{projectId}/activity")
    public ResponseEntity<ApiResponse<List<ActivityLogResponse>>> getProjectActivity(
            @PathVariable Long projectId,
            @RequestParam(required = false, defaultValue = "50") Integer limit
    ) {
        
        int cappedLimit = Math.min(limit, 500);

        List<ActivityLog> logs = activityLogService.getProjectActivity(projectId, cappedLimit);
        List<ActivityLogResponse> response = activityLogMapper.toResponseList(logs);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    
    @GetMapping("/organizations/{organizationId}/activity")
    public ResponseEntity<ApiResponse<List<ActivityLogResponse>>> getOrganizationActivity(
            @PathVariable Long organizationId,
            @RequestParam(required = false, defaultValue = "100") Integer limit
    ) {
        
        int cappedLimit = Math.min(limit, 1000);

        List<ActivityLog> logs = activityLogService.getOrganizationActivity(organizationId, cappedLimit);
        List<ActivityLogResponse> response = activityLogMapper.toResponseList(logs);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    
    @GetMapping("/activity/{entityType}/{entityId}/history")
    public ResponseEntity<ApiResponse<List<ActivityLogResponse>>> getEntityHistory(
            @PathVariable EntityType entityType,
            @PathVariable Long entityId
    ) {
        List<ActivityLog> logs = activityLogService.getEntityHistory(entityType, entityId);
        List<ActivityLogResponse> response = activityLogMapper.toResponseList(logs);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    
    @GetMapping("/activity/{entityType}/{entityId}/version/{version}")
    public ResponseEntity<ApiResponse<List<ActivityLogResponse>>> getEntityStateAtVersion(
            @PathVariable EntityType entityType,
            @PathVariable Long entityId,
            @PathVariable Integer version
    ) {
        List<ActivityLog> logs = activityLogService.getEntityStateAtVersion(entityType, entityId, version);
        List<ActivityLogResponse> response = activityLogMapper.toResponseList(logs);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
