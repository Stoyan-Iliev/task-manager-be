package com.gradproject.taskmanager.modules.git.controller;

import com.gradproject.taskmanager.modules.git.dto.request.CreateGitIntegrationRequest;
import com.gradproject.taskmanager.modules.git.dto.request.UpdateGitIntegrationRequest;
import com.gradproject.taskmanager.modules.git.dto.response.GitIntegrationResponse;
import com.gradproject.taskmanager.modules.git.service.GitIntegrationService;
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
public class GitIntegrationController {

    private final GitIntegrationService gitIntegrationService;

    
    @PostMapping("/organizations/{organizationId}/git-integrations")
    public ResponseEntity<ApiResponse<GitIntegrationResponse>> createIntegration(
            @PathVariable Long organizationId,
            @Valid @RequestBody CreateGitIntegrationRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        GitIntegrationResponse response = gitIntegrationService.createIntegration(organizationId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    
    @GetMapping("/organizations/{organizationId}/git-integrations")
    public ResponseEntity<ApiResponse<List<GitIntegrationResponse>>> listOrganizationIntegrations(
            @PathVariable Long organizationId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        List<GitIntegrationResponse> integrations = gitIntegrationService.listOrganizationIntegrations(organizationId, userId);
        return ResponseEntity.ok(ApiResponse.success(integrations));
    }

    
    @GetMapping("/projects/{projectId}/git-integrations")
    public ResponseEntity<ApiResponse<List<GitIntegrationResponse>>> listProjectIntegrations(
            @PathVariable Long projectId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        List<GitIntegrationResponse> integrations = gitIntegrationService.listProjectIntegrations(projectId, userId);
        return ResponseEntity.ok(ApiResponse.success(integrations));
    }

    
    @GetMapping("/git-integrations/{id}")
    public ResponseEntity<ApiResponse<GitIntegrationResponse>> getIntegration(@PathVariable Long id) {
        Integer userId = SecurityUtils.getCurrentUserId();
        GitIntegrationResponse response = gitIntegrationService.getIntegration(id, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    
    @PutMapping("/git-integrations/{id}")
    public ResponseEntity<ApiResponse<GitIntegrationResponse>> updateIntegration(
            @PathVariable Long id,
            @Valid @RequestBody UpdateGitIntegrationRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        GitIntegrationResponse response = gitIntegrationService.updateIntegration(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    
    @DeleteMapping("/git-integrations/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteIntegration(@PathVariable Long id) {
        Integer userId = SecurityUtils.getCurrentUserId();
        gitIntegrationService.deleteIntegration(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    
    @PostMapping("/organizations/{organizationId}/git-integrations/test-connection")
    public ResponseEntity<ApiResponse<Boolean>> testConnection(
            @PathVariable Long organizationId,
            @Valid @RequestBody CreateGitIntegrationRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        boolean success = gitIntegrationService.testConnection(organizationId, request, userId);
        return ResponseEntity.ok(ApiResponse.success(success));
    }

    
    @PostMapping("/git-integrations/{id}/sync")
    public ResponseEntity<ApiResponse<GitIntegrationResponse>> syncIntegration(@PathVariable Long id) {
        Integer userId = SecurityUtils.getCurrentUserId();
        GitIntegrationResponse response = gitIntegrationService.syncIntegration(id, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
