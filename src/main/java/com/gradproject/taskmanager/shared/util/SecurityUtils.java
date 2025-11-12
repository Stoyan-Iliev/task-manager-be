package com.gradproject.taskmanager.shared.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;


public final class SecurityUtils {

    private SecurityUtils() {
        
    }

    
    public static Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            String userIdStr = jwt.getSubject();
            try {
                return Integer.parseInt(userIdStr);
            } catch (NumberFormatException e) {
                throw new IllegalStateException("Invalid user ID in token: " + userIdStr);
            }
        }

        throw new IllegalStateException("Unable to extract user ID from authentication");
    }

    
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }

        
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            String username = jwt.getClaim("username");
            if (username != null) {
                return username;
            }
        }

        
        return authentication.getName();
    }
}
