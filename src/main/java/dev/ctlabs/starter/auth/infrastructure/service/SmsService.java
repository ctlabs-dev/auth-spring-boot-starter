package dev.ctlabs.starter.auth.infrastructure.service;

import dev.ctlabs.starter.auth.autoconfigure.AuthProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class SmsService {

    private static final String WHATSAPP_PREFIX = "whatsapp:";
    private static final String VERIFICATION_TEMPLATE_TEXT = "Tu código de verificación Distribol es: %s";
    private static final String RESET_TEMPLATE_TEXT = "Tu código para restablecer tu contraseña de Distribol es: %s";

    private final String accountSid;
    private final String authToken;
    private final String fromPhoneNumber;
    private final String fromWhatsappNumber;
    private final String whatsappContentSid;
    private final ObjectMapper objectMapper;
    private final AuthProperties authProperties;

    public SmsService(AuthProperties authProperties, ObjectMapper objectMapper) {
        this.authProperties = authProperties;
        this.accountSid = authProperties.getTwilio().getAccountSid();
        this.authToken = authProperties.getTwilio().getAuthToken();
        this.fromPhoneNumber = authProperties.getNotifications().getSms().getPhoneNumber();
        this.fromWhatsappNumber = authProperties.getNotifications().getWhatsapp().getPhoneNumber();
        this.whatsappContentSid = authProperties.getTwilio().getWhatsappContentSid();
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        validateConfiguration();

        // Solo inicializamos Twilio si existen las credenciales para evitar errores en runtime
        if (accountSid != null && !accountSid.isBlank() && authToken != null && !authToken.isBlank()) {
            try {
                Twilio.init(accountSid, authToken);
                log.info("Twilio initialized successfully.");
            } catch (Exception e) {
                log.error("Failed to initialize Twilio: {}", e.getMessage());
            }
        }
    }

    private void validateConfiguration() {
        boolean smsEnabled = authProperties.getNotifications().getSms().isEnabled();
        boolean whatsappEnabled = authProperties.getNotifications().getWhatsapp().isEnabled();

        if (smsEnabled || whatsappEnabled) {
            if (accountSid == null || accountSid.isBlank() || authToken == null || authToken.isBlank()) {
                log.warn("\n\n*** ADVERTENCIA DE CONFIGURACIÓN ***\nHas habilitado SMS o WhatsApp en 'ctlabs.auth.notifications.*', pero no se detectaron credenciales de Twilio (account-sid, auth-token).\nEs probable que el envío de mensajes falle. Por favor configura 'ctlabs.auth.twilio.*' en tu application.properties.\n");
            }
        }

        if (smsEnabled && (fromPhoneNumber == null || fromPhoneNumber.isBlank())) {
            log.warn("\n\n*** ADVERTENCIA DE CONFIGURACIÓN ***\nHas habilitado SMS, pero falta configurar 'ctlabs.auth.notifications.sms.phone-number'.\n");
        }

        if (whatsappEnabled && (fromWhatsappNumber == null || fromWhatsappNumber.isBlank())) {
            log.warn("\n\n*** ADVERTENCIA DE CONFIGURACIÓN ***\nHas habilitado WhatsApp, pero falta configurar 'ctlabs.auth.notifications.whatsapp.phone-number'.\n");
        }
    }

    @Async
    public void sendVerificationCode(String toPhoneNumber, String code, boolean whatsappEnabled, boolean smsEnabled) {
        boolean whatsappSent = false;

        if (whatsappEnabled && whatsappContentSid != null && !whatsappContentSid.isBlank()) {
            try {
                sendWhatsAppTemplate(toPhoneNumber, Map.of("1", code));
                whatsappSent = true;
            } catch (Exception e) {
                log.error("Failed to send WhatsApp Template to {}. Falling back to SMS if enabled. Error: {}", toPhoneNumber, e.getMessage());
            }
        } else if (whatsappEnabled) {
            log.warn("WhatsApp enabled but no ContentSid configured. Skipping WhatsApp for {}", toPhoneNumber);
        }

        if (!whatsappSent && smsEnabled) {
            try {
                String smsBody = VERIFICATION_TEMPLATE_TEXT.formatted(code);
                sendSms(toPhoneNumber, smsBody);
            } catch (Exception e) {
                log.error("Failed to send SMS to {}. Error: {}", toPhoneNumber, e.getMessage());
            }
        }
    }

    @Async
    public void sendPasswordResetCode(String toPhoneNumber, String code, boolean whatsappEnabled, boolean smsEnabled) {
        boolean whatsappSent = false;

        if (whatsappEnabled && whatsappContentSid != null && !whatsappContentSid.isBlank()) {
            try {
                sendWhatsAppTemplate(toPhoneNumber, Map.of("1", code));
                whatsappSent = true;
            } catch (Exception e) {
                log.error("Failed to send WhatsApp Template to {}. Falling back to SMS if enabled. Error: {}", toPhoneNumber, e.getMessage());
            }
        } else if (whatsappEnabled) {
            log.warn("WhatsApp enabled but no ContentSid configured. Skipping WhatsApp for {}", toPhoneNumber);
        }

        if (!whatsappSent && smsEnabled) {
            try {
                String smsBody = RESET_TEMPLATE_TEXT.formatted(code);
                sendSms(toPhoneNumber, smsBody);
            } catch (Exception e) {
                log.error("Failed to send SMS to {}. Error: {}", toPhoneNumber, e.getMessage());
            }
        }
    }

    private void sendWhatsAppTemplate(String toPhoneNumber, Map<String, String> variables) {
        try {
            String contentVariables = objectMapper.writeValueAsString(variables);
            Message message = Message.creator(
                            new PhoneNumber(WHATSAPP_PREFIX + toPhoneNumber),
                            new PhoneNumber(fromWhatsappNumber),
                            ""
                    )
                    .setContentSid(whatsappContentSid)
                    .setContentVariables(contentVariables)
                    .create();
            log.info("WhatsApp template sent to {}. SID: {}", toPhoneNumber, message.getSid());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize WhatsApp variables", e);
        }
    }

    private void sendSms(String toPhoneNumber, String messageBody) {
        Message message = Message.creator(
                new PhoneNumber(toPhoneNumber),
                new PhoneNumber(fromPhoneNumber),
                messageBody
        ).create();
        log.info("SMS sent to {}. SID: {}", toPhoneNumber, message.getSid());
    }
}
