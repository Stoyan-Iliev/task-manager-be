package com.gradproject.taskmanager.modules.git.controller;

import com.gradproject.taskmanager.modules.git.dto.response.PullRequestResponse;
import com.gradproject.taskmanager.modules.git.service.GitPullRequestService;
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
public class GitPullRequestController {

    private final GitPullRequestService gitPullRequestService;

    
    @GetMapping("/tasks/{taskId}/pull-requests")
    public ResponseEntity<ApiResponse<List<PullRequestResponse>>> getPullRequestsByTask(@PathVariable Long taskId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        List<PullRequestResponse> pullRequests = gitPullRequestService.getPullRequestsByTask(taskId, userId);
        return ResponseEntity.ok(ApiResponse.success(pullRequests));
    }

    
    @GetMapping("/projects/{projectId}/pull-requests")
    public ResponseEntity<ApiResponse<Page<PullRequestResponse>>> getPullRequestsByProject(
            @PathVariable Long projectId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Integer userId = SecurityUtils.getCurrentUserId();
        Page<PullRequestResponse> pullRequests = gitPullRequestService.getPullRequestsByProject(projectId, userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(pullRequests));
    }

    
    @GetMapping("/pull-requests/{prId}")
    public ResponseEntity<ApiResponse<PullRequestResponse>> getPullRequest(@PathVariable Long prId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        PullRequestResponse pullRequest = gitPullRequestService.getPullRequest(prId, userId);
        return ResponseEntity.ok(ApiResponse.success(pullRequest));
    }

    
    @GetMapping("/git-integrations/{integrationId}/pull-requests/{prNumber}")
    public ResponseEntity<ApiResponse<PullRequestResponse>> getPullRequestByNumber(
            @PathVariable Long integrationId,
            @PathVariable Integer prNumber) {
        Integer userId = SecurityUtils.getCurrentUserId();
        PullRequestResponse pullRequest = gitPullRequestService.getPullRequestByNumber(integrationId, prNumber, userId);
        return ResponseEntity.ok(ApiResponse.success(pullRequest));
    }

    
    @PostMapping("/pull-requests/{prId}/link-task/{taskId}")
    public ResponseEntity<ApiResponse<PullRequestResponse>> linkPullRequestToTask(
            @PathVariable Long prId,
            @PathVariable Long taskId,
            @RequestParam(required = false, defaultValue = "false") Boolean closesTask) {
        Integer userId = SecurityUtils.getCurrentUserId();
        PullRequestResponse pullRequest = gitPullRequestService.linkPullRequestToTask(prId, taskId, closesTask, userId);
        return ResponseEntity.ok(ApiResponse.success(pullRequest));
    }

    
    @DeleteMapping("/pull-requests/{prId}/unlink-task/{taskId}")
    public ResponseEntity<ApiResponse<PullRequestResponse>> unlinkPullRequestFromTask(
            @PathVariable Long prId,
            @PathVariable Long taskId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        PullRequestResponse pullRequest = gitPullRequestService.unlinkPullRequestFromTask(prId, taskId, userId);
        return ResponseEntity.ok(ApiResponse.success(pullRequest));
    }
}
