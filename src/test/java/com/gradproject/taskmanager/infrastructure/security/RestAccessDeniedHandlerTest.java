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
import org.springframework.security.access.AccessDeniedException;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RestAccessDeniedHandlerTest {

    @Mock
    private HttpServletRequest request;

    private RestAccessDeniedHandler handler;
    private MockHttpServletResponse response;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        handler = new RestAccessDeniedHandler();
        response = new MockHttpServletResponse();
        objectMapper = new ObjectMapper();
    }

    @Test
    void handle_shouldSetForbiddenStatus() throws IOException {
        
        AccessDeniedException exception = new AccessDeniedException("Access is denied");

        
        handler.handle(request, response, exception);

        
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    void handle_shouldSetJsonContentType() throws IOException {
        
        AccessDeniedException exception = new AccessDeniedException("Access is denied");

        
        handler.handle(request, response, exception);

        
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
    }

    @Test
    void handle_shouldSetCacheControlHeaders() throws IOException {
        
        AccessDeniedException exception = new AccessDeniedException("Access is denied");

        
        handler.handle(request, response, exception);

        
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(response.getHeader("Pragma")).isEqualTo("no-cache");
    }

    @Test
    void handle_shouldWriteErrorResponseBody() throws IOException {
        
        AccessDeniedException exception = new AccessDeniedException("Access is denied");

        
        handler.handle(request, response, exception);

        
        String responseBody = response.getContentAsString();
        assertThat(responseBody).isNotEmpty();

        @SuppressWarnings("unchecked")
        Map<String, Object> body = objectMapper.readValue(responseBody, Map.class);
        assertThat(body).containsEntry("error", "access_denied");
    }

    @Test
    void handle_shouldHandleDifferentExceptionMessages() throws IOException {
        
        AccessDeniedException exception = new AccessDeniedException("User does not have required role");

        
        handler.handle(request, response, exception);

        
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
        String responseBody = response.getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> body = objectMapper.readValue(responseBody, Map.class);
        assertThat(body).containsEntry("error", "access_denied");
    }

    @Test
    void handle_shouldAlwaysReturnSameErrorResponse() throws IOException {
        
        AccessDeniedException exception1 = new AccessDeniedException("Message 1");
        AccessDeniedException exception2 = new AccessDeniedException("Message 2");

        MockHttpServletResponse response1 = new MockHttpServletResponse();
        MockHttpServletResponse response2 = new MockHttpServletResponse();

        
        handler.handle(request, response1, exception1);
        handler.handle(request, response2, exception2);

        
        assertThat(response1.getContentAsString()).isEqualTo(response2.getContentAsString());
    }
}
