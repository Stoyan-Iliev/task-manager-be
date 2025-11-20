package com.gradproject.taskmanager.modules.git.controller;

import com.gradproject.taskmanager.modules.git.dto.request.CreateBranchRequest;
import com.gradproject.taskmanager.modules.git.dto.response.BranchResponse;
import com.gradproject.taskmanager.modules.git.service.GitBranchService;
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
public class GitBranchController {

    private final GitBranchService gitBranchService;

    /**
     * Create a new branch for a task
     * POST /api/secure/tasks/{taskId}/git-branches
     */
    @PostMapping("/tasks/{taskId}/git-branches")
    public ResponseEntity<ApiResponse<BranchResponse>> createBranch(
            @PathVariable Long taskId,
            @Valid @RequestBody CreateBranchRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        BranchResponse response = gitBranchService.createBranch(taskId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * Get all branches for a task
     * GET /api/secure/tasks/{taskId}/git-branches
     */
    @GetMapping("/tasks/{taskId}/git-branches")
    public ResponseEntity<ApiResponse<List<BranchResponse>>> getBranchesByTask(@PathVariable Long taskId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        List<BranchResponse> branches = gitBranchService.getBranchesByTask(taskId, userId);
        return ResponseEntity.ok(ApiResponse.success(branches));
    }

    /**
     * Get active branches for a task
     * GET /api/secure/tasks/{taskId}/git-branches/active
     */
    @GetMapping("/tasks/{taskId}/git-branches/active")
    public ResponseEntity<ApiResponse<List<BranchResponse>>> getActiveBranchesByTask(@PathVariable Long taskId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        List<BranchResponse> branches = gitBranchService.getActiveBranchesByTask(taskId, userId);
        return ResponseEntity.ok(ApiResponse.success(branches));
    }

    /**
     * Get all branches for a project
     * GET /api/secure/projects/{projectId}/git-branches
     */
    @GetMapping("/projects/{projectId}/git-branches")
    public ResponseEntity<ApiResponse<List<BranchResponse>>> getBranchesByProject(@PathVariable Long projectId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        List<BranchResponse> branches = gitBranchService.getBranchesByProject(projectId, userId);
        return ResponseEntity.ok(ApiResponse.success(branches));
    }

    /**
     * Get all branches for a Git integration
     * GET /api/secure/git-integrations/{integrationId}/branches
     */
    @GetMapping("/git-integrations/{integrationId}/branches")
    public ResponseEntity<ApiResponse<List<BranchResponse>>> getBranchesByIntegration(@PathVariable Long integrationId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        List<BranchResponse> branches = gitBranchService.getBranchesByIntegration(integrationId, userId);
        return ResponseEntity.ok(ApiResponse.success(branches));
    }

    /**
     * Get a specific branch by ID
     * GET /api/secure/git-branches/{branchId}
     */
    @GetMapping("/git-branches/{branchId}")
    public ResponseEntity<ApiResponse<BranchResponse>> getBranch(@PathVariable Long branchId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        BranchResponse branch = gitBranchService.getBranch(branchId, userId);
        return ResponseEntity.ok(ApiResponse.success(branch));
    }

    /**
     * Delete a branch
     * DELETE /api/secure/git-branches/{branchId}
     */
    @DeleteMapping("/git-branches/{branchId}")
    public ResponseEntity<ApiResponse<Void>> deleteBranch(@PathVariable Long branchId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        gitBranchService.deleteBranch(branchId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
