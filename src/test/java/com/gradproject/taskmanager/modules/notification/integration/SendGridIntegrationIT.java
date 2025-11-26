package com.gradproject.taskmanager.modules.notification.integration;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for SendGrid API connectivity.
 *
 * <p>These tests are OPTIONAL and only run when the SENDGRID_TEST_API_KEY
 * environment variable is set. They verify real API connectivity using
 * SendGrid's sandbox mode (emails are validated but not actually delivered).
 *
 * <p>To run these tests:
 * <pre>
 * export SENDGRID_TEST_API_KEY=SG.your-test-api-key
 * mvn test -Dtest=SendGridIntegrationIT
 * </pre>
 *
 * <p>Note: The test API key must have at least "Mail Send" permissions.
 * Using sandbox mode ensures no actual emails are sent.
 */
@EnabledIfEnvironmentVariable(named = "SENDGRID_TEST_API_KEY", matches = "SG\\..*")
class SendGridIntegrationIT {

    private SendGrid sendGrid;
    private String testApiKey;

    @BeforeEach
    void setUp() {
        testApiKey = System.getenv("SENDGRID_TEST_API_KEY");
        sendGrid = new SendGrid(testApiKey);
    }

    @Test
    void sendGrid_apiKeyIsValid_canConnectToApi() throws Exception {
        // Arrange - Create a simple test email in sandbox mode
        Email from = new Email("test@example.com", "Test Sender");
        Email to = new Email("recipient@example.com");
        String subject = "SendGrid Integration Test";
        Content content = new Content("text/plain", "This is a test email from integration tests.");
        Mail mail = new Mail(from, subject, to, content);

        // Enable sandbox mode - email won't actually be sent
        com.sendgrid.helpers.mail.objects.MailSettings mailSettings = new com.sendgrid.helpers.mail.objects.MailSettings();
        com.sendgrid.helpers.mail.objects.Setting sandboxSetting = new com.sendgrid.helpers.mail.objects.Setting();
        sandboxSetting.setEnable(true);
        mailSettings.setSandboxMode(sandboxSetting);
        mail.setMailSettings(mailSettings);

        // Act
        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());

        Response response = sendGrid.api(request);

        // Assert
        // In sandbox mode, 200 indicates successful validation
        assertThat(response.getStatusCode())
            .as("SendGrid should accept the request in sandbox mode")
            .isIn(200, 202);
    }

    @Test
    void sendGrid_htmlEmail_validatesSuccessfully() throws Exception {
        // Arrange - Create an HTML email similar to our digest emails
        Email from = new Email("notifications@taskmanager.com", "Task Manager");
        Email to = new Email("recipient@example.com");
        String subject = "[PROJ-123] Test Task - Status Changed";
        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body>
                <div style="font-family: Arial, sans-serif;">
                    <h1>Task Update</h1>
                    <p>The status of task PROJ-123 has been changed.</p>
                    <a href="http://localhost:5173/tasks/PROJ-123">View Task</a>
                </div>
            </body>
            </html>
            """;
        Content content = new Content("text/html", htmlContent);
        Mail mail = new Mail(from, subject, to, content);

        // Enable sandbox mode
        com.sendgrid.helpers.mail.objects.MailSettings mailSettings2 = new com.sendgrid.helpers.mail.objects.MailSettings();
        com.sendgrid.helpers.mail.objects.Setting sandboxSetting2 = new com.sendgrid.helpers.mail.objects.Setting();
        sandboxSetting2.setEnable(true);
        mailSettings2.setSandboxMode(sandboxSetting2);
        mail.setMailSettings(mailSettings2);

        // Act
        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());

        Response response = sendGrid.api(request);

        // Assert
        assertThat(response.getStatusCode())
            .as("SendGrid should validate HTML email successfully")
            .isIn(200, 202);
    }

    @Test
    void sendGrid_invalidApiKey_returns401() throws Exception {
        // Arrange - Use an invalid API key
        SendGrid invalidClient = new SendGrid("SG.invalid-key-for-testing");

        Email from = new Email("test@example.com");
        Email to = new Email("recipient@example.com");
        Mail mail = new Mail(from, "Test", to, new Content("text/plain", "Test"));

        // Act
        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());

        Response response = invalidClient.api(request);

        // Assert
        assertThat(response.getStatusCode())
            .as("Invalid API key should return 401 Unauthorized")
            .isEqualTo(401);
    }

    @Test
    void sendGrid_emptySubject_validatesSuccessfully() throws Exception {
        // Arrange - SendGrid allows empty subjects
        Email from = new Email("test@example.com", "Test Sender");
        Email to = new Email("recipient@example.com");
        String subject = ""; // Empty subject
        Content content = new Content("text/plain", "Message without subject");
        Mail mail = new Mail(from, subject, to, content);

        com.sendgrid.helpers.mail.objects.MailSettings mailSettings3 = new com.sendgrid.helpers.mail.objects.MailSettings();
        com.sendgrid.helpers.mail.objects.Setting sandboxSetting3 = new com.sendgrid.helpers.mail.objects.Setting();
        sandboxSetting3.setEnable(true);
        mailSettings3.setSandboxMode(sandboxSetting3);
        mail.setMailSettings(mailSettings3);

        // Act
        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());

        Response response = sendGrid.api(request);

        // Assert
        assertThat(response.getStatusCode()).isIn(200, 202);
    }

    @Test
    void sendGrid_unicodeContent_validatesSuccessfully() throws Exception {
        // Arrange - Test with unicode characters (emojis, etc.)
        Email from = new Email("test@example.com", "Test Sender");
        Email to = new Email("recipient@example.com");
        String subject = "Task Update 🔄";
        String htmlContent = """
            <html>
            <body>
                <p>✨ New task created</p>
                <p>👤 Assigned to user</p>
                <p>💬 Comment added: "Great work!"</p>
            </body>
            </html>
            """;
        Content content = new Content("text/html", htmlContent);
        Mail mail = new Mail(from, subject, to, content);

        com.sendgrid.helpers.mail.objects.MailSettings mailSettings4 = new com.sendgrid.helpers.mail.objects.MailSettings();
        com.sendgrid.helpers.mail.objects.Setting sandboxSetting4 = new com.sendgrid.helpers.mail.objects.Setting();
        sandboxSetting4.setEnable(true);
        mailSettings4.setSandboxMode(sandboxSetting4);
        mail.setMailSettings(mailSettings4);

        // Act
        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());

        Response response = sendGrid.api(request);

        // Assert
        assertThat(response.getStatusCode())
            .as("SendGrid should handle unicode content")
            .isIn(200, 202);
    }
}
