package com.gradproject.taskmanager.shared.mapper;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.project.domain.TaskStatus;
import com.gradproject.taskmanager.modules.task.domain.Task;
import com.gradproject.taskmanager.modules.task.dto.*;
import com.gradproject.taskmanager.shared.dto.UserSummary;
import org.mapstruct.*;


@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface TaskMapper {

    
    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "projectKey", source = "project.key")
    @Mapping(target = "projectName", source = "project.name")
    @Mapping(target = "sprintId", source = "sprint.id")
    @Mapping(target = "sprintName", source = "sprint.name")
    @Mapping(target = "parentTaskId", source = "parentTask.id")
    @Mapping(target = "parentTaskKey", source = "parentTask.key")
    @Mapping(target = "subtaskCount", expression = "java(0)")  
    @Mapping(target = "commentCount", expression = "java(0)")  
    @Mapping(target = "attachmentCount", expression = "java(0)")  
    @Mapping(target = "isOverdue", expression = "java(task.isOverdue())")
    TaskResponse toResponse(Task task);

    
    @Mapping(target = "isOverdue", expression = "java(task.isOverdue())")
    TaskSummary toSummary(Task task);

    
    @Mapping(target = "category", source = "category")
    TaskStatusSummary toStatusSummary(TaskStatus status);

    
    UserSummary toUserSummary(User user);

    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "key", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "reporter", ignore = true)
    @Mapping(target = "sprint", ignore = true)
    @Mapping(target = "parentTask", ignore = true)
    @Mapping(target = "loggedHours", ignore = true)
    @Mapping(target = "customFields", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Task fromCreateRequest(TaskCreateRequest request);

    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "key", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "reporter", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "sprint", ignore = true)
    @Mapping(target = "parentTask", ignore = true)
    @Mapping(target = "loggedHours", ignore = true)
    @Mapping(target = "customFields", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    void updateFromRequest(TaskUpdateRequest request, @MappingTarget Task task);
}
