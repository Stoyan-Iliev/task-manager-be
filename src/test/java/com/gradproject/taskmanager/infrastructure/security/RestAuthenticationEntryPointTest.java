package com.gradproject.taskmanager.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RestAuthenticationEntryPointTest {

    @Mock
    private HttpServletRequest request;

    private RestAuthenticationEntryPoint entryPoint;
    private MockHttpServletResponse response;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        entryPoint = new RestAuthenticationEntryPoint();
        response = new MockHttpServletResponse();
        objectMapper = new ObjectMapper();
    }

    @Test
    void commence_shouldSetUnauthorizedStatus() throws IOException {
        
        AuthenticationException exception = new BadCredentialsException("Invalid credentials");

        
        entryPoint.commence(request, response, exception);

        
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void commence_shouldSetJsonContentType() throws IOException {
        
        AuthenticationException exception = new BadCredentialsException("Invalid credentials");

        
        entryPoint.commence(request, response, exception);

        
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
    }

    @Test
    void commence_shouldSetCacheControlHeaders() throws IOException {
        
        AuthenticationException exception = new BadCredentialsException("Invalid credentials");

        
        entryPoint.commence(request, response, exception);

        
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(response.getHeader("Pragma")).isEqualTo("no-cache");
    }

    @Test
    void commence_shouldSetWwwAuthenticateHeader() throws IOException {
        
        AuthenticationException exception = new BadCredentialsException("Invalid credentials");

        
        entryPoint.commence(request, response, exception);

        
        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
    }

    @Test
    void commence_shouldWriteErrorResponseBody() throws IOException {
        
        AuthenticationException exception = new BadCredentialsException("Invalid credentials");

        
        entryPoint.commence(request, response, exception);

        
        String responseBody = response.getContentAsString();
        assertThat(responseBody).isNotEmpty();

        @SuppressWarnings("unchecked")
        Map<String, Object> body = objectMapper.readValue(responseBody, Map.class);
        assertThat(body).containsEntry("error", "invalid_token");
    }

    @Test
    void commence_shouldHandleInvalidBearerTokenException() throws IOException {
        
        AuthenticationException exception = new InvalidBearerTokenException("Token expired");

        
        entryPoint.commence(request, response, exception);

        
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        String responseBody = response.getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> body = objectMapper.readValue(responseBody, Map.class);
        assertThat(body).containsEntry("error", "invalid_token");
    }

    @Test
    void commence_shouldHandleDifferentAuthenticationExceptions() throws IOException {
        
        AuthenticationException exception = new BadCredentialsException("User not found");

        
        entryPoint.commence(request, response, exception);

        
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        String responseBody = response.getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> body = objectMapper.readValue(responseBody, Map.class);
        assertThat(body).containsEntry("error", "invalid_token");
    }

    @Test
    void commence_shouldAlwaysReturnSameErrorResponse() throws IOException {
        
        AuthenticationException exception1 = new BadCredentialsException("Error 1");
        AuthenticationException exception2 = new InvalidBearerTokenException("Error 2");

        MockHttpServletResponse response1 = new MockHttpServletResponse();
        MockHttpServletResponse response2 = new MockHttpServletResponse();

        
        entryPoint.commence(request, response1, exception1);
        entryPoint.commence(request, response2, exception2);

        
        assertThat(response1.getContentAsString()).isEqualTo(response2.getContentAsString());
    }

    @Test
    void commence_shouldIncludeAllRequiredHeaders() throws IOException {
        
        AuthenticationException exception = new BadCredentialsException("Invalid credentials");

        
        entryPoint.commence(request, response, exception);

        
        assertThat(response.getHeader("WWW-Authenticate")).isNotNull();
        assertThat(response.getHeader("Cache-Control")).isNotNull();
        assertThat(response.getHeader("Pragma")).isNotNull();
        assertThat(response.getContentType()).isNotNull();
    }
}
