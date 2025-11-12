package com.gradproject.taskmanager.modules.git.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


public record CreateBranchRequest(
    @NotNull(message = "Git integration ID is required")
    Long gitIntegrationId,

    @Size(max = 255, message = "Branch name cannot exceed 255 characters")
    @Pattern(regexp = "^[a-zA-Z0-9/_-]+$", message = "Branch name can only contain alphanumeric characters, slashes, hyphens, and underscores")
    String branchName,  

    @NotBlank(message = "Base branch is required")
    @Size(max = 255, message = "Base branch cannot exceed 255 characters")
    String baseBranch,  

    @Size(max = 50, message = "Branch type cannot exceed 50 characters")
    @Pattern(regexp = "^(feature|bugfix|hotfix|release)$", message = "Branch type must be one of: feature, bugfix, hotfix, release")
    String branchType   
) {
    public CreateBranchRequest {
        if (branchType == null || branchType.isEmpty()) {
            branchType = "feature";
        }
    }
}
