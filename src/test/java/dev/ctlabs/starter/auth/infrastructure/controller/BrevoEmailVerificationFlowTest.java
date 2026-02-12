package dev.ctlabs.starter.auth.infrastructure.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import dev.ctlabs.starter.auth.application.dto.LoginRequest;
import dev.ctlabs.starter.auth.application.dto.RegisterRequest;
import dev.ctlabs.starter.auth.application.dto.VerifyEmailRequest;
import dev.ctlabs.starter.auth.domain.repository.UserRepository;
import dev.ctlabs.starter.auth.domain.repository.VerificationCodeRepository;
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

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "ctlabs.auth.notifications.mail.provider=BREVO",
        "ctlabs.auth.notifications.mail.brevo.api-key=dummy-api-key",
        "ctlabs.auth.notifications.mail.brevo.verification-template-id=1",
        "ctlabs.auth.notifications.mail.brevo.password-reset-template-id=2",
        "ctlabs.auth.notifications.mail.brevo.base-url=http://localhost:8089"
})
@AutoConfigureMockMvc
@Transactional
@Testcontainers
class BrevoEmailVerificationFlowTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest");

    private static WireMockServer wireMockServer;

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
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);
    }

    @AfterAll
    static void stopServer() {
        wireMockServer.stop();
    }

    @BeforeEach
    void reset() {
        wireMockServer.resetAll();
        stubFor(WireMock.post(urlEqualTo("/smtp/email"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"messageId\":\"<dummy>\"}")));
    }

    @Test
    void registerShouldGenerateCodeWhenProviderIsBrevo() throws Exception {
        var request = new RegisterRequest(
                "Brevo", "User", "brevo@test.com", null, "Password123!"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        var user = userRepository.findByEmail("brevo@test.com").orElseThrow();

        assertThat(user.isEmailVerified()).isFalse();

        boolean hasCode = verificationCodeRepository.findAll().stream()
                .anyMatch(vc -> vc.getUser().getId().equals(user.getId()) && "EMAIL_VERIFICATION".equals(vc.getType()));

        assertThat(hasCode).isTrue();

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            verify(postRequestedFor(urlEqualTo("/smtp/email"))
                    .withHeader("api-key", equalTo("dummy-api-key"))
                    .withRequestBody(containing("brevo@test.com"))
                    .withRequestBody(containing("\"templateId\":1")));
        });
    }

    @Test
    void loginShouldFailWhenEmailNotVerified() throws Exception {
        var registerRequest = new RegisterRequest(
                "Brevo", "Login", "brevo_login@test.com", null, "Password123!"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> verify(postRequestedFor(urlEqualTo("/smtp/email"))));

        var loginRequest = new LoginRequest("brevo_login@test.com", "Password123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void verifyEmailShouldWorkWithValidCode() throws Exception {
        var registerRequest = new RegisterRequest(
                "Verify", "Brevo", "verify_brevo@test.com", null, "Password123!"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            verify(postRequestedFor(urlEqualTo("/smtp/email")));
        });

        var user = userRepository.findByEmail("verify_brevo@test.com").orElseThrow();
        assertThat(user.isEmailVerified()).isFalse();

        var codeEntity = verificationCodeRepository.findAll().stream()
                .filter(vc -> vc.getUser().getId().equals(user.getId()) && "EMAIL_VERIFICATION".equals(vc.getType()))
                .findFirst()
                .orElseThrow();

        var verifyRequest = new VerifyEmailRequest("verify_brevo@test.com", codeEntity.getCode());

        mockMvc.perform(post("/api/auth/email-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk());

        var loginRequest = new LoginRequest("verify_brevo@test.com", "Password123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void verifyEmailShouldFailWhenCodeIsInvalid() throws Exception {
        var registerRequest = new RegisterRequest("Invalid", "Code", "invalid_brevo@test.com", null, "Password123!");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> verify(postRequestedFor(urlEqualTo("/smtp/email"))));

        var verifyRequest = new VerifyEmailRequest("invalid_brevo@test.com", "WRONG-CODE");

        mockMvc.perform(post("/api/auth/email-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyEmailShouldFailWhenCodeIsExpired() throws Exception {
        var registerRequest = new RegisterRequest("Expired", "Code", "expired_brevo@test.com", null, "Password123!");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> verify(postRequestedFor(urlEqualTo("/smtp/email"))));

        var user = userRepository.findByEmail("expired_brevo@test.com").orElseThrow();
        var codeEntity = verificationCodeRepository.findAll().stream()
                .filter(vc -> vc.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow();

        codeEntity.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        verificationCodeRepository.save(codeEntity);

        var verifyRequest = new VerifyEmailRequest("expired_brevo@test.com", codeEntity.getCode());

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
        var registerRequest = new RegisterRequest("Already", "Verified", "already_brevo@test.com", null, "Password123!");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            verify(postRequestedFor(urlEqualTo("/smtp/email"))
                    .withRequestBody(containing("already_brevo@test.com")));
        });

        var user = userRepository.findByEmail("already_brevo@test.com").orElseThrow();
        var codeEntity = verificationCodeRepository.findAll().stream()
                .filter(vc -> vc.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow();

        var verifyRequest = new VerifyEmailRequest("already_brevo@test.com", codeEntity.getCode());
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