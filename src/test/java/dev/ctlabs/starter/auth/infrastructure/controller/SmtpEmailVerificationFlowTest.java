package dev.ctlabs.starter.auth.infrastructure.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import dev.ctlabs.starter.auth.application.dto.LoginRequest;
import dev.ctlabs.starter.auth.application.dto.RegisterRequest;
import dev.ctlabs.starter.auth.application.dto.VerifyEmailRequest;
import dev.ctlabs.starter.auth.domain.repository.UserRepository;
import dev.ctlabs.starter.auth.domain.repository.VerificationCodeRepository;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest");

    private static GreenMail greenMail;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationCodeRepository verificationCodeRepository;

    @BeforeAll
    static void startServer() {
        greenMail = new GreenMail(ServerSetupTest.SMTP);
        greenMail.start();
    }

    @AfterAll
    static void stopServer() {
        greenMail.stop();
    }

    @BeforeEach
    void setUp() {
        greenMail.reset();
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
            assertThat(receivedMessages).isNotEmpty();
            assertThat(receivedMessages)
                    .extracting(msg -> msg.getAllRecipients()[0].toString())
                    .contains("smtp@test.com");
            assertThat(receivedMessages[receivedMessages.length - 1].getSubject()).isEqualTo("Verify your email");
        });
    }

    @Test
    void loginShouldFailWhenEmailNotVerified() throws Exception {
        var registerRequest = new RegisterRequest(
                "Smtp", "Login", "smtp_login@test.com", null, "Password123!"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        var loginRequest = new LoginRequest(
                "smtp_login@test.com",
                "Password123!"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
            assertThat(receivedMessages)
                    .extracting(msg -> msg.getAllRecipients()[0].toString())
                    .contains("smtp_login@test.com");
        });
    }

    @Test
    void verifyEmailShouldWorkWithValidCode() throws Exception {
        var registerRequest = new RegisterRequest(
                "Verify", "User", "verify@test.com", null, "Password123!"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
            assertThat(receivedMessages)
                    .extracting(msg -> msg.getAllRecipients()[0].toString())
                    .contains("verify@test.com");
        });

        var user = userRepository.findByEmail("verify@test.com").orElseThrow();
        assertThat(user.isEmailVerified()).isFalse();

        var loginRequest = new LoginRequest("verify@test.com", "Password123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());

        var codeEntity = verificationCodeRepository.findAll().stream()
                .filter(vc -> vc.getUser().getId().equals(user.getId()) && "EMAIL_VERIFICATION".equals(vc.getType()))
                .findFirst()
                .orElseThrow();

        var verifyRequest = new VerifyEmailRequest("verify@test.com", codeEntity.getCode());

        mockMvc.perform(post("/api/auth/email-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk());

        var updatedUser = userRepository.findByEmail("verify@test.com").orElseThrow();
        assertThat(updatedUser.isEmailVerified()).isTrue();
        assertThat(verificationCodeRepository.findById(codeEntity.getId())).isEmpty();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void verifyEmailShouldFailWhenCodeIsInvalid() throws Exception {
        var registerRequest = new RegisterRequest("Invalid", "Code", "invalid_code@test.com", null, "Password123!");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
            assertThat(receivedMessages)
                    .extracting(msg -> msg.getAllRecipients()[0].toString())
                    .contains("invalid_code@test.com");
        });

        var verifyRequest = new VerifyEmailRequest("invalid_code@test.com", "WRONG-CODE");

        mockMvc.perform(post("/api/auth/email-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyEmailShouldFailWhenCodeIsExpired() throws Exception {
        var registerRequest = new RegisterRequest("Expired", "Code", "expired_code@test.com", null, "Password123!");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
            assertThat(receivedMessages)
                    .extracting(msg -> msg.getAllRecipients()[0].toString())
                    .contains("expired_code@test.com");
        });

        var user = userRepository.findByEmail("expired_code@test.com").orElseThrow();
        var codeEntity = verificationCodeRepository.findAll().stream()
                .filter(vc -> vc.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow();

        codeEntity.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        verificationCodeRepository.save(codeEntity);

        var verifyRequest = new VerifyEmailRequest("expired_code@test.com", codeEntity.getCode());

        mockMvc.perform(post("/api/auth/email-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyEmailShouldFailWhenUserDoesNotExist() throws Exception {
        var verifyRequest = new VerifyEmailRequest("nonexistent@test.com", "some-code");

        mockMvc.perform(post("/api/auth/email-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyEmailShouldReturnMessageWhenAlreadyVerified() throws Exception {
        var registerRequest = new RegisterRequest("Already", "Verified", "already@test.com", null, "Password123!");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
            assertThat(receivedMessages)
                    .extracting(msg -> msg.getAllRecipients()[0].toString())
                    .contains("already@test.com");
        });

        var user = userRepository.findByEmail("already@test.com").orElseThrow();
        var codeEntity = verificationCodeRepository.findAll().stream()
                .filter(vc -> vc.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow();

        var verifyRequest = new VerifyEmailRequest("already@test.com", codeEntity.getCode());
        mockMvc.perform(post("/api/auth/email-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/email-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("Email is already verified."));
    }
}