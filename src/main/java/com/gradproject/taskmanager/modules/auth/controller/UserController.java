package com.gradproject.taskmanager.modules.auth.controller;

import com.gradproject.taskmanager.modules.auth.dto.ChangePasswordRequest;
import com.gradproject.taskmanager.modules.auth.dto.UpdateUserProfileRequest;
import com.gradproject.taskmanager.modules.auth.dto.UserProfileResponse;
import com.gradproject.taskmanager.modules.auth.service.UserService;
import com.gradproject.taskmanager.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/secure/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUserProfile() {
        UserProfileResponse profile = userService.getCurrentUserProfile();
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateUserProfileRequest request) {
        UserProfileResponse updatedProfile = userService.updateProfile(request);
        return ResponseEntity.ok(ApiResponse.success(updatedProfile));
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadAvatar(
            @RequestParam("file") MultipartFile file) {
        String avatarUrl = userService.uploadAvatar(file);
        return ResponseEntity.ok(ApiResponse.success(Map.of("avatarUrl", avatarUrl)));
    }

    @DeleteMapping("/me/avatar")
    public ResponseEntity<ApiResponse<Void>> deleteAvatar() {
        userService.deleteAvatar();
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/me/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{userId}/avatar")
    public ResponseEntity<byte[]> getUserAvatar(@PathVariable Integer userId) {
        byte[] avatarData = userService.getAvatarByUserId(userId);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                .body(avatarData);
    }
}
