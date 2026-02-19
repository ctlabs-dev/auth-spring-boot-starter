package dev.ctlabs.starter.auth.autoconfigure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Configuration properties for the Auth Starter.
 * <p>
 * Supports configuration for JWT, database migration, notifications (mail/phone),
 * verification settings, and admin user creation.
 */
@ConfigurationProperties(prefix = "ctlabs.auth")
@Getter
@Setter
public class AuthProperties {

    /**
     * General application settings (URLs, etc).
     */
    private Application application = new Application();

    /**
     * Default role assigned to new users upon registration. Default: "USER".
     */
    private String defaultRole = "USER";

    /**
     * List of public paths that do not require authentication.
     * Ant-style patterns are supported (e.g. "/api/public/**", "/webhook/**").
     */
    private List<String> publicPaths = new ArrayList<>();

    /**
     * JWT (JSON Web Token) configuration.
     */
    private Jwt jwt = new Jwt();

    /**
     * Refresh token configuration.
     */
    private RefreshToken refreshToken = new RefreshToken();

    /**
     * Notification settings (Email, SMS, WhatsApp).
     */
    private Notifications notifications = new Notifications();

    /**
     * Database configuration.
     */
    private Db db = new Db();

    /**
     * Admin user configuration.
     */
    private Admin admin = new Admin();

    /**
     * Verification settings (Email/Phone codes).
     */
    private Verification verification = new Verification();

    /**
     * Password validation settings.
     */
    private Password password = new Password();

    /**
     * General application configuration.
     */
    @Getter
    @Setter
    public static class Application {
        /**
         * Base URL for authentication endpoints. Default: "/api/auth".
         */
        private String baseUrl = "/api/auth";
        /**
         * URL of the frontend application. Used for CORS and email links. Default: "http://localhost:3000".
         */
        private String frontendUrl = "http://localhost:3000";
    }

    /**
     * Configuration for JWT access tokens.
     */
    @Getter
    @Setter
    public static class Jwt {
        /**
         * Secret key used for signing JWTs. Must be at least 256 bits (32 bytes) for HS256.
         */
        private String secretKey;
        /**
         * Expiration time for JWT access tokens. Use ISO-8601 format (e.g. PT1H for 1 hour, P1D for 1 day).
         * Simple format like "24h" or "90m" is also supported. Default: "24h".
         */
        private Duration expiration = Duration.ofMinutes(15);
    }

    /**
     * Configuration for Refresh tokens.
     */
    @Getter
    @Setter
    public static class RefreshToken {
        /**
         * Expiration time for refresh tokens. Use ISO-8601 format (e.g. P30D for 30 days).
         * Simple format like "30d" is also supported. Default: "30d".
         */
        private Duration expiration = Duration.ofDays(30);
    }

    /**
     * Configuration for notifications.
     */
    @Getter
    @Setter
    public static class Notifications {
        /**
         * Email notification settings.
         */
        private Mail mail = new Mail();
        /**
         * Phone notification settings.
         */
        private Phone phone = new Phone();

        /**
         * Email settings.
         */
        @Getter
        @Setter
        public static class Mail {
            /**
             * Selects the mail provider. Default is NONE.
             */
            private Provider provider = Provider.NONE;
            /**
             * SMTP configuration. Used when provider is SMTP.
             */
            private Smtp smtp = new Smtp();
            /**
             * Brevo configuration. Used when provider is BREVO.
             */
            private Brevo brevo = new Brevo();

            /**
             * Supported email providers.
             */
            public enum Provider {
                /** No email provider. Emails will not be sent. */
                NONE,
                /** SMTP provider (e.g. Gmail, Outlook, custom SMTP). */
                SMTP,
                /** Brevo (formerly Sendinblue) API provider. */
                BREVO
            }

            /**
             * SMTP specific settings.
             */
            @Getter
            @Setter
            public static class Smtp {
                /**
                 * Email address to send emails from.
                 */
                private String fromEmail;

                /**
                 * Name to display as the sender.
                 */
                private String fromName;
            }

            /**
             * Brevo specific settings.
             */
            @Getter
            @Setter
            public static class Brevo {
                /**
                 * Brevo API Key (xkeysib-...)
                 */
                private String apiKey;

                /**
                 * Brevo API Base URL. Default: "https://api.brevo.com/v3".
                 */
                private String baseUrl = "https://api.brevo.com/v3";
                /**
                 * Template ID for email verification.
                 */
                private Integer verificationTemplateId;
                /**
                 * Template ID for password reset.
                 */
                private Integer passwordResetTemplateId;
            }
        }

        /**
         * Phone settings.
         */
        @Getter
        @Setter
        public static class Phone {
            /**
             * Selects the phone provider. Default is NONE.
             */
            private Provider provider = Provider.NONE;
            /**
             * Selects the communication channel. Default is SMS.
             */
            private Channel channel = Channel.SMS;
            /**
             * Generic 'from' number (e.g. +1234567890 or whatsapp:+123...).
             */
            private String fromPhoneNumber;
            /**
             * Twilio configuration. Used when provider is TWILIO.
             */
            private Twilio twilio = new Twilio();
            /**
             * Brevo configuration. Used when provider is BREVO.
             */
            private Brevo brevo = new Brevo();

            /**
             * Supported phone providers.
             */
            public enum Provider {
                /** No phone provider. Messages will not be sent. */
                NONE,
                /** Twilio API provider. */
                TWILIO,
                /** Brevo API provider. */
                BREVO
            }
            /**
             * Supported phone channels.
             */
            public enum Channel {
                /** SMS channel. */
                SMS,
                /** WhatsApp channel. */
                WHATSAPP
            }

            /**
             * Twilio specific settings.
             */
            @Getter
            @Setter
            public static class Twilio {
                /**
                 * Twilio Account SID.
                 */
                private String accountSid;
                /**
                 * Twilio Auth Token.
                 */
                private String authToken;
                /**
                 * Twilio API Base URL. Default: "https://api.twilio.com".
                 */
                private String baseUrl = "https://api.twilio.com";
            }

            /**
             * Brevo specific settings.
             */
            @Getter
            @Setter
            public static class Brevo {
                /**
                 * Brevo API Key.
                 */
                private String apiKey;
                /**
                 * Brevo API Base URL. Default: "https://api.brevo.com/v3".
                 */
                private String baseUrl = "https://api.brevo.com/v3";
                /**
                 * Sender name for SMS. Max 11 alphanumeric chars. Default: "AuthService".
                 */
                private String senderName = "AuthService";
            }
        }
    }

    /**
     * Database settings.
     */
    @Getter
    @Setter
    public static class Db {
        /**
         * Enables automatic migration of starter tables (users, verification). Default: true.
         */
        private boolean migrationEnabled = true;
    }

    /**
     * Admin user settings.
     */
    @Getter
    @Setter
    public static class Admin {
        /**
         * If true, attempts to create an admin user at startup if it doesn't exist. Default: false.
         */
        private boolean enabled = false;

        /**
         * Admin email address.
         */
        private String email;
        /**
         * Admin password.
         */
        private String password;
        /**
         * Admin first name. Default: "Admin".
         */
        private String firstName = "Admin";
        /**
         * Admin last name. Default: "System".
         */
        private String lastName = "System";
        /**
         * Admin role name. Default: "ADMIN".
         */
        private String role = "ADMIN";

        /**
         * List of permissions to assign to the admin role upon creation.
         * Useful if the application uses authority-based security (e.g. hasAuthority('users:write')).
         */
        private Set<String> permissions = new HashSet<>();
    }

    /**
     * Verification settings.
     */
    @Getter
    @Setter
    public static class Verification {
        /**
         * Expiration time for email verification links. Default: 24h.
         */
        private Duration emailLinkExpiration = Duration.ofDays(1);

        /**
         * Expiration time for phone verification codes. Default: 10m.
         */
        private Duration phoneCodeExpiration = Duration.ofMinutes(10);

        /**
         * Label for minutes unit (e.g. "minutes", "minutos"). Default: "minutes".
         */
        private String unitMinutes = "minutes";

        /**
         * Label for hours unit (e.g. "hours", "horas"). Default: "hours".
         */
        private String unitHours = "hours";
    }

    /**
     * Password validation settings.
     */
    @Getter
    @Setter
    public static class Password {
        /**
         * Regex for password validation. Default enforces 8-20 chars, 1 digit, 1 lower, 1 upper.
         */
        private String validationRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=\\S+$).{8,20}$";
        /**
         * Validation message to be shown when the password does not meet the regex requirements.
         */
        private String validationMessage = """
                Password must be 8-20 characters long, contain at least one digit, \
                one lowercase, one uppercase letter and no whitespace""";
    }
}
