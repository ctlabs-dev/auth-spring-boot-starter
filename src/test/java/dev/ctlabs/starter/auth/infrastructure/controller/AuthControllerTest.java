package dev.ctlabs.starter.auth.infrastructure.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg18");

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
    // </editor-fold>

//    // <editor-fold desc="Login Tests">
//    @Test
//    void loginShouldFailWhenEmailInvalid() throws Exception {
//        // 7: Login with invalid email.
//        var request = new LoginRequest(
//                "unknown@test.com",
//                "Password123!"
//        );
//
//        mockMvc.perform(post("/api/auth/login")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isUnauthorized());
//    }
//
//    @Test
//    void loginShouldFailWhenPhoneInvalid() throws Exception {
//        // 8: Login with invalid phone.
//        var request = new LoginRequest(
//                "+59100000000",
//                "Password123!"
//        );
//
//        mockMvc.perform(post("/api/auth/login")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isUnauthorized());
//    }
//
//    @Test
//    void loginShouldFailWhenEmailValidButPasswordIncorrect() throws Exception {
//        // 9: Login with valid email + incorrect password.
//        var registerRequest = new RegisterRequest(
//                "Login",
//                "User",
//                "login@test.com",
//                null,
//                "Password123!"
//        );
//
//        mockMvc.perform(post("/api/auth/register")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(registerRequest)))
//                .andExpect(status().isOk());
//
//        var user = userRepository.findByEmail("login@test.com").orElseThrow();
//        var code = user.getVerification().getEmailVerificationCode();
//        var verifyRequest = new VerifyEmailRequest("login@test.com", code);
//
//        mockMvc.perform(post("/api/auth/email-verification")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(verifyRequest)))
//                .andExpect(status().isOk());
//
//        var loginRequest = new LoginRequest(
//                "login@test.com",
//                "WrongPassword!"
//        );
//
//        mockMvc.perform(post("/api/auth/login")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(loginRequest)))
//                .andExpect(status().isUnauthorized());
//    }
//
//    @Test
//    void loginShouldFailWhenPhoneValidButPasswordIncorrect() throws Exception {
//        // 10: Login with valid phone + incorrect password.
//        var registerRequest = new RegisterRequest(
//                "Login",
//                "User",
//                null,
//                "+59170712345",
//                "Password123!"
//        );
//
//        mockMvc.perform(post("/api/auth/register")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(registerRequest)))
//                .andExpect(status().isOk());
//
//        var user = userRepository.findByPhoneNumber("+59170712345").orElseThrow();
//        var code = user.getVerification().getPhoneVerificationCode();
//        var verifyRequest = new VerifyPhoneRequest("+59170712345", code);
//
//        mockMvc.perform(post("/api/auth/phone-verification")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(verifyRequest)))
//                .andExpect(status().isOk());
//
//        var loginRequest = new LoginRequest(
//                "+59170712345",
//                "WrongPassword!"
//        );
//
//        mockMvc.perform(post("/api/auth/login")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(loginRequest)))
//                .andExpect(status().isUnauthorized());
//    }
//
//    @Test
//    void loginShouldFailWhenEmailNotVerified() throws Exception {
//        // 11: Login with email but user did not verify email.
//        var registerRequest = new RegisterRequest(
//                "Login",
//                "User",
//                "unverified@test.com",
//                null,
//                "Password123!"
//        );
//
//        mockMvc.perform(post("/api/auth/register")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(registerRequest)))
//                .andExpect(status().isOk());
//
//        var loginRequest = new LoginRequest(
//                "unverified@test.com",
//                "Password123!"
//        );
//
//        mockMvc.perform(post("/api/auth/login")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(loginRequest)))
//                .andExpect(status().isUnauthorized());
//    }
//
//    @Test
//    void loginShouldFailWhenPhoneNotVerified() throws Exception {
//        // 12: Login with phone but user did not verify phone.
//        var registerRequest = new RegisterRequest(
//                "Login",
//                "User",
//                null,
//                "+59170712345",
//                "Password123!"
//        );
//
//        mockMvc.perform(post("/api/auth/register")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(registerRequest)))
//                .andExpect(status().isOk());
//
//        var loginRequest = new LoginRequest(
//                "+59170712345",
//                "Password123!"
//        );
//
//        mockMvc.perform(post("/api/auth/login")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(loginRequest)))
//                .andExpect(status().isUnauthorized());
//    }
//    // </editor-fold>
//
//    // <editor-fold desc="Password Management Tests">
//    @Test
//    void forgotPasswordShouldReturnOkWhenUserExistsWithEmail() throws Exception {
//        // 13: Forgot password with valid email
//        var registerRequest = new RegisterRequest(
//                "Juan", "Perez", "forgot@test.com", null, "Password123!"
//        );
//        mockMvc.perform(post("/api/auth/register")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(registerRequest)))
//                .andExpect(status().isOk());
//
//        var user = userRepository.findByEmail("forgot@test.com").orElseThrow();
//        var code = user.getVerification().getEmailVerificationCode();
//        var verifyRequest = new VerifyEmailRequest("forgot@test.com", code);
//
//        mockMvc.perform(post("/api/auth/email-verification")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(verifyRequest)))
//                .andExpect(status().isOk());
//
//        var forgotRequest = new ForgotPasswordRequest("forgot@test.com");
//
//        mockMvc.perform(post("/api/auth/forgot-password")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(forgotRequest)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.token").value("Password reset code sent."));
//    }
//
//    @Test
//    void forgotPasswordShouldFailWhenUserDoesNotExist() throws Exception {
//        // 14: Forgot password with non-existent user
//        var forgotRequest = new ForgotPasswordRequest("nonexistent@test.com");
//
//        mockMvc.perform(post("/api/auth/forgot-password")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(forgotRequest)))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void resetPasswordShouldReturnOkWhenValidRequestWithEmail() throws Exception {
//        // 15: Reset password with valid code (Email)
//        var registerRequest = new RegisterRequest(
//                "Juan", "Perez", "reset@test.com", null, "Password123!"
//        );
//        mockMvc.perform(post("/api/auth/register")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(registerRequest)))
//                .andExpect(status().isOk());
//
//        var user = userRepository.findByEmail("reset@test.com").orElseThrow();
//        var verificationCode = user.getVerification().getEmailVerificationCode();
//        var verifyRequest = new VerifyEmailRequest("reset@test.com", verificationCode);
//
//        mockMvc.perform(post("/api/auth/email-verification")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(verifyRequest)))
//                .andExpect(status().isOk());
//
//        var forgotRequest = new ForgotPasswordRequest("reset@test.com");
//        mockMvc.perform(post("/api/auth/forgot-password")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(forgotRequest)))
//                .andExpect(status().isOk());
//
//        user = userRepository.findByEmail("reset@test.com").orElseThrow();
//        String code = user.getVerification().getResetCode();
//
//        var resetRequest = new ResetPasswordRequest("reset@test.com", code, "NewPassword123!");
//
//        mockMvc.perform(post("/api/auth/reset-password")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(resetRequest)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.token").value("Password reset successfully."));
//
//        // Verify login with new password
//        var loginRequest = new LoginRequest("reset@test.com", "NewPassword123!");
//        mockMvc.perform(post("/api/auth/login")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(loginRequest)))
//                .andExpect(status().isOk());
//    }
//
//    @Test
//    void resetPasswordShouldFailWhenCodeIsInvalid() throws Exception {
//        var registerRequest = new RegisterRequest(
//                "Juan", "Perez", "reset_invalid@test.com", null, "Password123!"
//        );
//        mockMvc.perform(post("/api/auth/register")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(registerRequest)))
//                .andExpect(status().isOk());
//
//        var user = userRepository.findByEmail("reset_invalid@test.com").orElseThrow();
//        var code = user.getVerification().getEmailVerificationCode();
//        var verifyRequest = new VerifyEmailRequest("reset_invalid@test.com", code);
//
//        mockMvc.perform(post("/api/auth/email-verification")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(verifyRequest)))
//                .andExpect(status().isOk());
//
//        var forgotRequest = new ForgotPasswordRequest("reset_invalid@test.com");
//        mockMvc.perform(post("/api/auth/forgot-password")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(forgotRequest)))
//                .andExpect(status().isOk());
//
//        var resetRequest = new ResetPasswordRequest("reset_invalid@test.com", "wrong-code", "NewPassword123!");
//
//        mockMvc.perform(post("/api/auth/reset-password")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(resetRequest)))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void resetPasswordShouldFailWhenPasswordIsWeak() throws Exception {
//        // 17: Reset password with weak password
//        var registerRequest = new RegisterRequest(
//                "Juan", "Perez", "reset_weak@test.com", null, "Password123!"
//        );
//        mockMvc.perform(post("/api/auth/register")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(registerRequest)))
//                .andExpect(status().isOk());
//
//        var user = userRepository.findByEmail("reset_weak@test.com").orElseThrow();
//        var verificationCode = user.getVerification().getEmailVerificationCode();
//        var verifyRequest = new VerifyEmailRequest("reset_weak@test.com", verificationCode);
//
//        mockMvc.perform(post("/api/auth/email-verification")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(verifyRequest)))
//                .andExpect(status().isOk());
//
//        var forgotRequest = new ForgotPasswordRequest("reset_weak@test.com");
//        mockMvc.perform(post("/api/auth/forgot-password")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(forgotRequest)))
//                .andExpect(status().isOk());
//
//        user = userRepository.findByEmail("reset_weak@test.com").orElseThrow();
//        String code = user.getVerification().getResetCode();
//
//        var resetRequest = new ResetPasswordRequest("reset_weak@test.com", code, "weak");
//
//        mockMvc.perform(post("/api/auth/reset-password")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(resetRequest)))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void forgotPasswordShouldReturnOkWhenUserExistsWithPhone() throws Exception {
//        // 18: Forgot password with valid phone
//        var registerRequest = new RegisterRequest(
//                "Maria", "Gomez", null, "+59170712345", "Password123!"
//        );
//        mockMvc.perform(post("/api/auth/register")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(registerRequest)))
//                .andExpect(status().isOk());
//
//        var user = userRepository.findByPhoneNumber("+59170712345").orElseThrow();
//        var code = user.getVerification().getPhoneVerificationCode();
//        var verifyRequest = new VerifyPhoneRequest("+59170712345", code);
//
//        mockMvc.perform(post("/api/auth/phone-verification")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(verifyRequest)))
//                .andExpect(status().isOk());
//
//        var forgotRequest = new ForgotPasswordRequest("+59170712345");
//
//        mockMvc.perform(post("/api/auth/forgot-password")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(forgotRequest)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.token").value("Password reset code sent."));
//    }
//
//    @Test
//    void resetPasswordShouldReturnOkWhenValidRequestWithPhone() throws Exception {
//        // 19: Reset password with valid code (Phone)
//        var registerRequest = new RegisterRequest(
//                "Maria", "Gomez", null, "+59170754321", "Password123!"
//        );
//        mockMvc.perform(post("/api/auth/register")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(registerRequest)))
//                .andExpect(status().isOk());
//
//        var user = userRepository.findByPhoneNumber("+59170754321").orElseThrow();
//        var verificationCode = user.getVerification().getPhoneVerificationCode();
//        var verifyRequest = new VerifyPhoneRequest("+59170754321", verificationCode);
//
//        mockMvc.perform(post("/api/auth/phone-verification")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(verifyRequest)))
//                .andExpect(status().isOk());
//
//        var forgotRequest = new ForgotPasswordRequest("+59170754321");
//        mockMvc.perform(post("/api/auth/forgot-password")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(forgotRequest)))
//                .andExpect(status().isOk());
//
//        user = userRepository.findByPhoneNumber("+59170754321").orElseThrow();
//        String code = user.getVerification().getResetCode();
//
//        var resetRequest = new ResetPasswordRequest("+59170754321", code, "NewPassword123!");
//
//        mockMvc.perform(post("/api/auth/reset-password")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(resetRequest)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.token").value("Password reset successfully."));
//
//        // Verify login with new password
//        var loginRequest = new LoginRequest("+59170754321", "NewPassword123!");
//        mockMvc.perform(post("/api/auth/login")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(loginRequest)))
//                .andExpect(status().isOk());
//    }
//    // </editor-fold>
}
