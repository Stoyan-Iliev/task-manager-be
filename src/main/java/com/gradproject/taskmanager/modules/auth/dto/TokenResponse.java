package com.gradproject.taskmanager.modules.auth.dto;

import java.util.List;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        String scope,
        UserInfo user
) {
    public record UserInfo(
        Integer id,
        String username,
        String firstName,
        String lastName,
        String avatarUrl,
        List<String> roles
    ) {}
}
