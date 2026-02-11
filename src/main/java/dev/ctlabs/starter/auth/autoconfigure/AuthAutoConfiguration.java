package dev.ctlabs.starter.auth.autoconfigure;

import dev.ctlabs.starter.auth.domain.model.Profile;
import dev.ctlabs.starter.auth.domain.model.Role;
import dev.ctlabs.starter.auth.domain.model.User;
import dev.ctlabs.starter.auth.domain.repository.RoleRepository;
import dev.ctlabs.starter.auth.domain.repository.UserRepository;
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

@AutoConfiguration(after = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@EnableConfigurationProperties(AuthProperties.class)
@ComponentScan(basePackages = "dev.ctlabs.starter.auth")
@EntityScan(basePackages = "dev.ctlabs.starter.auth.domain.model")
@EnableJpaRepositories(basePackages = "dev.ctlabs.starter.auth.domain.repository")
@EnableJpaAuditing
public class AuthAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AuthAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @ConditionalOnProperty(prefix = "ctlabs.auth.admin", name = "enabled", havingValue = "true")
    public CommandLineRunner createInitialAdmin(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, AuthProperties authProperties) {
        return args -> {
            AuthProperties.Admin adminProps = authProperties.getAdmin();

            if (adminProps.getEmail() == null || adminProps.getPassword() == null) {
                log.warn("Admin user creation enabled but email or password not provided in properties.");
                return;
            }

            if (userRepository.findByEmail(adminProps.getEmail()).isEmpty()) {
                log.info("Creating initial admin user: {}", adminProps.getEmail());
                User admin = new User();
                admin.setEmail(adminProps.getEmail());
                admin.setPassword(passwordEncoder.encode(adminProps.getPassword()));
                admin.setEmailVerified(true);
                admin.setPhoneVerified(true);
                admin.setStatus("active");

                Profile profile = new Profile();
                profile.setFirstName(adminProps.getFirstName());
                profile.setLastName(adminProps.getLastName());
                profile.setUser(admin);
                admin.setProfile(profile);

                String roleName = adminProps.getRole();
                Role role = roleRepository.findByName(roleName)
                        .orElseGet(() -> {
                            Role newRole = new Role();
                            newRole.setName(roleName);
                            newRole.setDescription("Administrator role");
                            return roleRepository.save(newRole);
                        });
                admin.getRoles().add(role);

                userRepository.save(admin);
                log.info("Initial admin user created successfully.");
            }
        };
    }
}
