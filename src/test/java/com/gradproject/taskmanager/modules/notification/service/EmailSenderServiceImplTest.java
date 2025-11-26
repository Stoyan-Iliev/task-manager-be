package com.gradproject.taskmanager.modules.notification.service;

import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailSenderServiceImplTest {

    @Mock
    private SendGrid mockSendGrid;

    private EmailSenderServiceImpl emailSenderService;

    private static final String FROM_EMAIL = "notifications@taskmanager.com";
    private static final String FROM_NAME = "Task Manager";
    private static final String API_KEY = "SG.test-api-key";

    @BeforeEach
    void setUp() throws Exception {
        // Create the service with real constructor
        emailSenderService = new EmailSenderServiceImpl(API_KEY, FROM_EMAIL, FROM_NAME);

        // Use reflection to inject the mock SendGrid client
        Field sendGridField = EmailSenderServiceImpl.class.getDeclaredField("sendGrid");
        sendGridField.setAccessible(true);
        sendGridField.set(emailSenderService, mockSendGrid);

        // Call validateConfiguration to set emailConfigured = true
        emailSenderService.validateConfiguration();
    }

    @Test
    void sendEmail_successfulResponse_sendsWithoutException() throws Exception {
        // Arrange
        Response response = new Response();
        response.setStatusCode(202);
        response.setBody("{}");
        when(mockSendGrid.api(any(Request.class))).thenReturn(response);

        // Act
        emailSenderService.sendEmail("recipient@example.com", "Test Subject", "<html>Content</html>");

        // Assert
        verify(mockSendGrid).api(any(Request.class));
    }

    @Test
    void sendEmail_verifiesRequestContainsCorrectData() throws Exception {
        // Arrange
        Response response = new Response();
        response.setStatusCode(202);
        when(mockSendGrid.api(any(Request.class))).thenReturn(response);

        // Act
        emailSenderService.sendEmail("recipient@example.com", "Test Subject", "<html>Content</html>");

        // Assert
        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mockSendGrid).api(requestCaptor.capture());

        Request request = requestCaptor.getValue();
        assertThat(request.getEndpoint()).isEqualTo("mail/send");
        assertThat(request.getBody()).contains("recipient@example.com");
        assertThat(request.getBody()).contains("Test Subject");
    }

    @Test
    void sendEmail_status4xx_throwsException() throws Exception {
        // Arrange
        Response response = new Response();
        response.setStatusCode(400);
        response.setBody("{\"errors\":[{\"message\":\"Bad request\"}]}");
        when(mockSendGrid.api(any(Request.class))).thenReturn(response);

        // Act & Assert
        assertThatThrownBy(() ->
            emailSenderService.sendEmail("recipient@example.com", "Test Subject", "<html>Content</html>")
        )
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("400");
    }

    @Test
    void sendEmail_status5xx_throwsException() throws Exception {
        // Arrange
        Response response = new Response();
        response.setStatusCode(500);
        response.setBody("Internal Server Error");
        when(mockSendGrid.api(any(Request.class))).thenReturn(response);

        // Act & Assert
        assertThatThrownBy(() ->
            emailSenderService.sendEmail("recipient@example.com", "Test Subject", "<html>Content</html>")
        )
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("500");
    }

    @Test
    void sendEmail_ioException_throwsWrappedException() throws Exception {
        // Arrange
        when(mockSendGrid.api(any(Request.class))).thenThrow(new IOException("Network error"));

        // Act & Assert
        assertThatThrownBy(() ->
            emailSenderService.sendEmail("recipient@example.com", "Test Subject", "<html>Content</html>")
        )
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Network error");
    }

    @Test
    void sendEmail_status200_succeeds() throws Exception {
        // Arrange
        Response response = new Response();
        response.setStatusCode(200);
        response.setBody("{}");
        when(mockSendGrid.api(any(Request.class))).thenReturn(response);

        // Act - should not throw
        emailSenderService.sendEmail("recipient@example.com", "Test Subject", "<html>Content</html>");

        // Assert
        verify(mockSendGrid).api(any(Request.class));
    }

    @Test
    void sendEmail_status201_succeeds() throws Exception {
        // Arrange
        Response response = new Response();
        response.setStatusCode(201);
        response.setBody("{}");
        when(mockSendGrid.api(any(Request.class))).thenReturn(response);

        // Act - should not throw
        emailSenderService.sendEmail("recipient@example.com", "Test Subject", "<html>Content</html>");

        // Assert
        verify(mockSendGrid).api(any(Request.class));
    }

    @Test
    void sendEmail_status299_succeeds() throws Exception {
        // Arrange - test edge case of 2xx range
        Response response = new Response();
        response.setStatusCode(299);
        response.setBody("{}");
        when(mockSendGrid.api(any(Request.class))).thenReturn(response);

        // Act - should not throw
        emailSenderService.sendEmail("recipient@example.com", "Test Subject", "<html>Content</html>");

        // Assert
        verify(mockSendGrid).api(any(Request.class));
    }

    @Test
    void sendEmail_status300_throwsException() throws Exception {
        // Arrange - redirect status should fail
        Response response = new Response();
        response.setStatusCode(300);
        response.setBody("{}");
        when(mockSendGrid.api(any(Request.class))).thenReturn(response);

        // Act & Assert
        assertThatThrownBy(() ->
            emailSenderService.sendEmail("recipient@example.com", "Test Subject", "<html>Content</html>")
        )
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("300");
    }

    @Test
    void sendEmail_status401_throwsUnauthorizedException() throws Exception {
        // Arrange - unauthorized status
        Response response = new Response();
        response.setStatusCode(401);
        response.setBody("{\"errors\":[{\"message\":\"API key invalid\"}]}");
        when(mockSendGrid.api(any(Request.class))).thenReturn(response);

        // Act & Assert
        assertThatThrownBy(() ->
            emailSenderService.sendEmail("recipient@example.com", "Test Subject", "<html>Content</html>")
        )
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("invalid");
    }

    @Test
    void sendEmail_status429_throwsRateLimitException() throws Exception {
        // Arrange - rate limit exceeded
        Response response = new Response();
        response.setStatusCode(429);
        response.setBody("{\"errors\":[{\"message\":\"Rate limit exceeded\"}]}");
        when(mockSendGrid.api(any(Request.class))).thenReturn(response);

        // Act & Assert
        assertThatThrownBy(() ->
            emailSenderService.sendEmail("recipient@example.com", "Test Subject", "<html>Content</html>")
        )
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("429");
    }

    // ==================== API Key Validation Tests ====================

    @Test
    void isEmailConfigured_withValidApiKey_returnsTrue() {
        // Arrange - setUp already creates service with valid API key
        // Act & Assert
        assertThat(emailSenderService.isEmailConfigured()).isTrue();
    }

    @Test
    void isEmailConfigured_withBlankApiKey_returnsFalse() {
        // Arrange
        EmailSenderServiceImpl service = new EmailSenderServiceImpl("", FROM_EMAIL, FROM_NAME);
        service.validateConfiguration();

        // Act & Assert
        assertThat(service.isEmailConfigured()).isFalse();
    }

    @Test
    void isEmailConfigured_withNullApiKey_returnsFalse() {
        // Arrange
        EmailSenderServiceImpl service = new EmailSenderServiceImpl(null, FROM_EMAIL, FROM_NAME);
        service.validateConfiguration();

        // Act & Assert
        assertThat(service.isEmailConfigured()).isFalse();
    }

    @Test
    void isEmailConfigured_withPlaceholderApiKey_returnsFalse() {
        // Arrange
        EmailSenderServiceImpl service = new EmailSenderServiceImpl("your-sendgrid-api-key-here", FROM_EMAIL, FROM_NAME);
        service.validateConfiguration();

        // Act & Assert
        assertThat(service.isEmailConfigured()).isFalse();
    }

    @Test
    void isEmailConfigured_withInvalidPrefixApiKey_stillReturnsTrue() {
        // Arrange - Invalid prefix but still marked as configured to let SendGrid validate
        EmailSenderServiceImpl service = new EmailSenderServiceImpl("invalid-key", FROM_EMAIL, FROM_NAME);
        service.validateConfiguration();

        // Act & Assert
        assertThat(service.isEmailConfigured()).isTrue();
    }

    @Test
    void getConfigurationStatus_whenConfigured_returnsConfiguredMessage() {
        // Arrange - setUp already creates service with valid API key
        // Act
        String status = emailSenderService.getConfigurationStatus();

        // Assert
        assertThat(status).contains("CONFIGURED");
        assertThat(status).contains(FROM_EMAIL);
    }

    @Test
    void getConfigurationStatus_whenBlankKey_returnsNotConfiguredMessage() {
        // Arrange
        EmailSenderServiceImpl service = new EmailSenderServiceImpl("", FROM_EMAIL, FROM_NAME);
        service.validateConfiguration();

        // Act
        String status = service.getConfigurationStatus();

        // Assert
        assertThat(status).contains("NOT_CONFIGURED");
        assertThat(status).contains("No API key");
    }

    @Test
    void getConfigurationStatus_whenPlaceholderKey_returnsPlaceholderMessage() {
        // Arrange
        EmailSenderServiceImpl service = new EmailSenderServiceImpl("your-sendgrid-api-key-here", FROM_EMAIL, FROM_NAME);
        service.validateConfiguration();

        // Act
        String status = service.getConfigurationStatus();

        // Assert
        assertThat(status).contains("NOT_CONFIGURED");
        assertThat(status).contains("placeholder");
    }

    @Test
    void sendEmail_whenNotConfigured_throwsException() {
        // Arrange
        EmailSenderServiceImpl service = new EmailSenderServiceImpl("", FROM_EMAIL, FROM_NAME);
        service.validateConfiguration();

        // Act & Assert
        assertThatThrownBy(() ->
            service.sendEmail("recipient@example.com", "Test Subject", "<html>Content</html>")
        )
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("not configured");
    }

    @Test
    void sendEmail_status401_disablesFurtherSending() throws Exception {
        // Arrange - 401 should disable email sending
        Response response = new Response();
        response.setStatusCode(401);
        response.setBody("{\"errors\":[{\"message\":\"API key invalid\"}]}");
        when(mockSendGrid.api(any(Request.class))).thenReturn(response);

        // Act - first call should throw and disable
        assertThatThrownBy(() ->
            emailSenderService.sendEmail("recipient@example.com", "Test Subject", "<html>Content</html>")
        ).isInstanceOf(RuntimeException.class);

        // Assert - emailConfigured should be false now
        assertThat(emailSenderService.isEmailConfigured()).isFalse();
    }
}
