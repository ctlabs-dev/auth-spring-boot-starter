package dev.ctlabs.starter.auth.infrastructure.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import dev.ctlabs.starter.auth.application.dto.RegisterRequest;
import dev.ctlabs.starter.auth.domain.repository.UserRepository;
import dev.ctlabs.starter.auth.domain.repository.VerificationCodeRepository;
import org.junit.jupiter.api.AfterEach;
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
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg18");

    private WireMockServer wireMockServer;

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
        wireMockServer = new WireMockServer(8090);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8090);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void registerShouldGenerateCodeWhenPhoneProviderIsBrevo() throws Exception {
        stubFor(WireMock.post(urlEqualTo("/transactionalSMS/sms"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withBody("{}")));

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
}