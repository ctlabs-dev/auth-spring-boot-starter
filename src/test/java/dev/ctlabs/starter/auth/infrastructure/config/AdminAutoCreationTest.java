package dev.ctlabs.starter.auth.infrastructure.config;

import dev.ctlabs.starter.auth.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "ctlabs.auth.admin.enabled=true",
        "ctlabs.auth.admin.email=superadmin@ctlabs.dev",
        "ctlabs.auth.admin.password=SuperSecret123!",
        "ctlabs.auth.admin.role=ROLE_SUPER_ADMIN"
})
@Transactional
@Testcontainers
class AdminAutoCreationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest");

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldCreateAdminUserOnStartup() {
        var adminOptional = userRepository.findByEmail("superadmin@ctlabs.dev");

        assertThat(adminOptional).isPresent();
        var admin = adminOptional.get();

        assertThat(admin.getRoles())
                .extracting("name")
                .contains("ROLE_SUPER_ADMIN");

        assertThat(admin.getStatus()).isEqualTo("active");
    }
}