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
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void registerShouldGenerateCodeWhenProviderIsBrevo() throws Exception {
        stubFor(WireMock.post(urlEqualTo("/smtp/email"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"messageId\":\"<dummy>\"}")));

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
}