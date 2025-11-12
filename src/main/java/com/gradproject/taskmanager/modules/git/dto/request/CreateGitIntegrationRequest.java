package com.gradproject.taskmanager.modules.git.dto.request;

import com.gradproject.taskmanager.modules.git.domain.enums.GitProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


public record CreateGitIntegrationRequest(
    @NotNull(message = "Project ID is required")
    Long projectId,

    @NotNull(message = "Git provider is required")
    GitProvider provider,

    @NotBlank(message = "Repository URL is required")
    @Size(max = 500, message = "Repository URL cannot exceed 500 characters")
    @Pattern(regexp = "^https://.*", message = "Repository URL must start with https://")
    String repositoryUrl,

    @NotBlank(message = "Access token is required")
    String accessToken,

    String webhookSecret,

    
    Boolean autoLinkEnabled,
    Boolean smartCommitsEnabled,
    Boolean autoCloseOnMerge,

    @Size(max = 50, message = "Branch prefix cannot exceed 50 characters")
    @Pattern(regexp = "^[a-z-]+/$", message = "Branch prefix must be lowercase with trailing slash (e.g., 'feature/')")
    String branchPrefix
) {
    
    public CreateGitIntegrationRequest {
        if (autoLinkEnabled == null) autoLinkEnabled = true;
        if (smartCommitsEnabled == null) smartCommitsEnabled = true;
        if (autoCloseOnMerge == null) autoCloseOnMerge = true;
    }
}
