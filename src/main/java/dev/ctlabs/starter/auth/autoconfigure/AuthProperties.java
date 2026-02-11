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
    private String defaultRole = "ROLE_USER";
    private Jwt jwt = new Jwt();
    private Notifications notifications = new Notifications();
    private Db db = new Db();
    private Admin admin = new Admin();
    private Verification verification = new Verification();
    private Password password = new Password();

    @Getter
    @Setter
    public static class Jwt {
        private String secretKey;
        private long expiration = 86400000;
    }

    @Getter
    @Setter
    public static class Notifications {
        private Mail mail = new Mail();
        private Phone phone = new Phone();

        @Getter
        @Setter
        public static class Mail {
            public enum Provider {
                NONE,
                SMTP,
                BREVO,
                MAILCHIMP
            }

            /**
             * Selects the mail provider. Default is NONE.
             */
            private Provider provider = Provider.NONE;

            private String fromEmail = "noreply@distribol.com";
            private String fromName = "Auth Service";

            private Brevo brevo = new Brevo();

            @Getter
            @Setter
            public static class Brevo {
                /**
                 * Brevo API Key (xkeysib-...)
                 */
                private String apiKey;
                private String baseUrl = "https://api.brevo.com/v3";
                private Integer verificationTemplateId;
                private Integer passwordResetTemplateId;
            }
        }

        @Getter
        @Setter
        public static class Phone {
            public enum Provider {
                NONE,
                TWILIO,
                BREVO
            }

            public enum Channel {
                SMS,
                WHATSAPP
            }

            /**
             * Selects the phone provider. Default is NONE.
             */
            private Provider provider = Provider.NONE;
            private Channel channel = Channel.SMS;
            private String fromPhoneNumber; // Generic 'from' number (e.g. +1234567890 or whatsapp:+123...)

            private Twilio twilio = new Twilio();
            private Brevo brevo = new Brevo();

            @Getter
            @Setter
            public static class Twilio {
                private String accountSid;
                private String authToken;
                private String baseUrl = "https://api.twilio.com";
            }

            @Getter
            @Setter
            public static class Brevo {
                private String apiKey;
                private String baseUrl = "https://api.brevo.com/v3";
                private String senderName = "AuthService"; // Max 11 alphanumeric chars for SMS
            }
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

    @Getter
    @Setter
    public static class Verification {
        /**
         * Expiration time for email verification links in minutes. Default: 1440 (24 hours).
         */
        private long emailLinkExpirationMinutes = 1440;

        /**
         * Expiration time for phone verification codes in minutes. Default: 10.
         */
        private long phoneCodeExpirationMinutes = 10;

        /**
         * Label for minutes unit (e.g. "minutes", "minutos"). Default: "minutes".
         */
        private String unitMinutes = "minutes";

        /**
         * Label for hours unit (e.g. "hours", "horas"). Default: "hours".
         */
        private String unitHours = "hours";
    }

    @Getter
    @Setter
    public static class Password {
        /**
         * Regex for password validation.
         */
        private String validationRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=\\S+$).{8,20}$";
        private String validationMessage = "Password must be 8-20 characters long, contain at least one digit, one lowercase, one uppercase letter and no whitespace";
    }
}
