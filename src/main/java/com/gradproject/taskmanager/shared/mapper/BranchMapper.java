package com.gradproject.taskmanager.shared.mapper;

import com.gradproject.taskmanager.modules.git.domain.GitBranch;
import com.gradproject.taskmanager.modules.git.domain.GitIntegration;
import com.gradproject.taskmanager.modules.git.dto.response.BranchResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BranchMapper {

    @Mapping(target = "gitIntegrationId", source = "gitIntegration.id")
    @Mapping(target = "taskId", source = "task.id")
    @Mapping(target = "taskKey", source = "task.key")
    @Mapping(target = "createdByUsername", expression = "java(getCreatorUsername(branch))")
    @Mapping(target = "branchUrl", expression = "java(buildBranchUrl(branch))")
    BranchResponse toResponse(GitBranch branch);

    /**
     * Get the username of who created the branch.
     * Prefers system user (createdBy) for UI-created branches,
     * falls back to Git provider username (creatorUsername) for webhook-created branches.
     */
    default String getCreatorUsername(GitBranch branch) {
        if (branch == null) {
            return null;
        }
        // UI-created branches have createdBy set
        if (branch.getCreatedBy() != null) {
            return branch.getCreatedBy().getUsername();
        }
        // Webhook-created branches use creatorUsername from Git provider
        return branch.getCreatorUsername();
    }

    /**
     * Build the URL to view the branch on the Git provider (GitHub/GitLab/Bitbucket).
     */
    default String buildBranchUrl(GitBranch branch) {
        if (branch == null) {
            return null;
        }

        GitIntegration integration = branch.getGitIntegration();
        if (integration == null || integration.getRepositoryUrl() == null) {
            return null;
        }

        String baseUrl = integration.getRepositoryUrl();
        String branchName = branch.getBranchName();

        if (branchName == null || branchName.isEmpty()) {
            return null;
        }

        return switch (integration.getProvider()) {
            case GITHUB -> baseUrl + "/tree/" + branchName;
            case GITLAB -> baseUrl + "/-/tree/" + branchName;
            case BITBUCKET -> baseUrl + "/branch/" + branchName;
        };
    }
}
