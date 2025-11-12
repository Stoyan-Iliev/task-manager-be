package com.gradproject.taskmanager.modules.auth.controller;

import com.gradproject.taskmanager.AbstractIntegrationTest;
import com.gradproject.taskmanager.infrastructure.security.RateLimiterService;
import com.gradproject.taskmanager.modules.auth.service.AuthService;
import com.gradproject.taskmanager.modules.auth.service.RefreshTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@AutoConfigureMockMvc
class AuthControllerRateLimitTests extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    RateLimiterService rateLimiterService;

    @MockitoBean
    AuthService authService;

    @MockitoBean
    RefreshTokenService refreshTokenService;

    @Test
    void login_rateLimited_returns429WithRetryAfter() throws Exception {
        given(rateLimiterService.allowLogin(anyString(), anyString())).willReturn(false);

        String body = "{\n  \"username\": \"bob\",\n  \"password\": \"Password1\"\n}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", notNullValue()))
                .andExpect(jsonPath("$.error").value("rate_limited"));
    }
}
