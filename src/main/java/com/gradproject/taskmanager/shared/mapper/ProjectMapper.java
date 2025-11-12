package com.gradproject.taskmanager.shared.mapper;

import com.gradproject.taskmanager.modules.project.domain.*;
import com.gradproject.taskmanager.modules.project.dto.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;


@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ProjectMapper {

    
    @Mapping(target = "organizationId", source = "organization.id")
    @Mapping(target = "createdByUsername", source = "createdBy.username")
    @Mapping(target = "memberCount", expression = "java(project.getMembers() != null ? (long) project.getMembers().size() : 0L)")
    @Mapping(target = "statusCount", expression = "java(project.getStatuses() != null ? (long) project.getStatuses().size() : 0L)")
    @Mapping(target = "defaultStatus", source = "defaultStatus")
    ProjectResponse toResponse(Project project);

    
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "color", source = "color")
    @Mapping(target = "category", source = "category")
    TaskStatusSummary toStatusSummary(TaskStatus status);

    
    @Mapping(target = "projectId", source = "project.id")
    TaskStatusResponse toStatusResponse(TaskStatus status);

    
    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "createdByUsername", source = "createdBy.username")
    @Mapping(target = "completedByUsername", source = "completedBy.username")
    @Mapping(target = "metrics", ignore = true)
    SprintResponse toSprintResponse(Sprint sprint);

    
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "addedByUsername", source = "addedBy.username")
    ProjectMemberResponse toMemberResponse(ProjectMember member);

    
    StatusTemplateResponse toTemplateResponse(StatusTemplate template);
}
