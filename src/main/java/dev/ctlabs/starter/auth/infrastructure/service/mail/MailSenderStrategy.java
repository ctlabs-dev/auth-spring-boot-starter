package dev.ctlabs.starter.auth.infrastructure.service.mail;

import java.util.Map;

public interface MailSenderStrategy {
    void send(String name, String to, EmailType type, String subject, Map<String, Object> variables);
}