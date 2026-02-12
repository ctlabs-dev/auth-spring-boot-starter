package dev.ctlabs.starter.auth.infrastructure.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ctlabs.starter.auth.application.dto.RegisterRequest;
import dev.ctlabs.starter.auth.domain.repository.UserRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "ctlabs.auth.base-url=/custom/auth",
        "ctlabs.auth.password.validation-regex=^.{4}$",
        "ctlabs.auth.password.validation-message=Password must be exactly 4 characters",
        "ctlabs.auth.default-role=ROLE_TESTER"
})
@AutoConfigureMockMvc
@Transactional
@Testcontainers
class CustomConfigurationAuthControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void registerShouldAcceptSimplePasswordBasedOnConfig() throws Exception {
        var request = new RegisterRequest(
                "Custom",
                "Config",
                "custom@test.com",
                null,
                "1234"
        );

        mockMvc.perform(post("/custom/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        var user = userRepository.findByEmail("custom@test.com").orElseThrow();
        assertThat(user.getRoles()).anyMatch(role -> role.getName().equals("ROLE_TESTER"));
    }

    @Test
    void registerShouldFailIfPasswordDoesNotMatchCustomConfig() throws Exception {
        var request = new RegisterRequest(
                "Fail", "User", "fail@test.com", null, "Password123!"
        );

        mockMvc.perform(post("/custom/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").value("Password must be exactly 4 characters"));
    }

    @Test
    void defaultUrlShouldReturnNotFound() throws Exception {
        var request = new RegisterRequest(
                "Custom",
                "Url",
                "custom@test.com",
                null,
                "Password123!"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }
}