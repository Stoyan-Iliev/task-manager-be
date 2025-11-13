package com.gradproject.taskmanager.modules.auth.service;

import com.gradproject.taskmanager.modules.auth.dto.ChangePasswordRequest;
import com.gradproject.taskmanager.modules.auth.dto.UpdateUserProfileRequest;
import com.gradproject.taskmanager.modules.auth.dto.UserProfileResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    UserProfileResponse getCurrentUserProfile();

    UserProfileResponse updateProfile(UpdateUserProfileRequest request);

    String uploadAvatar(MultipartFile file);

    void deleteAvatar();

    void changePassword(ChangePasswordRequest request);

    byte[] getAvatarByUserId(Integer userId);
}
