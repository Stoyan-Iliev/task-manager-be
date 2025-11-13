package com.gradproject.taskmanager.shared.mapper;

import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.dto.UpdateUserProfileRequest;
import com.gradproject.taskmanager.modules.auth.dto.UserProfileResponse;
import com.gradproject.taskmanager.modules.auth.dto.UserResponse;
import org.mapstruct.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserMapper {

    DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Mapping(target = "tsCreated", expression = "java(formatDateTime(user.getCreatedAt()))")
    @Mapping(target = "tsUpdated", expression = "java(formatDateTime(user.getUpdatedAt()))")
    UserProfileResponse toProfileResponse(User user);

    @Mapping(target = "tsCreated", expression = "java(formatDateTime(user.getCreatedAt()))")
    @Mapping(target = "tsUpdated", expression = "java(formatDateTime(user.getUpdatedAt()))")
    UserResponse toUserResponse(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "locked", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    void updateEntityFromRequest(UpdateUserProfileRequest request, @MappingTarget User user);

    default String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(FORMATTER) : null;
    }
}
