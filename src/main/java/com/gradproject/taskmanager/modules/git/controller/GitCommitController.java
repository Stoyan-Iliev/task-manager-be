package com.gradproject.taskmanager.modules.git.controller;

import com.gradproject.taskmanager.modules.git.dto.response.CommitResponse;
import com.gradproject.taskmanager.modules.git.service.GitCommitService;
import com.gradproject.taskmanager.shared.dto.ApiResponse;
import com.gradproject.taskmanager.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/secure")
@RequiredArgsConstructor
public class GitCommitController {

    private final GitCommitService gitCommitService;

    
    @GetMapping("/tasks/{taskId}/git-commits")
    public ResponseEntity<ApiResponse<List<CommitResponse>>> getCommitsByTask(@PathVariable Long taskId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        List<CommitResponse> commits = gitCommitService.getCommitsByTask(taskId, userId);
        return ResponseEntity.ok(ApiResponse.success(commits));
    }

    
    @GetMapping("/tasks/{taskId}/git-commits/paginated")
    public ResponseEntity<ApiResponse<Page<CommitResponse>>> getCommitsByTaskPaginated(
            @PathVariable Long taskId,
            @PageableDefault(size = 20, sort = "authorDate") Pageable pageable) {
        Integer userId = SecurityUtils.getCurrentUserId();
        Page<CommitResponse> commits = gitCommitService.getCommitsByTask(taskId, userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(commits));
    }

    
    @GetMapping("/projects/{projectId}/git-commits")
    public ResponseEntity<ApiResponse<Page<CommitResponse>>> getCommitsByProject(
            @PathVariable Long projectId,
            @PageableDefault(size = 20, sort = "authorDate") Pageable pageable) {
        Integer userId = SecurityUtils.getCurrentUserId();
        Page<CommitResponse> commits = gitCommitService.getCommitsByProject(projectId, userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(commits));
    }

    
    @GetMapping("/git-commits/{commitId}")
    public ResponseEntity<ApiResponse<CommitResponse>> getCommit(@PathVariable Long commitId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        CommitResponse commit = gitCommitService.getCommit(commitId, userId);
        return ResponseEntity.ok(ApiResponse.success(commit));
    }

    
    @GetMapping("/git-integrations/{integrationId}/commits/{commitSha}")
    public ResponseEntity<ApiResponse<CommitResponse>> getCommitBySha(
            @PathVariable Long integrationId,
            @PathVariable String commitSha) {
        Integer userId = SecurityUtils.getCurrentUserId();
        CommitResponse commit = gitCommitService.getCommitBySha(integrationId, commitSha, userId);
        return ResponseEntity.ok(ApiResponse.success(commit));
    }

    
    @PostMapping("/git-commits/{commitId}/link-task/{taskId}")
    public ResponseEntity<ApiResponse<CommitResponse>> linkCommitToTask(
            @PathVariable Long commitId,
            @PathVariable Long taskId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        CommitResponse commit = gitCommitService.linkCommitToTask(commitId, taskId, userId);
        return ResponseEntity.ok(ApiResponse.success(commit));
    }

    
    @DeleteMapping("/git-commits/{commitId}/unlink-task/{taskId}")
    public ResponseEntity<ApiResponse<CommitResponse>> unlinkCommitFromTask(
            @PathVariable Long commitId,
            @PathVariable Long taskId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        CommitResponse commit = gitCommitService.unlinkCommitFromTask(commitId, taskId, userId);
        return ResponseEntity.ok(ApiResponse.success(commit));
    }
}
