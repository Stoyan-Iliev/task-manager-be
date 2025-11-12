package com.gradproject.taskmanager.modules.organization.dto;

import com.gradproject.taskmanager.modules.organization.domain.OrganizationRole;

import java.time.LocalDateTime;


public record MemberResponse(
    Long id,
    Integer userId,
    String username,
    String email,
    OrganizationRole role,
    LocalDateTime joinedAt,
    String invitedByUsername
) {}
