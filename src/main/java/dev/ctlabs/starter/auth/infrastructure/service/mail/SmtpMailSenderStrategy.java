package dev.ctlabs.starter.auth.infrastructure.service.mail;

import dev.ctlabs.starter.auth.autoconfigure.AuthProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

/**
 * Mail sender strategy implementation using SMTP.
 * Sends emails via JavaMailSender using Thymeleaf templates.
 */
@Slf4j
public class SmtpMailSenderStrategy implements MailSenderStrategy {

    private final JavaMailSender javaMailSender;
    private final AuthProperties authProperties;
    private final TemplateEngine templateEngine;
    private final Environment environment;

    public SmtpMailSenderStrategy(
            JavaMailSender javaMailSender, AuthProperties authProperties, TemplateEngine templateEngine, Environment environment) {
        this.javaMailSender = javaMailSender;
        this.authProperties = authProperties;
        this.templateEngine = templateEngine;
        this.environment = environment;
    }

    @Override
    public void send(String name, String to, EmailType type, String subject, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);

        String htmlContent = templateEngine.process(type.getTemplateName(), context);

        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            String fromEmail = authProperties.getNotifications().getMail().getSmtp().getFromEmail();
            String fromName = authProperties.getNotifications().getMail().getSmtp().getFromName();

            if (fromEmail == null || fromEmail.isBlank()) {
                String fallbackEmail = environment.getProperty("spring.mail.username");
                if (fallbackEmail == null || fallbackEmail.isBlank()) {
                    log.error("SMTP sender email (from-email) is not configured and spring.mail.username is not set. Email will not be sent.");
                    return;
                }
                fromEmail = fallbackEmail;
                log.warn("SMTP sender email (from-email) is not configured. Using spring.mail.username as fallback: {}", fromEmail);
            }

            helper.setTo(to);
            if (fromName != null && !fromName.isBlank()) {
                try {
                    helper.setFrom(fromEmail, fromName);
                } catch (java.io.UnsupportedEncodingException e) {
                    log.error("Invalid encoding for fromName in SMTP email: {}", fromName, e);
                    helper.setFrom(fromEmail);
                }
            } else {
                helper.setFrom(fromEmail);
            }
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            javaMailSender.send(mimeMessage);
            log.info("Email sent via SMTP to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email via SMTP to {}", to, e);
        }
    }
}
