package dev.ctlabs.starter.auth.infrastructure.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ctlabs.starter.auth.application.dto.AuthResponse;
import dev.ctlabs.starter.auth.application.dto.LoginRequest;
import dev.ctlabs.starter.auth.application.dto.RefreshTokenRequest;
import dev.ctlabs.starter.auth.application.dto.RegisterRequest;
import dev.ctlabs.starter.auth.domain.model.User;
import dev.ctlabs.starter.auth.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Testcontainers
class AuthControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest");

    private final MockMvc mockMvc;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Autowired
    public AuthControllerTest(MockMvc mockMvc,
                              UserRepository userRepository,
                              PasswordEncoder passwordEncoder,
                              ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    // <editor-fold desc="Registration Tests">
    @Test
    void registerShouldReturnOkWhenValidEmailRequest() throws Exception {
        var request = new RegisterRequest(
                "Juan",
                "Perez",
                "juan.perez@test.com",
                null,
                "Password123!"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("User registered. Please verify your account."));

        var user = userRepository.findByEmail("juan.perez@test.com").orElseThrow();

        assertThat(user.getProfile()).isNotNull();
        assertThat(user.getProfile().getFirstName()).isEqualTo("Juan");
        assertThat(user.getProfile().getLastName()).isEqualTo("Perez");

        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getRoles()).extracting("name").contains("ROLE_USER");
    }

    @Test
    void registerShouldReturnOkWhenValidPhoneRequest() throws Exception {
        var request = new RegisterRequest(
                "Juan",
                "Perez",
                null,
                "+59170712345",
                "Password123!"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("User registered. Please verify your account."));

        var user = userRepository.findByPhoneNumber("+59170712345").orElseThrow();
        assertThat(user.isPhoneVerified()).isTrue();
    }

    @Test
    void registerShouldReturnOkWhenValidEmailAndPhoneRequest() throws Exception {
        var request = new RegisterRequest(
                "Juan",
                "Perez",
                "juan.perez@test.com",
                "+59170712345",
                "Password123!"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("User registered. Please verify your account."));
    }

    @Test
    void registerShouldFailWhenEmailAlreadyExists() throws Exception {
        var user = new User();
        user.setEmail("existing@test.com");
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setStatus("active");
        userRepository.save(user);

        var request = new RegisterRequest(
                "Juan",
                "Perez",
                "existing@test.com",
                null,
                "Password123!"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerShouldFailWhenPhoneAlreadyExists() throws Exception {
        var user = new User();
        user.setPhoneNumber("+59170712345");
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setStatus("active");
        userRepository.save(user);

        var request = new RegisterRequest(
                "Juan",
                "Perez",
                null,
                "+59170712345",
                "Password123!"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerShouldFailWhenNoContactMethodProvided() throws Exception {
        var request = new RegisterRequest(
                "Juan",
                "Perez",
                null,
                null,
                "Password123!"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerShouldFailWhenPhoneFormatIsInvalid() throws Exception {
        var request = new RegisterRequest(
                "Juan",
                "Perez",
                null,
                "12345",
                "Password123!"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerShouldFailWhenFirstNameIsMissing() throws Exception {
        var request = new RegisterRequest(
                null,
                "Perez",
                "juan@test.com",
                null,
                "Password123!"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerShouldFailWhenPasswordIsTooShort() throws Exception {
        var request = new RegisterRequest(
                "Juan",
                "Perez",
                "juan.perez@test.com",
                null,
                "short"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerShouldFailWhenEmailExistsWithDifferentCase() throws Exception {
        var user = new User();
        user.setEmail("mixedcase@test.com");
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setStatus("active");
        userRepository.save(user);

        var request = new RegisterRequest(
                "Juan", "Perez", "MixedCase@Test.com", null, "Password123!"
        );


        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    // </editor-fold>

    // <editor-fold desc="Login Tests">
    @Test
    void loginShouldReturnOkWhenCredentialsAreValid() throws Exception {
        var registerRequest = new RegisterRequest(
                "Login", "Success", "loginsuccess@test.com", null, "Password123!"
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        var loginRequest = new LoginRequest("loginsuccess@test.com", "Password123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    void loginShouldFailWhenEmailInvalid() throws Exception {
        var request = new LoginRequest(
                "unknown@test.com",
                "Password123!"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginShouldFailWhenPhoneInvalid() throws Exception {
        var request = new LoginRequest(
                "+59100000000",
                "Password123!"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginShouldFailWhenEmailValidButPasswordIncorrect() throws Exception {
        var registerRequest = new RegisterRequest(
                "Login",
                "User",
                "login@test.com",
                null,
                "Password123!"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        var loginRequest = new LoginRequest(
                "login@test.com",
                "WrongPassword!"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginShouldFailWhenPhoneValidButPasswordIncorrect() throws Exception {
        var registerRequest = new RegisterRequest(
                "Login",
                "User",
                null,
                "+59170712345",
                "Password123!"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        var loginRequest = new LoginRequest(
                "+59170712345",
                "WrongPassword!"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginShouldFailWhenUserIsSuspended() throws Exception {
        var registerRequest = new RegisterRequest(
                "Suspended", "User", "suspended@test.com", null, "Password123!"
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        var user = userRepository.findByEmail("suspended@test.com").orElseThrow();
        user.setStatus("suspended");
        userRepository.save(user);

        var loginRequest = new LoginRequest("suspended@test.com", "Password123!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginShouldFailWhenUserIsArchived() throws Exception {
        var registerRequest = new RegisterRequest("Archived", "User", "archived@test.com", null, "Password123!");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        var user = userRepository.findByEmail("archived@test.com").orElseThrow();
        user.setStatus("archived");
        userRepository.save(user);

        var loginRequest = new LoginRequest("archived@test.com", "Password123!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginShouldSucceedWithDifferentCaseEmail() throws Exception {
        var user = new User();
        user.setEmail("casesensitive@test.com");
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setStatus("active");
        user.setEmailVerified(true);
        userRepository.save(user);

        var loginRequest = new LoginRequest("CaseSensitive@Test.com", "Password123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());
    }
    // </editor-fold>

    // <editor-fold desc="Refresh Token Tests">
    @Test
    void refreshTokenShouldReturnNewAccessToken() throws Exception {
        var registerRequest = new RegisterRequest("Refresh", "Token", "refresh@test.com", null, "Password123!");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        var loginRequest = new LoginRequest("refresh@test.com", "Password123!");
        String responseJson = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn().getResponse().getContentAsString();

        var authResponse = objectMapper.readValue(responseJson, AuthResponse.class);
        var refreshRequest = new RefreshTokenRequest(authResponse.refreshToken());

        mockMvc.perform(post("/api/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void refreshTokenShouldFailWhenTokenFormatIsInvalid() throws Exception {
        var refreshRequest = new RefreshTokenRequest("invalid-format");

        mockMvc.perform(post("/api/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshTokenShouldFailWhenTokenDoesNotExist() throws Exception {
        var randomId = java.util.UUID.randomUUID();
        var refreshRequest = new RefreshTokenRequest(randomId + ":some-secret");

        mockMvc.perform(post("/api/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshTokenShouldFailWhenTokenHashDoesNotMatch() throws Exception {
        var registerRequest = new RegisterRequest("Hash", "Test", "hash@test.com", null, "Password123!");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        var loginRequest = new LoginRequest("hash@test.com", "Password123!");
        String responseJson = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn().getResponse().getContentAsString();

        var authResponse = objectMapper.readValue(responseJson, AuthResponse.class);
        String validToken = authResponse.refreshToken();
        String[] parts = validToken.split(":");
        String tamperedToken = parts[0] + ":tampered-secret";

        var refreshRequest = new RefreshTokenRequest(tamperedToken);

        mockMvc.perform(post("/api/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshTokenShouldFailWhenUserIsSuspended() throws Exception {
        var registerRequest = new RegisterRequest("Suspend", "Refresh", "suspend_refresh@test.com", null, "Password123!");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        var loginRequest = new LoginRequest("suspend_refresh@test.com", "Password123!");
        String responseJson = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn().getResponse().getContentAsString();

        var authResponse = objectMapper.readValue(responseJson, AuthResponse.class);

        var user = userRepository.findByEmail("suspend_refresh@test.com").orElseThrow();
        user.setStatus("suspended");
        userRepository.save(user);

        var refreshRequest = new RefreshTokenRequest(authResponse.refreshToken());

        mockMvc.perform(post("/api/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isBadRequest());
    }
    // </editor-fold>
}
