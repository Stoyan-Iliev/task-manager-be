package com.gradproject.taskmanager.modules.project.dto;

import com.gradproject.taskmanager.modules.project.domain.ProjectRole;

import java.time.LocalDateTime;


public record ProjectMemberResponse(
    Long id,
    Integer userId,
    String username,
    String email,
    String firstName,
    String lastName,
    String avatarUrl,
    ProjectRole role,
    LocalDateTime addedAt,
    String addedByUsername
) {}
