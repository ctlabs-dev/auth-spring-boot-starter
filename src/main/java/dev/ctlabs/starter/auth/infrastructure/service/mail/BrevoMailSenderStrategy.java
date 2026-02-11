package dev.ctlabs.starter.auth.infrastructure.service.mail;

import dev.ctlabs.starter.auth.autoconfigure.AuthProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
public class BrevoMailSenderStrategy implements MailSenderStrategy {

    private final AuthProperties authProperties;
    private final RestClient restClient;

    public BrevoMailSenderStrategy(AuthProperties authProperties) {
        this.authProperties = authProperties;
        this.restClient = RestClient.builder()
                .baseUrl(authProperties.getNotifications().getMail().getBrevo().getBaseUrl())
                .defaultHeader("api-key", authProperties.getNotifications().getMail().getBrevo().getApiKey())
                .build();
    }

    @Override
    public void send(String name, String to, EmailType type, String subject, Map<String, Object> variables) {
        Integer templateId = getTemplateId(type);

        if (templateId == null) {
            log.error("Template ID for email type '{}' is not configured in AuthProperties. Skipping Brevo email to '{}'.", type, to);
            return;
        }

        var request = new BrevoEmailRequest(
                List.of(new Recipient(name, to)),
                templateId,
                variables
        );

        try {
            restClient.post()
                    .uri("/smtp/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Email sent via Brevo to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email via Brevo to {}", to, e);
        }
    }

    private Integer getTemplateId(EmailType type) {
        return switch (type) {
            case VERIFICATION -> authProperties.getNotifications().getMail().getBrevo().getVerificationTemplateId();
            case PASSWORD_RESET -> authProperties.getNotifications().getMail().getBrevo().getPasswordResetTemplateId();
        };
    }

    private record Recipient(String name, String email) {
    }

    private record BrevoEmailRequest(List<Recipient> to, long templateId, Map<String, Object> params) {
    }
}