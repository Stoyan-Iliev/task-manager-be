package com.gradproject.taskmanager.modules.notification.controller;

import com.gradproject.taskmanager.modules.notification.dto.EmailPreferencesResponse;
import com.gradproject.taskmanager.modules.notification.dto.EmailPreferencesUpdateRequest;
import com.gradproject.taskmanager.modules.notification.dto.NotificationResponse;
import com.gradproject.taskmanager.modules.notification.service.EmailPreferenceService;
import com.gradproject.taskmanager.modules.notification.service.NotificationService;
import com.gradproject.taskmanager.shared.dto.ApiResponse;
import com.gradproject.taskmanager.shared.dto.PageResponse;
import com.gradproject.taskmanager.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;



@RestController
@RequestMapping("/api/secure/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;
    private final EmailPreferenceService emailPreferenceService;

    
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();

        PageResponse<NotificationResponse> notifications = notificationService.getUserNotifications(
                currentUserId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    
    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getUnreadNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();

        PageResponse<NotificationResponse> notifications = notificationService.getUnreadNotifications(
                currentUserId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    
    @GetMapping("/unread/count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount() {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        long count = notificationService.getUnreadCount(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("count", count)));
    }

    
    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        notificationService.markAsRead(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    
    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        notificationService.markAllAsRead(currentUserId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // === Email Preferences Endpoints ===

    /**
     * Get current user's email notification preferences.
     *
     * @return the user's email preferences
     */
    @GetMapping("/email-preferences")
    public ResponseEntity<ApiResponse<EmailPreferencesResponse>> getEmailPreferences() {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        EmailPreferencesResponse preferences = emailPreferenceService.getPreferences(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(preferences));
    }

    /**
     * Update current user's email notification preferences.
     * Only provided fields will be updated.
     *
     * @param request the preference updates
     * @return the updated email preferences
     */
    @PutMapping("/email-preferences")
    public ResponseEntity<ApiResponse<EmailPreferencesResponse>> updateEmailPreferences(
            @RequestBody EmailPreferencesUpdateRequest request) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        EmailPreferencesResponse preferences = emailPreferenceService.updatePreferences(currentUserId, request);
        log.info("Updated email preferences for user ID: {}", currentUserId);
        return ResponseEntity.ok(ApiResponse.success(preferences));
    }
}
