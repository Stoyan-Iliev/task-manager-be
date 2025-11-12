package com.gradproject.taskmanager.modules.project.controller;

import com.gradproject.taskmanager.modules.project.dto.CompleteSprintRequest;
import com.gradproject.taskmanager.modules.project.dto.SprintRequest;
import com.gradproject.taskmanager.modules.project.dto.SprintResponse;
import com.gradproject.taskmanager.modules.project.service.SprintService;
import com.gradproject.taskmanager.shared.dto.ApiResponse;
import com.gradproject.taskmanager.shared.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/secure")
@RequiredArgsConstructor
public class SprintController {

    private final SprintService sprintService;

    
    @PostMapping("/projects/{projectId}/sprints")
    public ResponseEntity<ApiResponse<SprintResponse>> createSprint(
            @PathVariable Long projectId,
            @Valid @RequestBody SprintRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        SprintResponse response = sprintService.createSprint(projectId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    
    @GetMapping("/projects/{projectId}/sprints")
    public ResponseEntity<ApiResponse<List<SprintResponse>>> listSprints(@PathVariable Long projectId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        List<SprintResponse> sprints = sprintService.listProjectSprints(projectId, userId);
        return ResponseEntity.ok(ApiResponse.success(sprints));
    }

    
    @GetMapping("/sprints/{sprintId}")
    public ResponseEntity<ApiResponse<SprintResponse>> getSprint(@PathVariable Long sprintId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        SprintResponse response = sprintService.getSprint(sprintId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    
    @PutMapping("/sprints/{sprintId}")
    public ResponseEntity<ApiResponse<SprintResponse>> updateSprint(
            @PathVariable Long sprintId,
            @Valid @RequestBody SprintRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        SprintResponse response = sprintService.updateSprint(sprintId, request, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    
    @DeleteMapping("/sprints/{sprintId}")
    public ResponseEntity<ApiResponse<Void>> deleteSprint(@PathVariable Long sprintId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        sprintService.deleteSprint(sprintId, userId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    
    @PostMapping("/sprints/{sprintId}/start")
    public ResponseEntity<ApiResponse<SprintResponse>> startSprint(@PathVariable Long sprintId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        SprintResponse response = sprintService.startSprint(sprintId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    
    @PostMapping("/sprints/{sprintId}/complete")
    public ResponseEntity<ApiResponse<SprintResponse>> completeSprint(
            @PathVariable Long sprintId,
            @Valid @RequestBody CompleteSprintRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        SprintResponse response = sprintService.completeSprint(sprintId, request, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    
    @PostMapping("/sprints/{sprintId}/tasks")
    public ResponseEntity<ApiResponse<Map<String, Object>>> assignTasksToSprint(
            @PathVariable Long sprintId,
            @RequestBody Map<String, List<Long>> request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        List<Long> taskIds = request.get("taskIds");
        if (taskIds == null || taskIds.isEmpty()) {
            throw new IllegalArgumentException("taskIds is required and cannot be empty");
        }
        sprintService.assignTasksToSprint(sprintId, taskIds, userId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Tasks assigned successfully", "count", taskIds.size())));
    }

    
    @DeleteMapping("/sprints/tasks")
    public ResponseEntity<ApiResponse<Map<String, Object>>> removeTasksFromSprint(@RequestBody Map<String, List<Long>> request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        List<Long> taskIds = request.get("taskIds");
        if (taskIds == null || taskIds.isEmpty()) {
            throw new IllegalArgumentException("taskIds is required and cannot be empty");
        }
        sprintService.removeTasksFromSprint(taskIds, userId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Tasks removed from sprint successfully", "count", taskIds.size())));
    }

    
    @GetMapping("/sprints/{sprintId}/tasks")
    public ResponseEntity<ApiResponse<List<?>>> getSprintTasks(@PathVariable Long sprintId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        List<?> tasks = sprintService.getSprintTasks(sprintId, userId);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }
}
