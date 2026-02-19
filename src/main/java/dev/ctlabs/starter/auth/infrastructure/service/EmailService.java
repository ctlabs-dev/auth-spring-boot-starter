package dev.ctlabs.starter.auth.infrastructure.service;

import dev.ctlabs.starter.auth.autoconfigure.AuthProperties;
import dev.ctlabs.starter.auth.infrastructure.service.mail.EmailType;
import dev.ctlabs.starter.auth.infrastructure.service.mail.MailSenderStrategy;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Year;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for handling email operations.
 * Uses the configured {@link MailSenderStrategy} to send emails.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final MailSenderStrategy mailSenderStrategy;
    private final AuthProperties authProperties;
    private final Environment environment;

    @PostConstruct
    public void validateConfiguration() {
        var mailProps = authProperties.getNotifications().getMail();
        if (mailProps.getProvider() == AuthProperties.Notifications.Mail.Provider.SMTP) {
            String mailHost = environment.getProperty("spring.mail.host");
            if (mailHost == null || mailHost.isBlank()) {
                log.warn("\n\n"
                        + "*** CONFIGURATION WARNING ***\n"
                        + "You have selected SMTP provider, but no configuration was detected for"
                        + " 'spring.mail.host'.\n"
                        + "Email sending is likely to fail.\n");
            }
            if (mailProps.getSmtp().getFromEmail() == null || mailProps.getSmtp().getFromEmail().isBlank()) {
                log.warn("\n\n"
                        + "*** CONFIGURATION WARNING ***\n"
                        + "You have selected SMTP provider, but 'ctlabs.auth.notifications.mail.smtp.from-email'"
                        + " is missing.\n"
                        + "Emails might be rejected by receivers.\n");
            }
        } else if (mailProps.getProvider() == AuthProperties.Notifications.Mail.Provider.BREVO) {
            String apiKey = mailProps.getBrevo().getApiKey();
            if (apiKey == null || apiKey.isBlank()) {
                log.warn("\n\n"
                        + "*** CONFIGURATION WARNING ***\n"
                        + "You have selected BREVO provider, but no 'ctlabs.auth.notifications.mail.brevo.api-key'"
                        + " was detected.\n"
                        + "Email sending will fail.\n");
            }
        }
    }

    /**
     * Sends a verification email to a user.
     *
     * @param to         The recipient's email address.
     * @param name       The recipient's name.
     * @param code       The verification code.
     * @param expiration The expiration time value.
     * @param unit       The expiration time unit (e.g., "minutes").
     */
    @Async
    public void sendVerificationEmail(String to, String name, String code, long expiration, String unit) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", name);
        variables.put("expiration", expiration);
        variables.put("unit", unit);
        variables.put("currentYear", Year.now().getValue());

        String confirmationUrl = UriComponentsBuilder.fromUriString(authProperties.getApplication().getFrontendUrl())
                .path("/verify-email")
                .queryParam("code", code)
                .queryParam("email", to)
                .toUriString();
        variables.put("confirmationUrl", confirmationUrl);

        mailSenderStrategy.send(name, to, EmailType.VERIFICATION, "Verify your email", variables);
    }

    /**
     * Sends a password reset email to a user.
     *
     * @param to         The recipient's email address.
     * @param name       The recipient's name.
     * @param code       The reset code.
     * @param expiration The expiration time value.
     * @param unit       The expiration time unit.
     */
    @Async
    public void sendPasswordResetEmail(String to, String name, String code, long expiration, String unit) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", name);
        variables.put("expiration", expiration);
        variables.put("unit", unit);
        variables.put("currentYear", Year.now().getValue());

        String resetUrl = UriComponentsBuilder.fromUriString(authProperties.getApplication().getFrontendUrl())
                .path("/reset-password")
                .queryParam("code", code)
                .queryParam("email", to)
                .toUriString();
        variables.put("resetUrl", resetUrl);

        mailSenderStrategy.send(name, to, EmailType.PASSWORD_RESET, "Reset password", variables);
    }
}
