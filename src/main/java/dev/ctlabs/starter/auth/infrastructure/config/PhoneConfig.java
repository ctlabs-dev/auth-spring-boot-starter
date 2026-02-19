package dev.ctlabs.starter.auth.infrastructure.config;

import dev.ctlabs.starter.auth.autoconfigure.AuthProperties;
import dev.ctlabs.starter.auth.infrastructure.service.phone.BrevoPhoneSenderStrategy;
import dev.ctlabs.starter.auth.infrastructure.service.phone.NoOpPhoneSenderStrategy;
import dev.ctlabs.starter.auth.infrastructure.service.phone.PhoneSenderStrategy;
import dev.ctlabs.starter.auth.infrastructure.service.phone.TwilioPhoneSenderStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for phone messaging strategies.
 * Configures the appropriate {@link PhoneSenderStrategy} based on properties.
 */
@Configuration
public class PhoneConfig {

    /**
     * Creates a No-Op phone sender strategy.
     * Used when phone notifications are disabled.
     *
     * @return The configured {@link NoOpPhoneSenderStrategy}.
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "ctlabs.auth.notifications.phone",
            name = "provider",
            havingValue = "NONE",
            matchIfMissing = true)
    public PhoneSenderStrategy noOpPhoneSenderStrategy() {
        return new NoOpPhoneSenderStrategy();
    }

    /**
     * Creates a Twilio phone sender strategy.
     *
     * @param authProperties The authentication properties.
     * @return The configured {@link TwilioPhoneSenderStrategy}.
     */
    @Bean
    @ConditionalOnProperty(prefix = "ctlabs.auth.notifications.phone", name = "provider", havingValue = "TWILIO")
    public PhoneSenderStrategy twilioPhoneSenderStrategy(AuthProperties authProperties) {
        return new TwilioPhoneSenderStrategy(authProperties);
    }

    /**
     * Creates a Brevo phone sender strategy.
     *
     * @param authProperties The authentication properties.
     * @return The configured {@link BrevoPhoneSenderStrategy}.
     */
    @Bean
    @ConditionalOnProperty(prefix = "ctlabs.auth.notifications.phone", name = "provider", havingValue = "BREVO")
    public PhoneSenderStrategy brevoPhoneSenderStrategy(AuthProperties authProperties) {
        return new BrevoPhoneSenderStrategy(authProperties);
    }
}
