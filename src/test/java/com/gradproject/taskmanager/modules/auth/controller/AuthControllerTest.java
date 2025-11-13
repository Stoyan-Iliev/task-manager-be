package com.gradproject.taskmanager.modules.auth.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gradproject.taskmanager.AbstractIntegrationTest;
import com.gradproject.taskmanager.modules.auth.domain.RefreshToken;
import com.gradproject.taskmanager.modules.auth.dto.LoginRequest;
import com.gradproject.taskmanager.modules.auth.dto.SignUpRequest;
import com.gradproject.taskmanager.modules.auth.repository.RefreshTokenRepository;
import com.gradproject.taskmanager.modules.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AuthControllerWebTests extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired TransactionTemplate transactionTemplate;

    private String toJson(Object o) throws Exception { return objectMapper.writeValueAsString(o); }

    private String signupUser(String username, String email, String password, String firstName, String lastName) throws Exception {
        SignUpRequest req = new SignUpRequest(username, email, password, firstName, lastName);
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isCreated());
        return username;
    }

    private JsonNode login(String username, String password) throws Exception {
        LoginRequest req = new LoginRequest(username, password);
        String s = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andReturn().getResponse().getContentAsString();
        
        return objectMapper.readTree(s).get("data");
    }

    @Test
    @Transactional
    void signup_success_and_login_returns_tokens() throws Exception {
        signupUser("webuser1", "webuser1@example.com", "StrongPass1", "web", "user");
        JsonNode body = login("webuser1", "StrongPass1");
        assertThat(body.get("accessToken").asText()).isNotBlank();
        assertThat(body.get("refreshToken").asText()).isNotBlank();
        assertThat(body.get("tokenType").asText()).isEqualTo("bearer");
        assertThat(body.get("expiresIn").asLong()).isGreaterThan(0);
        assertThat(body.path("user").path("username").asText()).isEqualTo("webuser1");
    }

    @Test
    @Transactional
    void signup_conflict_when_username_taken() throws Exception {
        signupUser("taken", "t1@example.com", "StrongPass1", "taken", "t1");
        SignUpRequest req = new SignUpRequest("taken", "t2@example.com", "StrongPass1", "taken", "t2");
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void signup_validation_error() throws Exception {
        SignUpRequest req = new SignUpRequest("a", "not-an-email", "weak", "a", "b");
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_unauthorized_on_bad_credentials() throws Exception {
        
        LoginRequest req = new LoginRequest("nouser", "bad");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_credentials"));
    }

    @Test
    @Transactional
    void refresh_rotate_and_reuse_fails() throws Exception {
        signupUser("rotator", "rotator@example.com", "StrongPass1", "rotator", "rotatorLast");
        JsonNode login = login("rotator", "StrongPass1");
        String refresh1 = login.get("refreshToken").asText();

        
        String payload = "{\"refreshToken\":\"" + refresh1 + "\"}";
        String s = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        
        JsonNode refreshed = objectMapper.readTree(s).get("data");
        String refresh2 = refreshed.get("refreshToken").asText();
        assertThat(refresh2).isNotBlank();

        
        String reuse = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh1 + "\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(reuse).get("error").asText()).isEqualTo("token_revoked");
    }

    @Test
    @Transactional
    void refresh_expired_token_returns_401() throws Exception {
        signupUser("expiree", "expiree@example.com", "StrongPass1", "expiree", "expireeLast");
        JsonNode login = login("expiree", "StrongPass1");
        String refresh = login.get("refreshToken").asText();
        
        
        
        
        RefreshToken rt = refreshTokenRepository.findAll().getLast();
        rt.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        refreshTokenRepository.save(rt);

        String res = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(res).get("error").asText()).isEqualTo("token_expired");
    }

    @Test
    @Transactional
    void logout_revokes_token() throws Exception {
        signupUser("logoutter", "logoutter@example.com", "StrongPass1", "logoutter", "logoutterLast");
        JsonNode login = login("logoutter", "StrongPass1");
        String refresh = login.get("refreshToken").asText();
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"));
        
        String res = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(res).get("error").asText()).isEqualTo("token_revoked");
    }
}
