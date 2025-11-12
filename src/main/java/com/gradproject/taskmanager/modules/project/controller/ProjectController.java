package com.gradproject.taskmanager.modules.project.controller;

import com.gradproject.taskmanager.modules.project.dto.ProjectCreateRequest;
import com.gradproject.taskmanager.modules.project.dto.ProjectResponse;
import com.gradproject.taskmanager.modules.project.dto.ProjectUpdateRequest;
import com.gradproject.taskmanager.modules.project.service.ProjectService;
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
public class ProjectController {

    private final ProjectService projectService;

    
    @PostMapping("/organizations/{orgId}/projects")
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @PathVariable Long orgId,
            @Valid @RequestBody ProjectCreateRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        ProjectResponse response = projectService.createProject(orgId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    
    @GetMapping("/organizations/{orgId}/projects")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> listOrganizationProjects(@PathVariable Long orgId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        List<ProjectResponse> projects = projectService.listOrganizationProjects(orgId, userId);
        return ResponseEntity.ok(ApiResponse.success(projects));
    }

    
    @GetMapping("/projects/{projectId}")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProject(@PathVariable Long projectId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        ProjectResponse response = projectService.getProject(projectId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    
    @PutMapping("/projects/{projectId}")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectUpdateRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        ProjectResponse response = projectService.updateProject(projectId, request, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    
    @DeleteMapping("/projects/{projectId}")
    public ResponseEntity<ApiResponse<Void>> deleteProject(@PathVariable Long projectId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        projectService.deleteProject(projectId, userId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    
    @GetMapping("/projects")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> listMyProjects() {
        Integer userId = SecurityUtils.getCurrentUserId();
        List<ProjectResponse> projects = projectService.listUserProjects(userId);
        return ResponseEntity.ok(ApiResponse.success(projects));
    }
}
