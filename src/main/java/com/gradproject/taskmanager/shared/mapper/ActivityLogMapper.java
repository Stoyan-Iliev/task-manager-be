package com.gradproject.taskmanager.shared.mapper;

import com.gradproject.taskmanager.modules.activity.domain.ActivityLog;
import com.gradproject.taskmanager.modules.activity.dto.ActivityLogResponse;
import com.gradproject.taskmanager.shared.dto.UserSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;


@Mapper(componentModel = "spring")
public interface ActivityLogMapper {

    
    @Mapping(source = "user", target = "user")
    ActivityLogResponse toResponse(ActivityLog activityLog);

    
    List<ActivityLogResponse> toResponseList(List<ActivityLog> activityLogs);

    
    @Mapping(source = "id", target = "id")
    @Mapping(source = "username", target = "username")
    @Mapping(source = "email", target = "email")
    @Mapping(source = "firstName", target = "firstName")
    @Mapping(source = "lastName", target = "lastName")
    @Mapping(source = "avatarUrl", target = "avatarUrl")
    UserSummary toUserSummary(com.gradproject.taskmanager.modules.auth.domain.User user);
}
