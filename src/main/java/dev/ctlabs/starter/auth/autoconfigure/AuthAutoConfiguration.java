package dev.ctlabs.starter.auth.autoconfigure;

import dev.ctlabs.starter.auth.application.service.UserManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Auto-configuration for the Auth Starter.
 * Configures JPA, security, and initial administrative user.
 */
@AutoConfiguration(after = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@EnableConfigurationProperties(AuthProperties.class)
@ComponentScan(basePackages = "dev.ctlabs.starter.auth")
@EntityScan(basePackages = "dev.ctlabs.starter.auth.domain.model")
@EnableJpaRepositories(basePackages = "dev.ctlabs.starter.auth.domain.repository")
@EnableJpaAuditing
public class AuthAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AuthAutoConfiguration.class);

    /**
     * Bean that provides a password encoder.
     *
     * @return A BCryptPasswordEncoder instance.
     */
    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Creates an initial admin user if configured.
     *
     * @param userManagementService The service to manage users.
     * @param authProperties        The authentication properties.
     * @return A CommandLineRunner that creates the admin user.
     */
    @Bean
    @ConditionalOnProperty(prefix = "ctlabs.auth.admin", name = "enabled", havingValue = "true")
    public CommandLineRunner createInitialAdmin(
            UserManagementService userManagementService, AuthProperties authProperties) {
        return args -> {
            AuthProperties.Admin adminProps = authProperties.getAdmin();
            if (adminProps.getEmail() == null || adminProps.getPassword() == null) {
                log.warn("Admin user creation enabled but email or password not provided in properties.");
                return;
            }
            userManagementService.createAdminUserIfNotExists(adminProps);
        };
    }
}
