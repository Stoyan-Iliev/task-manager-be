package com.gradproject.taskmanager.modules.notification.service;

/**
 * Service for sending emails via SendGrid.
 *
 * <p>Provides a simple interface for email delivery, abstracting
 * the SendGrid API details. This makes it easy to mock for testing
 * or swap implementations if needed.
 */
public interface EmailSenderService {

    /**
     * Send an email via SendGrid.
     *
     * @param to recipient email address
     * @param subject email subject line
     * @param htmlContent HTML email body
     * @throws Exception if email sending fails
     */
    void sendEmail(String to, String subject, String htmlContent) throws Exception;

    /**
     * Check if email sending is properly configured.
     *
     * @return true if email can be sent, false if not configured
     */
    boolean isEmailConfigured();

    /**
     * Get the current configuration status for health checks.
     *
     * @return human-readable status description
     */
    String getConfigurationStatus();
}
