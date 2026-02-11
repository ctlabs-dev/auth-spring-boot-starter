package dev.ctlabs.starter.auth.infrastructure.service.mail;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class NoOpMailSenderStrategy implements MailSenderStrategy {
    @Override
    public void send(String name, String to, EmailType type, String subject, Map<String, Object> variables) {
        log.warn("Email sending is disabled (Provider: NONE). Email type '{}' to '{}' with subject '{}' was not sent.", type, to, subject);
    }
}