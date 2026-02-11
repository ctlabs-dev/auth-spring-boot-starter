package dev.ctlabs.starter.auth.infrastructure.config;

import dev.ctlabs.starter.auth.autoconfigure.AuthProperties;
import dev.ctlabs.starter.auth.infrastructure.service.phone.BrevoPhoneSenderStrategy;
import dev.ctlabs.starter.auth.infrastructure.service.phone.NoOpPhoneSenderStrategy;
import dev.ctlabs.starter.auth.infrastructure.service.phone.PhoneSenderStrategy;
import dev.ctlabs.starter.auth.infrastructure.service.phone.TwilioPhoneSenderStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PhoneConfig {

    @Bean
    @ConditionalOnProperty(prefix = "ctlabs.auth.notifications.phone", name = "provider", havingValue = "NONE", matchIfMissing = true)
    public PhoneSenderStrategy noOpPhoneSenderStrategy() {
        return new NoOpPhoneSenderStrategy();
    }

    @Bean
    @ConditionalOnProperty(prefix = "ctlabs.auth.notifications.phone", name = "provider", havingValue = "TWILIO")
    public PhoneSenderStrategy twilioPhoneSenderStrategy(AuthProperties authProperties) {
        return new TwilioPhoneSenderStrategy(authProperties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "ctlabs.auth.notifications.phone", name = "provider", havingValue = "BREVO")
    public PhoneSenderStrategy brevoPhoneSenderStrategy(AuthProperties authProperties) {
        return new BrevoPhoneSenderStrategy(authProperties);
    }
}