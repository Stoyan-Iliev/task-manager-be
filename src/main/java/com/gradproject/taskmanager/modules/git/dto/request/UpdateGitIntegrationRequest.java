package com.gradproject.taskmanager.modules.git.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


public record UpdateGitIntegrationRequest(
    
    Boolean autoLinkEnabled,
    Boolean smartCommitsEnabled,
    Boolean autoCloseOnMerge,

    @Size(max = 50, message = "Branch prefix cannot exceed 50 characters")
    @Pattern(regexp = "^[a-z-]+/$", message = "Branch prefix must be lowercase with trailing slash (e.g., 'feature/')")
    String branchPrefix,

    Boolean isActive,

    
    String accessToken,
    String webhookSecret
) {}
