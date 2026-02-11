package dev.ctlabs.starter.auth.infrastructure.service;

import dev.ctlabs.starter.auth.autoconfigure.AuthProperties;
import dev.ctlabs.starter.auth.infrastructure.service.mail.EmailType;
import dev.ctlabs.starter.auth.infrastructure.service.mail.MailSenderStrategy;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Year;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class EmailService {

    private final MailSenderStrategy mailSenderStrategy;
    private final AuthProperties authProperties;
    private final Environment environment;

    public EmailService(MailSenderStrategy mailSenderStrategy,
                        AuthProperties authProperties,
                        Environment environment) {
        this.mailSenderStrategy = mailSenderStrategy;
        this.authProperties = authProperties;
        this.environment = environment;
    }

    @PostConstruct
    public void validateConfiguration() {
        if (authProperties.getNotifications().getMail().getProvider() == AuthProperties.Notifications.Mail.Provider.SMTP) {
            String mailHost = environment.getProperty("spring.mail.host");
            if (mailHost == null || mailHost.isBlank()) {
                log.warn("\n\n*** CONFIGURATION WARNING ***\nYou have selected SMTP provider, but no configuration was detected for 'spring.mail.host'.\nEmail sending is likely to fail.\n");
            }
        } else if (authProperties.getNotifications().getMail().getProvider() == AuthProperties.Notifications.Mail.Provider.BREVO) {
            String apiKey = authProperties.getNotifications().getMail().getBrevo().getApiKey();
            if (apiKey == null || apiKey.isBlank()) {
                log.warn("\n\n*** CONFIGURATION WARNING ***\nYou have selected BREVO provider, but no 'ctlabs.auth.notifications.mail.brevo.api-key' was detected.\nEmail sending will fail.\n");
            }
        }
    }

    @Async
    public void sendVerificationEmail(String to, String name, String code, long expiration, String unit) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", name);
        variables.put("expiration", expiration);
        variables.put("unit", unit);
        variables.put("currentYear", Year.now().getValue());

        String confirmationUrl = UriComponentsBuilder.fromUriString(authProperties.getFrontendUrl())
                .path("/verify-email")
                .queryParam("code", code)
                .queryParam("email", to)
                .toUriString();
        variables.put("confirmationUrl", confirmationUrl);

        mailSenderStrategy.send(name, to, EmailType.VERIFICATION, "Verify your email", variables);
    }

    @Async
    public void sendPasswordResetEmail(String to, String name, String code, long expiration, String unit) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", name);
        variables.put("expiration", expiration);
        variables.put("unit", unit);
        variables.put("currentYear", Year.now().getValue());

        String resetUrl = UriComponentsBuilder.fromUriString(authProperties.getFrontendUrl())
                .path("/reset-password")
                .queryParam("code", code)
                .queryParam("email", to)
                .toUriString();
        variables.put("resetUrl", resetUrl);

        mailSenderStrategy.send(name, to, EmailType.PASSWORD_RESET, "Reset password", variables);
    }
}