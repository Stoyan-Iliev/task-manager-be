package com.gradproject.taskmanager.modules.auth.controller;

import com.gradproject.taskmanager.infrastructure.security.RateLimiterService;
import com.gradproject.taskmanager.modules.auth.dto.*;
import com.gradproject.taskmanager.modules.auth.service.AuthService;
import com.gradproject.taskmanager.modules.auth.service.RefreshTokenService;
import com.gradproject.taskmanager.shared.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RateLimiterService rateLimiterService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthService authService, RateLimiterService rateLimiterService, RefreshTokenService refreshTokenService) {
        this.authService = authService;
        this.rateLimiterService = rateLimiterService;
        this.refreshTokenService = refreshTokenService;
    }

    
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserResponse>> signUp(@Valid @RequestBody SignUpRequest request) {
        UserResponse body = authService.signUp(request);
        return ResponseEntity.created(URI.create("/api/users/" + body.id()))
                .headers(noStoreHeaders())
                .body(ApiResponse.success(body));
    }

    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        String username = request.username();
        if (!rateLimiterService.allowLogin(ip, username)) {
            long retryAfter = 60 - (Instant.now().getEpochSecond() % 60);
            return ResponseEntity.status(429).headers(rateLimitHeaders(retryAfter)).body(ApiResponse.error("rate_limited"));
        }
        try {
            TokenResponse tokens = authService.login(request, httpRequest);
            return ResponseEntity.ok().headers(noStoreHeaders()).body(ApiResponse.success(tokens));
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .headers(authErrorHeaders())
                    .body(ApiResponse.error("invalid_credentials"));
        }
    }

    
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        String username = refreshTokenService.findByRawToken(request.refreshToken())
                .map(rt -> rt.getUser().getUsername())
                .orElse("unknown");
        if (!rateLimiterService.allowRefresh(ip, username)) {
            long retryAfter = 60 - (Instant.now().getEpochSecond() % 60);
            return ResponseEntity.status(429).headers(rateLimitHeaders(retryAfter)).body(ApiResponse.error("rate_limited"));
        }
        try {
            TokenResponse tokens = authService.refresh(request.refreshToken(), httpRequest);
            return ResponseEntity.ok().headers(noStoreHeaders()).body(ApiResponse.success(tokens));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).headers(authErrorHeaders()).body(ApiResponse.error("invalid_token"));
        } catch (IllegalStateException e) {
            String code = e.getMessage();
            if ("token_expired".equals(code)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).headers(authErrorHeaders()).body(ApiResponse.error("token_expired"));
            }
            if ("token_revoked".equals(code)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).headers(authErrorHeaders()).body(ApiResponse.error("token_revoked"));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(noStoreHeaders()).body(ApiResponse.error("invalid_token"));
        }
    }

    
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@Valid @RequestBody LogoutRequest request) {
        authService.revoke(request.refreshToken());
        return ResponseEntity.ok().headers(noStoreHeaders()).body(ApiResponse.success("Logged out"));
    }

    private static HttpHeaders noStoreHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "no-store");
        headers.add("Pragma", "no-cache");
        return headers;
    }

    private static HttpHeaders authErrorHeaders() {
        HttpHeaders headers = noStoreHeaders();
        headers.add("WWW-Authenticate", "Bearer");
        return headers;
    }

    private static HttpHeaders rateLimitHeaders(long retryAfterSeconds) {
        HttpHeaders headers = noStoreHeaders();
        headers.add("Retry-After", String.valueOf(retryAfterSeconds));
        return headers;
    }
}
