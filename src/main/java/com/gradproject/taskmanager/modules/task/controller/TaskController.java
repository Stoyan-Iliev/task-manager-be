package com.gradproject.taskmanager.modules.task.controller;

import com.gradproject.taskmanager.modules.task.dto.*;
import com.gradproject.taskmanager.modules.task.service.TaskService;
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
public class TaskController {

    private final TaskService taskService;

    
    @PostMapping("/projects/{projectId}/tasks")
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(
            @PathVariable Long projectId,
            @Valid @RequestBody TaskCreateRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        TaskResponse response = taskService.createTask(projectId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    
    @GetMapping("/projects/{projectId}/tasks")
    public ResponseEntity<ApiResponse<List<TaskSummary>>> listProjectTasks(
            @PathVariable Long projectId,
            @RequestParam(required = false) Long statusId,
            @RequestParam(required = false) Integer assigneeId,
            @RequestParam(required = false) Long sprintId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        List<TaskSummary> tasks = taskService.listProjectTasks(projectId, statusId, assigneeId, sprintId, userId);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    
    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<ApiResponse<TaskResponse>> getTask(@PathVariable Long taskId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        TaskResponse response = taskService.getTask(taskId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    
    @GetMapping("/organizations/{orgId}/tasks/{key}")
    public ResponseEntity<ApiResponse<TaskResponse>> getTaskByKey(
            @PathVariable Long orgId,
            @PathVariable String key) {
        Integer userId = SecurityUtils.getCurrentUserId();
        TaskResponse response = taskService.getTaskByKey(orgId, key, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    
    @PutMapping("/tasks/{taskId}")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskUpdateRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        TaskResponse response = taskService.updateTask(taskId, request, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    
    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable Long taskId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        taskService.deleteTask(taskId, userId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    
    @PostMapping("/tasks/{taskId}/assign")
    public ResponseEntity<ApiResponse<TaskResponse>> assignTask(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskAssignRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        TaskResponse response = taskService.assignTask(taskId, request, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    
    @PostMapping("/tasks/{taskId}/transition")
    public ResponseEntity<ApiResponse<TaskResponse>> transitionStatus(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskTransitionRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        TaskResponse response = taskService.transitionStatus(taskId, request, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    
    @GetMapping("/organizations/{orgId}/tasks/my-open")
    public ResponseEntity<ApiResponse<List<TaskSummary>>> getMyOpenTasks(@PathVariable Long orgId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        List<TaskSummary> tasks = taskService.getMyOpenTasks(orgId, userId);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    
    @GetMapping("/tasks/{parentTaskId}/subtasks")
    public ResponseEntity<ApiResponse<List<TaskSummary>>> getSubtasks(@PathVariable Long parentTaskId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        List<TaskSummary> subtasks = taskService.getSubtasks(parentTaskId, userId);
        return ResponseEntity.ok(ApiResponse.success(subtasks));
    }

    
    @GetMapping("/projects/{projectId}/backlog")
    public ResponseEntity<ApiResponse<List<TaskSummary>>> getBacklog(@PathVariable Long projectId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        List<TaskSummary> backlogTasks = taskService.getBacklogTasks(projectId, userId);
        return ResponseEntity.ok(ApiResponse.success(backlogTasks));
    }
}
