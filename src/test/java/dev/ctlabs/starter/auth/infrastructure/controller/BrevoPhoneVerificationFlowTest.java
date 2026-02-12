package dev.ctlabs.starter.auth.infrastructure.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import dev.ctlabs.starter.auth.application.dto.LoginRequest;
import dev.ctlabs.starter.auth.application.dto.RegisterRequest;
import dev.ctlabs.starter.auth.application.dto.VerifyPhoneRequest;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.Duration;

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
        "ctlabs.auth.notifications.phone.provider=BREVO",
        "ctlabs.auth.notifications.phone.brevo.api-key=dummy-api-key",
        "ctlabs.auth.notifications.phone.brevo.base-url=http://localhost:8090"
})
@AutoConfigureMockMvc
@Transactional
@Testcontainers
class BrevoPhoneVerificationFlowTest {

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
        wireMockServer = new WireMockServer(8090);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8090);
    }

    @AfterAll
    static void stopServer() {
        wireMockServer.stop();
    }

    @BeforeEach
    void reset() {
        wireMockServer.resetAll();
        stubFor(WireMock.post(urlEqualTo("/transactionalSMS/sms"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withBody("{}")));
    }

    @Test
    void registerShouldGenerateCodeWhenPhoneProviderIsBrevo() throws Exception {
        var request = new RegisterRequest(
                "BrevoPhone", "User", null, "+15551234567", "Password123!"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        var user = userRepository.findByPhoneNumber("+15551234567").orElseThrow();

        assertThat(user.isPhoneVerified()).isFalse();

        boolean hasCode = verificationCodeRepository.findAll().stream()
                .anyMatch(vc -> vc.getUser().getId().equals(user.getId()) && "PHONE_VERIFICATION".equals(vc.getType()));

        assertThat(hasCode).isTrue();

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            verify(postRequestedFor(urlEqualTo("/transactionalSMS/sms"))
                    .withHeader("api-key", equalTo("dummy-api-key"))
                    .withRequestBody(containing("+15551234567"))
                    .withRequestBody(containing("Your verification code is")));
        });
    }

    @Test
    void loginShouldFailWhenPhoneNotVerified() throws Exception {
        var registerRequest = new RegisterRequest(
                "Brevo", "Login", null, "+15558887777", "Password123!"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        var loginRequest = new LoginRequest(
                "+15558887777",
                "Password123!"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void verifyPhoneShouldWorkWithValidCode() throws Exception {
        var registerRequest = new RegisterRequest("Verify", "Phone", null, "+15559990000", "Password123!");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        var user = userRepository.findByPhoneNumber("+15559990000").orElseThrow();
        assertThat(user.isPhoneVerified()).isFalse();

        var codeEntity = verificationCodeRepository.findAll().stream()
                .filter(vc -> vc.getUser().getId().equals(user.getId()) && "PHONE_VERIFICATION".equals(vc.getType()))
                .findFirst()
                .orElseThrow();

        var verifyRequest = new VerifyPhoneRequest("+15559990000", codeEntity.getCode());

        mockMvc.perform(post("/api/auth/phone-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk());

        var updatedUser = userRepository.findByPhoneNumber("+15559990000").orElseThrow();
        assertThat(updatedUser.isPhoneVerified()).isTrue();

        var loginRequest = new LoginRequest("+15559990000", "Password123!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void verifyPhoneShouldFailWhenCodeIsInvalid() throws Exception {
        var registerRequest = new RegisterRequest("Invalid", "Code", null, "+15559990001", "Password123!");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)));

        var verifyRequest = new VerifyPhoneRequest("+15559990001", "WRONG-CODE");

        mockMvc.perform(post("/api/auth/phone-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyPhoneShouldFailWhenCodeIsExpired() throws Exception {
        var registerRequest = new RegisterRequest("Expired", "Code", null, "+15559990002", "Password123!");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)));

        var user = userRepository.findByPhoneNumber("+15559990002").orElseThrow();
        var codeEntity = verificationCodeRepository.findAll().stream()
                .filter(vc -> vc.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow();

        codeEntity.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        verificationCodeRepository.save(codeEntity);

        var verifyRequest = new VerifyPhoneRequest("+15559990002", codeEntity.getCode());

        mockMvc.perform(post("/api/auth/phone-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyPhoneShouldFailWhenUserDoesNotExist() throws Exception {
        var verifyRequest = new VerifyPhoneRequest("+10000000000", "some-code");

        mockMvc.perform(post("/api/auth/phone-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyPhoneShouldReturnMessageWhenAlreadyVerified() throws Exception {
        var registerRequest = new RegisterRequest("Already", "Verified", null, "+15559990003", "Password123!");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)));

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            verify(postRequestedFor(urlEqualTo("/transactionalSMS/sms"))
                    .withRequestBody(containing("+15559990003")));
        });

        var user = userRepository.findByPhoneNumber("+15559990003").orElseThrow();
        var codeEntity = verificationCodeRepository.findAll().stream()
                .filter(vc -> vc.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow();

        var verifyRequest = new VerifyPhoneRequest("+15559990003", codeEntity.getCode());
        mockMvc.perform(post("/api/auth/phone-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/phone-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("Phone is already verified."));
    }
}