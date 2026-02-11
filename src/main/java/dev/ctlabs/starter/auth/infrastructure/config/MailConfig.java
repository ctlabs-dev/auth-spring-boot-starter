package dev.ctlabs.starter.auth.infrastructure.config;

import dev.ctlabs.starter.auth.autoconfigure.AuthProperties;
import dev.ctlabs.starter.auth.infrastructure.service.mail.BrevoMailSenderStrategy;
import dev.ctlabs.starter.auth.infrastructure.service.mail.MailSenderStrategy;
import dev.ctlabs.starter.auth.infrastructure.service.mail.NoOpMailSenderStrategy;
import dev.ctlabs.starter.auth.infrastructure.service.mail.SmtpMailSenderStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;

@Configuration
public class MailConfig {

    @Bean
    @ConditionalOnProperty(prefix = "ctlabs.auth.notifications.mail", name = "provider", havingValue = "SMTP")
    public MailSenderStrategy smtpMailSenderStrategy(JavaMailSender javaMailSender,
                                                     AuthProperties authProperties,
                                                     TemplateEngine templateEngine) {
        return new SmtpMailSenderStrategy(javaMailSender, authProperties, templateEngine);
    }

    @Bean
    @ConditionalOnProperty(prefix = "ctlabs.auth.notifications.mail", name = "provider", havingValue = "NONE", matchIfMissing = true)
    public MailSenderStrategy noOpMailSenderStrategy() {
        return new NoOpMailSenderStrategy();
    }

    @Bean
    @ConditionalOnProperty(prefix = "ctlabs.auth.notifications.mail", name = "provider", havingValue = "BREVO")
    public MailSenderStrategy brevoMailSenderStrategy(AuthProperties authProperties) {
        return new BrevoMailSenderStrategy(authProperties);
    }
}