package dev.ctlabs.starter.auth.autoconfigure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ctlabs.auth")
public class AuthProperties {

    private String frontendUrl = "http://localhost:3000";
    private Jwt jwt = new Jwt();
    private Twilio twilio = new Twilio();
    private Notifications notifications = new Notifications();
    private Db db = new Db();

    @Getter
    @Setter
    public static class Jwt {
        private String secretKey;
        private long expiration = 86400000;
    }

    @Getter
    @Setter
    public static class Twilio {
        private String accountSid;
        private String authToken;
        private String whatsappContentSid;
    }

    @Getter
    @Setter
    public static class Notifications {
        private Mail mail = new Mail();
        private Sms sms = new Sms();
        private Whatsapp whatsapp = new Whatsapp();

        @Getter
        @Setter
        public static class Mail {
            /** Habilita el envío de correos. Requiere spring.mail.* */
            private boolean enabled = false;
        }

        @Getter
        @Setter
        public static class Sms {
            /** Habilita el envío de SMS. Requiere Twilio y phone-number. */
            private boolean enabled = false;
            private String phoneNumber;
        }

        @Getter
        @Setter
        public static class Whatsapp {
            /** Habilita el envío de WhatsApp. Requiere Twilio y whatsapp-number. */
            private boolean enabled = false;
            private String phoneNumber;
        }
    }

    @Getter
    @Setter
    public static class Db {
        /** Habilita la migración automática de las tablas del starter (users, verification). */
        private boolean migrationEnabled = true;
    }
}
