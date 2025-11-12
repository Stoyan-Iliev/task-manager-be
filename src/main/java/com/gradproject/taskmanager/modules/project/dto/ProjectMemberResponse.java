package com.gradproject.taskmanager.modules.project.dto;

import com.gradproject.taskmanager.modules.project.domain.ProjectRole;

import java.time.LocalDateTime;


public record ProjectMemberResponse(
    Long id,
    Integer userId,
    String username,
    String email,
    ProjectRole role,
    LocalDateTime addedAt,
    String addedByUsername
) {}
