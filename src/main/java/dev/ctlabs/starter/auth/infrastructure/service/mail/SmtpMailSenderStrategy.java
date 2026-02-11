package dev.ctlabs.starter.auth.infrastructure.service.mail;

import dev.ctlabs.starter.auth.autoconfigure.AuthProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Slf4j
public class SmtpMailSenderStrategy implements MailSenderStrategy {

    private final JavaMailSender javaMailSender;
    private final AuthProperties authProperties;
    private final TemplateEngine templateEngine;

    public SmtpMailSenderStrategy(JavaMailSender javaMailSender,
                                  AuthProperties authProperties,
                                  TemplateEngine templateEngine) {
        this.javaMailSender = javaMailSender;
        this.authProperties = authProperties;
        this.templateEngine = templateEngine;
    }

    @Override
    public void send(String name, String to, EmailType type, String subject, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);

        String htmlContent = templateEngine.process(type.getTemplateName(), context);

        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            helper.setTo(to);
            helper.setFrom(authProperties.getNotifications().getMail().getFromEmail());
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            javaMailSender.send(mimeMessage);
            log.info("Email sent via SMTP to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email via SMTP to {}", to, e);
        }
    }
}