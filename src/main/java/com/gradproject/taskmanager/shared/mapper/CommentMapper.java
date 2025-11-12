package com.gradproject.taskmanager.shared.mapper;

import com.gradproject.taskmanager.modules.task.domain.Comment;
import com.gradproject.taskmanager.modules.task.dto.CommentRequest;
import com.gradproject.taskmanager.modules.task.dto.CommentResponse;
import com.gradproject.taskmanager.shared.dto.UserSummary;
import org.mapstruct.*;

import java.util.Collections;


@Mapper(componentModel = "spring", imports = Collections.class)
public interface CommentMapper {

    
    @Mapping(source = "task.id", target = "taskId")
    @Mapping(source = "user", target = "user")
    @Mapping(source = "parentComment.id", target = "parentCommentId")
    @Mapping(target = "replies", expression = "java(Collections.emptyList())")
    CommentResponse toResponse(Comment comment);

    
    @Mapping(source = "id", target = "id")
    @Mapping(source = "username", target = "username")
    @Mapping(source = "email", target = "email")
    UserSummary toUserSummary(com.gradproject.taskmanager.modules.auth.domain.User user);

    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "task", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "parentComment", ignore = true)
    @Mapping(target = "edited", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Comment fromRequest(CommentRequest request);

    
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "task", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "parentComment", ignore = true)
    @Mapping(target = "edited", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateFromRequest(CommentRequest request, @MappingTarget Comment comment);
}
