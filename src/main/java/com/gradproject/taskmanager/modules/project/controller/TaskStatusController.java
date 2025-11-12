package com.gradproject.taskmanager.modules.project.controller;

import com.gradproject.taskmanager.modules.project.dto.ReorderStatusesRequest;
import com.gradproject.taskmanager.modules.project.dto.StatusTemplateResponse;
import com.gradproject.taskmanager.modules.project.dto.TaskStatusRequest;
import com.gradproject.taskmanager.modules.project.dto.TaskStatusResponse;
import com.gradproject.taskmanager.modules.project.service.TaskStatusService;
import com.gradproject.taskmanager.shared.dto.ApiResponse;
import com.gradproject.taskmanager.shared.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/secure")
@RequiredArgsConstructor
public class TaskStatusController {

    private final TaskStatusService taskStatusService;

    
    @PostMapping("/projects/{projectId}/statuses")
    public ResponseEntity<ApiResponse<TaskStatusResponse>> createStatus(
            @PathVariable Long projectId,
            @Valid @RequestBody TaskStatusRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        TaskStatusResponse response = taskStatusService.createStatus(projectId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    
    @GetMapping("/projects/{projectId}/statuses")
    public ResponseEntity<ApiResponse<List<TaskStatusResponse>>> listStatuses(@PathVariable Long projectId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        List<TaskStatusResponse> statuses = taskStatusService.listProjectStatuses(projectId, userId);
        return ResponseEntity.ok(ApiResponse.success(statuses));
    }

    
    @PutMapping("/statuses/{statusId}")
    public ResponseEntity<ApiResponse<TaskStatusResponse>> updateStatus(
            @PathVariable Long statusId,
            @Valid @RequestBody TaskStatusRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        TaskStatusResponse response = taskStatusService.updateStatus(statusId, request, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    
    @DeleteMapping("/statuses/{statusId}")
    public ResponseEntity<ApiResponse<Void>> deleteStatus(@PathVariable Long statusId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        taskStatusService.deleteStatus(statusId, userId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    
    @PostMapping("/projects/{projectId}/statuses/reorder")
    public ResponseEntity<ApiResponse<List<TaskStatusResponse>>> reorderStatuses(
            @PathVariable Long projectId,
            @Valid @RequestBody ReorderStatusesRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        List<TaskStatusResponse> statuses = taskStatusService.reorderStatuses(projectId, request, userId);
        return ResponseEntity.ok(ApiResponse.success(statuses));
    }

    
    @GetMapping("/status-templates")
    public ResponseEntity<ApiResponse<List<StatusTemplateResponse>>> getStatusTemplates() {
        List<StatusTemplateResponse> templates = taskStatusService.getStatusTemplates();
        return ResponseEntity.ok(ApiResponse.success(templates));
    }
}
