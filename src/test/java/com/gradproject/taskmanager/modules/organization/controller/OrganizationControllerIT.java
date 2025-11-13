package com.gradproject.taskmanager.modules.organization.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gradproject.taskmanager.AbstractIntegrationTest;
import com.gradproject.taskmanager.modules.auth.dto.LoginRequest;
import com.gradproject.taskmanager.modules.auth.dto.SignUpRequest;
import com.gradproject.taskmanager.modules.auth.dto.TokenResponse;
import com.gradproject.taskmanager.modules.organization.dto.OrganizationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;



@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class OrganizationControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        
        String username = "orgtest" + System.currentTimeMillis();
        String email = "orgtest" + System.currentTimeMillis() + "@example.com";

        SignUpRequest signUpRequest = new SignUpRequest(username, email, "Password123", "org", "test");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andExpect(status().isCreated());

        
        LoginRequest loginRequest = new LoginRequest(username, "Password123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        
        String responseContent = loginResult.getResponse().getContentAsString();
        TokenResponse tokenResponse = objectMapper.readValue(
                objectMapper.readTree(responseContent).get("data").toString(),
                TokenResponse.class
        );

        accessToken = tokenResponse.accessToken();
    }

    @Test
    void createOrganization_shouldSucceed() throws Exception {
        OrganizationRequest request = new OrganizationRequest(
                "Test Organization",
                "A test organization"
        );

        mockMvc.perform(post("/api/secure/organizations")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Test Organization"))
                .andExpect(jsonPath("$.data.slug").value("test-organization"))
                .andExpect(jsonPath("$.data.memberCount").value(1));
    }

    @Test
    void listMyOrganizations_shouldReturnOrganizations() throws Exception {
        
        OrganizationRequest request = new OrganizationRequest(
                "My Org",
                "My organization"
        );

        mockMvc.perform(post("/api/secure/organizations")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        
        mockMvc.perform(get("/api/secure/organizations")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("My Org"));
    }

    @Test
    void createOrganization_shouldFailWithoutAuth() throws Exception {
        OrganizationRequest request = new OrganizationRequest(
                "Test Organization",
                "A test organization"
        );

        mockMvc.perform(post("/api/secure/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
