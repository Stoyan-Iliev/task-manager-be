package com.gradproject.taskmanager.shared.mapper;

import com.gradproject.taskmanager.modules.git.domain.GitBranch;
import com.gradproject.taskmanager.modules.git.dto.response.BranchResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BranchMapper {

    @Mapping(target = "gitIntegrationId", source = "gitIntegration.id")
    @Mapping(target = "taskId", source = "task.id")
    @Mapping(target = "taskKey", source = "task.key")
    @Mapping(target = "createdByUsername", source = "createdBy.username")
    @Mapping(target = "pullRequestNumber", ignore = true)
    @Mapping(target = "pullRequestStatus", ignore = true)
    BranchResponse toResponse(GitBranch branch);
}
