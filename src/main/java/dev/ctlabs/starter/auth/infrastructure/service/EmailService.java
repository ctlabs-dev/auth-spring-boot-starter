package dev.ctlabs.starter.auth.infrastructure.service;

import dev.ctlabs.starter.auth.autoconfigure.AuthProperties;
import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final AuthProperties authProperties;
    private final Environment environment;

    public EmailService(JavaMailSender mailSender,
                        TemplateEngine templateEngine,
                        AuthProperties authProperties,
                        Environment environment) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.authProperties = authProperties;
        this.environment = environment;
    }

    @PostConstruct
    public void validateConfiguration() {
        if (authProperties.getNotifications().getMail().isEnabled()) {
            String mailHost = environment.getProperty("spring.mail.host");
            if (mailHost == null || mailHost.isBlank()) {
                log.warn("\n\n*** ADVERTENCIA DE CONFIGURACIÓN ***\nHas habilitado 'ctlabs.auth.notifications.mail.enabled=true', pero no se detectó configuración para 'spring.mail.host'.\nEs probable que el envío de correos falle. Por favor configura las propiedades de spring.mail en tu application.properties.\n");
            }
        }
    }

    @Async
    public void sendVerificationEmail(String to, String name, String code) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            Context context = new Context();
            context.setVariable("name", name);
            String confirmationUrl = UriComponentsBuilder.fromUriString(authProperties.getFrontendUrl())
                    .path("/verify-email")
                    .queryParam("code", code)
                    .queryParam("email", to)
                    .toUriString();
            context.setVariable("confirmationUrl", confirmationUrl);

            String htmlContent = templateEngine.process("confirmation", context);

            helper.setTo(to);
            helper.setFrom("noreply@distribol.com");
            helper.setSubject("Verifica tu correo electrónico");
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("Verification email sent to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}", to, e);
        }
    }

    @Async
    public void sendPasswordResetEmail(String to, String name, String code) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            Context context = new Context();
            context.setVariable("name", name);
            String resetUrl = UriComponentsBuilder.fromUriString(authProperties.getFrontendUrl())
                    .path("/reset-password")
                    .queryParam("code", code)
                    .queryParam("email", to)
                    .toUriString();
            context.setVariable("resetUrl", resetUrl);

            String htmlContent = templateEngine.process("reset-password", context);

            helper.setTo(to);
            helper.setFrom("noreply@distribol.com");
            helper.setSubject("Restablecer contraseña");
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("Password reset email sent to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send password reset email to {}", to, e);
        }
    }
}