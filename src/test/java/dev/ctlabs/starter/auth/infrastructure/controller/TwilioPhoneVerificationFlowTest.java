package dev.ctlabs.starter.auth.infrastructure.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import dev.ctlabs.starter.auth.application.dto.ForgotPasswordRequest;
import dev.ctlabs.starter.auth.application.dto.LoginRequest;
import dev.ctlabs.starter.auth.application.dto.RegisterRequest;
import dev.ctlabs.starter.auth.application.dto.ResetPasswordRequest;
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

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
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
        "ctlabs.auth.notifications.phone.provider=TWILIO",
        "ctlabs.auth.notifications.phone.twilio.account-sid=AC_DUMMY_SID",
        "ctlabs.auth.notifications.phone.twilio.auth-token=DUMMY_TOKEN",
        "ctlabs.auth.notifications.phone.twilio.base-url=http://localhost:8091"
})
@AutoConfigureMockMvc
@Transactional
@Testcontainers
class TwilioPhoneVerificationFlowTest {

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
        wireMockServer = new WireMockServer(8091);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8091);
    }

    @AfterAll
    static void stopServer() {
        wireMockServer.stop();
    }

    @BeforeEach
    void reset() {
        wireMockServer.resetAll();
        stubFor(WireMock.post(urlEqualTo("/2010-04-01/Accounts/AC_DUMMY_SID/Messages.json"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withBody("{}")));
    }

    @Test
    void registerShouldGenerateCodeWhenPhoneProviderIsTwilio() throws Exception {
        var request = new RegisterRequest(
                "Twilio", "User", null, "+1234567890", "Password123!"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        var user = userRepository.findByPhoneNumber("+1234567890").orElseThrow();

        assertThat(user.isPhoneVerified()).isFalse();

        boolean hasCode = verificationCodeRepository.findAll().stream()
                .anyMatch(vc -> vc.getUser().getId().equals(user.getId()) && "PHONE_VERIFICATION".equals(vc.getType()));

        assertThat(hasCode).isTrue();

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            verify(postRequestedFor(urlEqualTo("/2010-04-01/Accounts/AC_DUMMY_SID/Messages.json"))
                    .withHeader("Authorization", matching("Basic .*"))
                    .withRequestBody(containing("To=%2B1234567890"))
                    .withRequestBody(containing("Body=Your+verification+code+is")));
        });
    }

    @Test
    void loginShouldFailWhenPhoneNotVerified() throws Exception {
        var registerRequest = new RegisterRequest(
                "Twilio", "Login", null, "+19998887777", "Password123!"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        var loginRequest = new LoginRequest(
                "+19998887777",
                "Password123!"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void verifyPhoneShouldWorkWithValidCode() throws Exception {
        var registerRequest = new RegisterRequest("Verify", "Phone", null, "+18889990000", "Password123!");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        var user = userRepository.findByPhoneNumber("+18889990000").orElseThrow();
        assertThat(user.isPhoneVerified()).isFalse();

        var codeEntity = verificationCodeRepository.findAll().stream()
                .filter(vc -> vc.getUser().getId().equals(user.getId()) && "PHONE_VERIFICATION".equals(vc.getType()))
                .findFirst()
                .orElseThrow();

        var verifyRequest = new VerifyPhoneRequest("+18889990000", codeEntity.getCode());

        mockMvc.perform(post("/api/auth/phone-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk());

        var updatedUser = userRepository.findByPhoneNumber("+18889990000").orElseThrow();
        assertThat(updatedUser.isPhoneVerified()).isTrue();

        var loginRequest = new LoginRequest("+18889990000", "Password123!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void verifyPhoneShouldFailWhenCodeIsInvalid() throws Exception {
        var registerRequest = new RegisterRequest("Invalid", "Code", null, "+18889990001", "Password123!");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        var verifyRequest = new VerifyPhoneRequest("+18889990001", "WRONG-CODE");

        mockMvc.perform(post("/api/auth/phone-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyPhoneShouldFailWhenCodeIsExpired() throws Exception {
        var registerRequest = new RegisterRequest("Expired", "Code", null, "+18889990002", "Password123!");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        var user = userRepository.findByPhoneNumber("+18889990002").orElseThrow();
        var codeEntity = verificationCodeRepository.findAll().stream()
                .filter(vc -> vc.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow();

        codeEntity.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        verificationCodeRepository.save(codeEntity);

        var verifyRequest = new VerifyPhoneRequest("+18889990002", codeEntity.getCode());

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
        var registerRequest = new RegisterRequest("Already", "Verified", null, "+18889990003", "Password123!");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            verify(postRequestedFor(urlEqualTo("/2010-04-01/Accounts/AC_DUMMY_SID/Messages.json"))
                    .withRequestBody(containing("To=%2B18889990003")));
        });

        var user = userRepository.findByPhoneNumber("+18889990003").orElseThrow();
        var codeEntity = verificationCodeRepository.findAll().stream()
                .filter(vc -> vc.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow();

        var verifyRequest = new VerifyPhoneRequest("+18889990003", codeEntity.getCode());
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

    @Test
    void forgotPasswordShouldSendSms() throws Exception {
        var registerRequest = new RegisterRequest("Forgot", "Phone", null, "+18889990004", "Password123!");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        var forgotRequest = new ForgotPasswordRequest("+18889990004");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotRequest)))
                .andExpect(status().isOk());

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            verify(postRequestedFor(urlEqualTo("/2010-04-01/Accounts/AC_DUMMY_SID/Messages.json"))
                    .withRequestBody(containing("To=%2B18889990004")));
        });
    }

    @Test
    void resetPasswordShouldWorkWithValidCode() throws Exception {
        var registerRequest = new RegisterRequest("Reset", "Phone", null, "+18889990005", "Password123!");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        var user = userRepository.findByPhoneNumber("+18889990005").orElseThrow();
        var verificationCode = verificationCodeRepository.findAll().stream()
                .filter(vc -> vc.getUser().getId().equals(user.getId()) && "PHONE_VERIFICATION".equals(vc.getType()))
                .findFirst()
                .orElseThrow();
        var verifyRequest = new VerifyPhoneRequest("+18889990005", verificationCode.getCode());
        mockMvc.perform(post("/api/auth/phone-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk());
        wireMockServer.resetAll();

        var forgotRequest = new ForgotPasswordRequest("+18889990005");
        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(forgotRequest)));

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            stubFor(WireMock.post(urlEqualTo("/2010-04-01/Accounts/AC_DUMMY_SID/Messages.json")).willReturn(aResponse().withStatus(201)));
            verify(postRequestedFor(urlEqualTo("/2010-04-01/Accounts/AC_DUMMY_SID/Messages.json"))
                    .withRequestBody(containing("To=%2B18889990005")));
        });

        var userForReset = userRepository.findByPhoneNumber("+18889990005").orElseThrow();
        var codeEntity = verificationCodeRepository.findAll().stream()
                .filter(vc -> vc.getUser().getId().equals(userForReset.getId()) && "PASSWORD_RESET".equals(vc.getType()))
                .findFirst()
                .orElseThrow();

        var resetRequest = new ResetPasswordRequest("+18889990005", codeEntity.getCode(), "NewPassword123!");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isOk());

        var loginRequest = new LoginRequest("+18889990005", "NewPassword123!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());

        var oldLoginRequest = new LoginRequest("+18889990005", "Password123!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(oldLoginRequest)))
                .andExpect(status().isUnauthorized());
    }
}