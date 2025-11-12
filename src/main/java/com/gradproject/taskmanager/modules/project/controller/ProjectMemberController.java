package com.gradproject.taskmanager.modules.project.controller;

import com.gradproject.taskmanager.modules.project.dto.AddProjectMemberRequest;
import com.gradproject.taskmanager.modules.project.dto.ProjectMemberResponse;
import com.gradproject.taskmanager.modules.project.dto.UpdateProjectMemberRoleRequest;
import com.gradproject.taskmanager.modules.project.service.ProjectMemberService;
import com.gradproject.taskmanager.shared.dto.ApiResponse;
import com.gradproject.taskmanager.shared.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/secure/projects/{projectId}/members")
@RequiredArgsConstructor
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    
    @PostMapping
    public ResponseEntity<ApiResponse<ProjectMemberResponse>> addMember(
            @PathVariable Long projectId,
            @Valid @RequestBody AddProjectMemberRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        ProjectMemberResponse response = projectMemberService.addMember(projectId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectMemberResponse>>> listMembers(@PathVariable Long projectId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        List<ProjectMemberResponse> members = projectMemberService.listProjectMembers(projectId, userId);
        return ResponseEntity.ok(ApiResponse.success(members));
    }

    
    @PutMapping("/{userId}/role")
    public ResponseEntity<ApiResponse<ProjectMemberResponse>> updateMemberRole(
            @PathVariable Long projectId,
            @PathVariable Integer userId,
            @Valid @RequestBody UpdateProjectMemberRoleRequest request) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        ProjectMemberResponse response = projectMemberService.updateMemberRole(
                projectId, userId, request.role(), currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable Long projectId,
            @PathVariable Integer userId) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        projectMemberService.removeMember(projectId, userId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
