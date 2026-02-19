package dev.ctlabs.starter.auth.infrastructure.config;

import dev.ctlabs.starter.auth.autoconfigure.AuthProperties;
import dev.ctlabs.starter.auth.infrastructure.service.mail.BrevoMailSenderStrategy;
import dev.ctlabs.starter.auth.infrastructure.service.mail.MailSenderStrategy;
import dev.ctlabs.starter.auth.infrastructure.service.mail.NoOpMailSenderStrategy;
import dev.ctlabs.starter.auth.infrastructure.service.mail.SmtpMailSenderStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;

/**
 * Configuration for email sending strategies. Configures the appropriate {@link MailSenderStrategy}
 * based on properties.
 */
@Configuration
public class MailConfig {

    /**
     * Creates an SMTP mail sender strategy.
     *
     * @param javaMailSender The JavaMailSender instance.
     * @param authProperties The authentication properties.
     * @param templateEngine The Thymeleaf template engine.
     * @param environment The Spring Environment.
     * @return The configured {@link SmtpMailSenderStrategy}.
     */
    @Bean
    @ConditionalOnProperty(prefix = "ctlabs.auth.notifications.mail", name = "provider", havingValue = "SMTP")
    public MailSenderStrategy smtpMailSenderStrategy(
            JavaMailSender javaMailSender,
            AuthProperties authProperties,
            TemplateEngine templateEngine,
            Environment environment) {
        return new SmtpMailSenderStrategy(javaMailSender, authProperties, templateEngine, environment);
    }

    /**
     * Creates a No-Op mail sender strategy. Used when email notifications are disabled.
     *
     * @return The configured {@link NoOpMailSenderStrategy}.
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "ctlabs.auth.notifications.mail",
            name = "provider",
            havingValue = "NONE",
            matchIfMissing = true)
    public MailSenderStrategy noOpMailSenderStrategy() {
        return new NoOpMailSenderStrategy();
    }

    /**
     * Creates a Brevo mail sender strategy.
     *
     * @param authProperties The authentication properties.
     * @return The configured {@link BrevoMailSenderStrategy}.
     */
    @Bean
    @ConditionalOnProperty(prefix = "ctlabs.auth.notifications.mail", name = "provider", havingValue = "BREVO")
    public MailSenderStrategy brevoMailSenderStrategy(AuthProperties authProperties) {
        return new BrevoMailSenderStrategy(authProperties);
    }
}
