package dev.ctlabs.starter.auth.infrastructure.service.phone;

import dev.ctlabs.starter.auth.autoconfigure.AuthProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Slf4j
public class BrevoPhoneSenderStrategy implements PhoneSenderStrategy {

    private final AuthProperties authProperties;
    private final RestClient restClient;

    public BrevoPhoneSenderStrategy(AuthProperties authProperties) {
        this.authProperties = authProperties;
        this.restClient = RestClient.builder()
                .baseUrl(authProperties.getNotifications().getPhone().getBrevo().getBaseUrl())
                .defaultHeader("api-key", authProperties.getNotifications().getPhone().getBrevo().getApiKey())
                .build();
    }

    @Override
    public void send(String to, String message) {
        var phoneConfig = authProperties.getNotifications().getPhone();

        var request = new BrevoSmsRequest(
                phoneConfig.getBrevo().getSenderName(),
                to,
                message
        );

        try {
            restClient.post()
                    .uri("/transactionalSMS/sms")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
            log.info("SMS sent via Brevo to {}", to);
        } catch (Exception e) {
            log.error("Failed to send SMS via Brevo to {}", to, e);
        }
    }

    private record BrevoSmsRequest(String sender, String recipient, String content) {
    }
}