package dev.ctlabs.starter.auth.infrastructure.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import dev.ctlabs.starter.auth.application.dto.RegisterRequest;
import dev.ctlabs.starter.auth.domain.repository.UserRepository;
import dev.ctlabs.starter.auth.domain.repository.VerificationCodeRepository;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "ctlabs.auth.notifications.mail.provider=SMTP",
        "spring.mail.host=localhost",
        "spring.mail.port=3025",
        "spring.mail.username=test",
        "spring.mail.password=test",
        "spring.mail.properties.mail.smtp.auth=true",
        "spring.mail.properties.mail.smtp.starttls.enable=false",
        "spring.mail.properties.mail.smtp.starttls.required=false"
})
@AutoConfigureMockMvc
@Transactional
@Testcontainers
class SmtpEmailVerificationFlowTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg18");

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationCodeRepository verificationCodeRepository;

    @BeforeEach
    void setUp() {
        greenMail.setUser("test", "test");
    }

    @Test
    void registerShouldGenerateCodeWhenProviderIsSmtp() throws Exception {
        var request = new RegisterRequest(
                "Smtp", "User", "smtp@test.com", null, "Password123!"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        var user = userRepository.findByEmail("smtp@test.com").orElseThrow();

        assertThat(user.isEmailVerified()).isFalse();

        boolean hasCode = verificationCodeRepository.findAll().stream()
                .anyMatch(vc -> vc.getUser().getId().equals(user.getId()) && "EMAIL_VERIFICATION".equals(vc.getType()));

        assertThat(hasCode).isTrue();

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
            assertThat(receivedMessages).hasSize(1);
            assertThat(receivedMessages[0].getSubject()).isEqualTo("Verify your email");
            assertThat(receivedMessages[0].getAllRecipients()[0].toString()).isEqualTo("smtp@test.com");
        });
    }
}