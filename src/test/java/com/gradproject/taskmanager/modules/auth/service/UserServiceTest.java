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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private S3Service s3Service;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "test@example.com", "hashedPassword");
        testUser.setFirstName("Test");
        testUser.setLastName("User");

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("1")
                .claim("username", "testuser")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void getCurrentUserProfile_shouldReturnProfile_whenUserExists() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        UserProfileResponse expectedResponse = new UserProfileResponse(
                1, "testuser", "test@example.com", "Test", "User",
                null, null, null, null, "UTC", "en", "MM/DD/YYYY", "12h", null,
                true, false, null, null
        );
        when(userMapper.toProfileResponse(testUser)).thenReturn(expectedResponse);

        UserProfileResponse result = userService.getCurrentUserProfile();

        assertNotNull(result);
        assertEquals("testuser", result.username());
        verify(userRepository).findById(1);
    }

    @Test
    void getCurrentUserProfile_shouldThrowException_whenUserNotFound() {
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getCurrentUserProfile());
    }

    @Test
    void updateProfile_shouldUpdateAndReturnProfile() {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "UpdatedFirst", "UpdatedLast", "Engineer", "IT",
                "+123456789", "America/New_York", "en", "DD/MM/YYYY", "24h", "Bio text"
        );

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.updateProfile(request);

        verify(userMapper).updateEntityFromRequest(request, testUser);
        verify(userRepository).save(testUser);
    }

    @Test
    void changePassword_shouldSucceed_whenCurrentPasswordMatches() {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "oldPassword", "NewPassword1", "NewPassword1"
        );

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", "hashedPassword")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword1")).thenReturn("newHashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.changePassword(request);

        verify(passwordEncoder).matches("oldPassword", "hashedPassword");
        verify(passwordEncoder).encode("NewPassword1");
        verify(userRepository).save(testUser);
    }

    @Test
    void changePassword_shouldFail_whenCurrentPasswordIncorrect() {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "wrongPassword", "NewPassword1", "NewPassword1"
        );

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", "hashedPassword")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> userService.changePassword(request));
    }

    @Test
    void changePassword_shouldFail_whenPasswordsDoNotMatch() {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "oldPassword", "NewPassword1", "DifferentPassword1"
        );

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", "hashedPassword")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> userService.changePassword(request));
    }

    @Test
    void deleteAvatar_shouldFail_whenNoAvatarExists() {
        testUser.setAvatarUrl(null);
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        assertThrows(BadRequestException.class, () -> userService.deleteAvatar());
    }
}
