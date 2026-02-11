package dev.ctlabs.starter.auth.infrastructure.service.phone;

import dev.ctlabs.starter.auth.autoconfigure.AuthProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Base64;

@Slf4j
public class TwilioPhoneSenderStrategy implements PhoneSenderStrategy {

    private final AuthProperties authProperties;
    private final RestClient restClient;

    public TwilioPhoneSenderStrategy(AuthProperties authProperties) {
        this.authProperties = authProperties;
        String accountSid = authProperties.getNotifications().getPhone().getTwilio().getAccountSid();
        String authToken = authProperties.getNotifications().getPhone().getTwilio().getAuthToken();

        String basicAuth = Base64.getEncoder().encodeToString((accountSid + ":" + authToken).getBytes());

        this.restClient = RestClient.builder()
                .baseUrl(authProperties.getNotifications().getPhone().getTwilio().getBaseUrl() + "/2010-04-01/Accounts/" + accountSid)
                .defaultHeader("Authorization", "Basic " + basicAuth)
                .build();
    }

    @Override
    public void send(String to, String message) {
        var phoneConfig = authProperties.getNotifications().getPhone();
        String from = phoneConfig.getFromPhoneNumber();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("To", phoneConfig.getChannel() == AuthProperties.Notifications.Phone.Channel.WHATSAPP ? "whatsapp:" + to : to);
        formData.add("From", from);
        formData.add("Body", message);

        try {
            restClient.post()
                    .uri("/Messages.json")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .toBodilessEntity();
            log.info("SMS sent via Twilio ({}) to {}", phoneConfig.getChannel(), to);
        } catch (Exception e) {
            log.error("Failed to send SMS via Twilio to {}", to, e);
        }
    }
}