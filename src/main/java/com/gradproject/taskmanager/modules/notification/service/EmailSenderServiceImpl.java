package com.gradproject.taskmanager.modules.notification.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * SendGrid implementation of EmailSenderService.
 *
 * <p>Sends emails using the SendGrid Java SDK. Requires a valid
 * SendGrid API key configured via application properties.
 *
 * <p>Configuration:
 * <pre>
 * app.email.sendgrid.api-key=your-api-key
 * app.email.from.address=notifications@yourdomain.com
 * app.email.from.name=Task Manager
 * </pre>
 *
 * <p>The API key should start with "SG." to be considered valid.
 * If no valid key is configured, email functionality will be disabled
 * and a warning will be logged at startup.
 */
@Service
@Slf4j
public class EmailSenderServiceImpl implements EmailSenderService {

    private static final String API_KEY_PREFIX = "SG.";
    private static final String DEFAULT_PLACEHOLDER_KEY = "your-sendgrid-api-key-here";

    private final SendGrid sendGrid;
    private final String fromEmail;
    private final String fromName;
    private final String apiKey;

    /**
     * Whether email sending is properly configured and available.
     */
    @Getter
    private boolean emailConfigured;

    /**
     * Constructor with configuration injection.
     *
     * @param apiKey SendGrid API key
     * @param fromEmail sender email address (must be verified in SendGrid)
     * @param fromName sender display name
     */
    public EmailSenderServiceImpl(
        @Value("${app.email.sendgrid.api-key}") String apiKey,
        @Value("${app.email.from.address}") String fromEmail,
        @Value("${app.email.from.name:Task Manager}") String fromName
    ) {
        this.apiKey = apiKey;
        this.sendGrid = new SendGrid(apiKey);
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        this.emailConfigured = false; // Will be validated in @PostConstruct
    }

    /**
     * Validates the SendGrid configuration on startup.
     * Logs warnings if the API key appears invalid or is a placeholder.
     */
    @PostConstruct
    public void validateConfiguration() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("========================================");
            log.warn("SendGrid API key is NOT configured!");
            log.warn("Email notifications will be DISABLED.");
            log.warn("Set SENDGRID_API_KEY environment variable");
            log.warn("or app.email.sendgrid.api-key in config.");
            log.warn("========================================");
            emailConfigured = false;
            return;
        }

        if (apiKey.equals(DEFAULT_PLACEHOLDER_KEY)) {
            log.warn("========================================");
            log.warn("SendGrid API key is set to placeholder!");
            log.warn("Email notifications will be DISABLED.");
            log.warn("Get your API key from SendGrid and");
            log.warn("set SENDGRID_API_KEY environment variable.");
            log.warn("========================================");
            emailConfigured = false;
            return;
        }

        if (!apiKey.startsWith(API_KEY_PREFIX)) {
            log.warn("========================================");
            log.warn("SendGrid API key format appears invalid!");
            log.warn("API key should start with '{}'. Current prefix: '{}'",
                API_KEY_PREFIX, apiKey.length() > 3 ? apiKey.substring(0, 3) + "..." : "???");
            log.warn("Email sending may fail. Verify your key.");
            log.warn("========================================");
            // Still mark as configured - let SendGrid validate
            emailConfigured = true;
        } else {
            emailConfigured = true;
            log.info("SendGrid email configured (from: {} <{}>)", fromName, fromEmail);
            log.debug("API key validation passed (starts with {})", API_KEY_PREFIX);
        }
    }

    @Override
    public void sendEmail(String to, String subject, String htmlContent) throws Exception {
        if (!emailConfigured) {
            log.warn("Email not sent - SendGrid is not properly configured. To: {}, Subject: {}", to, subject);
            throw new RuntimeException("Email service is not configured. Set a valid SendGrid API key.");
        }

        Email from = new Email(fromEmail, fromName);
        Email toEmail = new Email(to);
        Content content = new Content("text/html", htmlContent);
        Mail mail = new Mail(from, subject, toEmail, content);

        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);

            // SendGrid returns 202 for successful queue
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                log.info("Email sent successfully to {}: {}", to, subject);
                log.debug("SendGrid response: status={}, body={}", response.getStatusCode(), response.getBody());
            } else if (response.getStatusCode() == 401) {
                // Invalid API key
                log.error("SendGrid authentication failed - API key is invalid!");
                emailConfigured = false; // Disable further attempts
                throw new RuntimeException("SendGrid API key is invalid. Check your configuration.");
            } else {
                String error = String.format("SendGrid returned status %d: %s",
                    response.getStatusCode(), response.getBody());
                log.error("Failed to send email: {}", error);
                throw new RuntimeException(error);
            }

        } catch (IOException e) {
            log.error("Failed to send email to {} via SendGrid: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email via SendGrid: " + e.getMessage(), e);
        }
    }

    /**
     * Get the current email configuration status.
     *
     * @return status summary for health checks
     */
    public String getConfigurationStatus() {
        if (!emailConfigured) {
            if (apiKey == null || apiKey.isBlank()) {
                return "NOT_CONFIGURED: No API key set";
            } else if (apiKey.equals(DEFAULT_PLACEHOLDER_KEY)) {
                return "NOT_CONFIGURED: Using placeholder key";
            } else {
                return "INVALID: API key validation failed";
            }
        }
        return "CONFIGURED: SendGrid ready (from: " + fromEmail + ")";
    }
}
