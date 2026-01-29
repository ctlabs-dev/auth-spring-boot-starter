package dev.ctlabs.starter.auth.autoconfigure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ctlabs.auth")
public class AuthProperties {

    private String baseUrl = "/api/auth";
    private String frontendUrl = "http://localhost:3000";
    private String defaultRole = "ROLE_CUSTOMER";
    private Jwt jwt = new Jwt();
    private Twilio twilio = new Twilio();
    private Notifications notifications = new Notifications();
    private Db db = new Db();
    private Admin admin = new Admin();

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
            /**
             * Enables email sending. Requires spring.mail.*
             */
            private boolean enabled = false;
        }

        @Getter
        @Setter
        public static class Sms {
            /**
             * Enables SMS sending. Requires Twilio and phone-number.
             */
            private boolean enabled = false;
            private String phoneNumber;
        }

        @Getter
        @Setter
        public static class Whatsapp {
            /**
             * Enables WhatsApp sending. Requires Twilio and whatsapp-number.
             */
            private boolean enabled = false;
            private String phoneNumber;
        }
    }

    @Getter
    @Setter
    public static class Db {
        /**
         * Enables automatic migration of starter tables (users, verification).
         */
        private boolean migrationEnabled = true;
    }

    @Getter
    @Setter
    public static class Admin {
        /**
         * If true, attempts to create an admin user at startup if it doesn't exist.
         */
        private boolean enabled = false;
        private String email;
        private String password;
        private String firstName = "Admin";
        private String lastName = "System";
        private String role = "ROLE_ADMIN";
    }
}
