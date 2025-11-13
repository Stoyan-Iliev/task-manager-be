package com.gradproject.taskmanager.modules.auth.service;

import com.gradproject.taskmanager.infrastructure.storage.S3Service;
import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.dto.ChangePasswordRequest;
import com.gradproject.taskmanager.modules.auth.dto.UpdateUserProfileRequest;
import com.gradproject.taskmanager.modules.auth.dto.UserProfileResponse;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import com.gradproject.taskmanager.shared.exception.BadRequestException;
import com.gradproject.taskmanager.shared.exception.ResourceNotFoundException;
import com.gradproject.taskmanager.shared.mapper.UserMapper;
import com.gradproject.taskmanager.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final S3Service s3Service;
    private final PasswordEncoder passwordEncoder;

    private static final List<String> ALLOWED_AVATAR_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );
    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile() {
        Integer userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        log.debug("Retrieved profile for user: {}", user.getUsername());
        return userMapper.toProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(UpdateUserProfileRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        userMapper.updateEntityFromRequest(request, user);
        user.setUpdatedAt(LocalDateTime.now());

        user = userRepository.save(user);

        log.info("User {} updated their profile", user.getUsername());
        return userMapper.toProfileResponse(user);
    }

    @Override
    @Transactional
    public String uploadAvatar(MultipartFile file) {
        Integer userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        validateAvatarFile(file);

        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            try {
                s3Service.deleteFile(user.getAvatarUrl());
                log.debug("Deleted old avatar for user {}", userId);
            } catch (Exception e) {
                log.warn("Failed to delete old avatar for user {}: {}", userId, e.getMessage());
            }
        }

        String uuid = UUID.randomUUID().toString();
        String extension = getFileExtension(file.getOriginalFilename());
        String storagePath = String.format("user-avatars/%d/%s%s", userId, uuid, extension);

        s3Service.uploadFile(file, storagePath);

        user.setAvatarUrl(storagePath);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("User {} uploaded new avatar: {}", user.getUsername(), storagePath);
        return storagePath;
    }

    @Override
    @Transactional
    public void deleteAvatar() {
        Integer userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (user.getAvatarUrl() == null || user.getAvatarUrl().isEmpty()) {
            throw new BadRequestException("User does not have an avatar");
        }

        try {
            s3Service.deleteFile(user.getAvatarUrl());
        } catch (Exception e) {
            log.warn("Failed to delete avatar file for user {}: {}", userId, e.getMessage());
        }

        user.setAvatarUrl(null);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("User {} deleted their avatar", user.getUsername());
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BadRequestException("New password and confirmation do not match");
        }

        if (request.currentPassword().equals(request.newPassword())) {
            throw new BadRequestException("New password must be different from current password");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("User {} changed their password", user.getUsername());
    }

    private void validateAvatarFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new BadRequestException("Avatar size exceeds limit of 5 MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_AVATAR_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Avatar must be an image (JPEG, PNG, GIF, or WebP)");
        }
    }

    @Override
    public byte[] getAvatarByUserId(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (user.getAvatarUrl() == null || user.getAvatarUrl().isEmpty()) {
            throw new ResourceNotFoundException("Avatar not found for user", userId);
        }

        return s3Service.downloadFile(user.getAvatarUrl());
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}
