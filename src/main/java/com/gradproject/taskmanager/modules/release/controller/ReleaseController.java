package com.gradproject.taskmanager.modules.release.controller;

import com.gradproject.taskmanager.modules.release.domain.enums.ReleaseStatus;
import com.gradproject.taskmanager.modules.release.dto.AddTaskToReleaseRequest;
import com.gradproject.taskmanager.modules.release.dto.CreateReleaseRequest;
import com.gradproject.taskmanager.modules.release.dto.ReleaseResponse;
import com.gradproject.taskmanager.modules.release.dto.UpdateReleaseRequest;
import com.gradproject.taskmanager.modules.release.service.ReleaseService;
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
public class ReleaseController {

    private final ReleaseService releaseService;

    
    @PostMapping("/projects/{projectId}/releases")
    public ResponseEntity<ApiResponse<ReleaseResponse>> createRelease(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateReleaseRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        ReleaseResponse response = releaseService.createRelease(projectId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    
    @GetMapping("/projects/{projectId}/releases")
    public ResponseEntity<ApiResponse<List<ReleaseResponse>>> getProjectReleases(
            @PathVariable Long projectId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        List<ReleaseResponse> releases = releaseService.getProjectReleases(projectId, userId);
        return ResponseEntity.ok(ApiResponse.success(releases));
    }

    
    @GetMapping("/projects/{projectId}/releases/status/{status}")
    public ResponseEntity<ApiResponse<List<ReleaseResponse>>> getProjectReleasesByStatus(
            @PathVariable Long projectId,
            @PathVariable ReleaseStatus status) {
        Integer userId = SecurityUtils.getCurrentUserId();
        List<ReleaseResponse> releases = releaseService.getProjectReleasesByStatus(projectId, status, userId);
        return ResponseEntity.ok(ApiResponse.success(releases));
    }

    
    @GetMapping("/releases/{releaseId}")
    public ResponseEntity<ApiResponse<ReleaseResponse>> getRelease(@PathVariable Long releaseId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        ReleaseResponse response = releaseService.getRelease(releaseId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    
    @PutMapping("/releases/{releaseId}")
    public ResponseEntity<ApiResponse<ReleaseResponse>> updateRelease(
            @PathVariable Long releaseId,
            @Valid @RequestBody UpdateReleaseRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        ReleaseResponse response = releaseService.updateRelease(releaseId, request, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    
    @DeleteMapping("/releases/{releaseId}")
    public ResponseEntity<ApiResponse<Void>> deleteRelease(@PathVariable Long releaseId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        releaseService.deleteRelease(releaseId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    
    @PostMapping("/releases/{releaseId}/tasks")
    public ResponseEntity<ApiResponse<ReleaseResponse>> addTaskToRelease(
            @PathVariable Long releaseId,
            @Valid @RequestBody AddTaskToReleaseRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        ReleaseResponse response = releaseService.addTaskToRelease(releaseId, request.taskId(), userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    
    @DeleteMapping("/releases/{releaseId}/tasks/{taskId}")
    public ResponseEntity<ApiResponse<ReleaseResponse>> removeTaskFromRelease(
            @PathVariable Long releaseId,
            @PathVariable Long taskId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        ReleaseResponse response = releaseService.removeTaskFromRelease(releaseId, taskId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    
    @GetMapping("/releases/{releaseId}/tasks")
    public ResponseEntity<ApiResponse<List<Long>>> getReleaseTasks(@PathVariable Long releaseId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        List<Long> taskIds = releaseService.getReleaseTasks(releaseId, userId);
        return ResponseEntity.ok(ApiResponse.success(taskIds));
    }

    
    @PutMapping("/releases/{releaseId}/release")
    public ResponseEntity<ApiResponse<ReleaseResponse>> markReleaseAsReleased(@PathVariable Long releaseId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        ReleaseResponse response = releaseService.markReleaseAsReleased(releaseId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    
    @PutMapping("/releases/{releaseId}/archive")
    public ResponseEntity<ApiResponse<ReleaseResponse>> archiveRelease(@PathVariable Long releaseId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        ReleaseResponse response = releaseService.archiveRelease(releaseId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
