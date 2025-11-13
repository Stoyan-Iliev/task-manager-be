package com.gradproject.taskmanager.modules.auth.service;

import com.gradproject.taskmanager.infrastructure.security.jwt.JwtTokenService;
import com.gradproject.taskmanager.modules.auth.domain.RefreshToken;
import com.gradproject.taskmanager.modules.auth.domain.Role;
import com.gradproject.taskmanager.modules.auth.domain.User;
import com.gradproject.taskmanager.modules.auth.dto.LoginRequest;
import com.gradproject.taskmanager.modules.auth.dto.SignUpRequest;
import com.gradproject.taskmanager.modules.auth.dto.TokenResponse;
import com.gradproject.taskmanager.modules.auth.dto.UserResponse;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenService jwtTokenService,
                       RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
    }

    public UserResponse signUp(SignUpRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            throw new IllegalStateException("Username already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException("Email already registered");
        }

        String hashed = passwordEncoder.encode(request.password());
        User user = new User(username, email, hashed);
        if (request.firstName() != null && !request.firstName().isBlank()) {
            user.setFirstName(request.firstName().trim());
        }
        if (request.lastName() != null && !request.lastName().isBlank()) {
            user.setLastName(request.lastName().trim());
        }
        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    public TokenResponse login(LoginRequest request, HttpServletRequest httpRequest) throws BadCredentialsException {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        
        User user = userRepository.findByUsername(request.username()).orElseThrow();
        String access = jwtTokenService.generateAccessToken(user);
        String refresh = refreshTokenService.issue(user, httpRequest.getHeader("User-Agent"), httpRequest.getRemoteAddr());
        return toTokenResponse(user, access, refresh);
    }

    public TokenResponse refresh(String refreshToken, HttpServletRequest request) {
        
        User user = refreshTokenService.findByRawToken(refreshToken)
                .map(RefreshToken::getUser)
                .orElseThrow(() -> new IllegalArgumentException("invalid_token"));
        String rotated = refreshTokenService.rotate(refreshToken, request.getHeader("User-Agent"), request.getRemoteAddr());
        String access = jwtTokenService.generateAccessToken(user);
        return toTokenResponse(user, access, rotated);
    }

    public void revoke(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getAvatarUrl(),
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : null,
                user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null
        );
    }

    private TokenResponse toTokenResponse(User user, String access, String refresh) {
        List<String> roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toList());
        TokenResponse.UserInfo info = new TokenResponse.UserInfo(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getAvatarUrl(),
                roles
        );
        String scope = String.join(" ", roles);
        return new TokenResponse(access, refresh, "bearer", jwtTokenService.getAccessTokenExpiresInSeconds(), scope, info);
    }
}
