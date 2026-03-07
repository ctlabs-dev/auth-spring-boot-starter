package dev.ctlabs.starter.auth.infrastructure.service.mail;

import lombok.Getter;

/**
 * Enumeration of supported email types.
 * Maps email types to their corresponding template names.
 */
@Getter
public enum EmailType {
    VERIFICATION("verification"),
    PASSWORD_RESET("password-reset");

    private final String templateName;

    EmailType(String templateName) {
        this.templateName = templateName;
    }
}
