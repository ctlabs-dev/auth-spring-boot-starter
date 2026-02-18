package dev.ctlabs.starter.auth.infrastructure.service.mail;

import java.util.Map;

/**
 * Strategy interface for sending emails.
 * Implementations define the logic for specific email providers.
 */
public interface MailSenderStrategy {
    /**
     * Sends an email.
     *
     * @param name      The recipient's name.
     * @param to        The recipient's email address.
     * @param type      The type of email (e.g., verification, password reset).
     * @param subject   The email subject.
     * @param variables Variables to be replaced in the email template.
     */
    void send(String name, String to, EmailType type, String subject, Map<String, Object> variables);
}
