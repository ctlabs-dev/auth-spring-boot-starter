package dev.ctlabs.starter.auth.infrastructure.controller;

import dev.ctlabs.starter.auth.application.dto.AuthResponse;
import dev.ctlabs.starter.auth.application.dto.ForgotPasswordRequest;
import dev.ctlabs.starter.auth.application.dto.LoginRequest;
import dev.ctlabs.starter.auth.application.dto.RefreshTokenRequest;
import dev.ctlabs.starter.auth.application.dto.RegisterRequest;
import dev.ctlabs.starter.auth.application.dto.ResetPasswordRequest;
import dev.ctlabs.starter.auth.application.dto.VerifyEmailRequest;
import dev.ctlabs.starter.auth.application.dto.VerifyPhoneRequest;
import dev.ctlabs.starter.auth.application.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for authentication operations.
 * Exposes endpoints for login, registration, password reset, and verification.
 */
@RestController
@RequestMapping("${ctlabs.auth.application.base-url:/api/auth}")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Registers a new user.
     *
     * @param request The registration request.
     * @return The authentication response.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    /**
     * Authenticates a user.
     *
     * @param request        The login request.
     * @param servletRequest The HTTP request.
     * @return The authentication response containing tokens.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return ResponseEntity.ok(authService.login(request, servletRequest));
    }

    /**
     * Refreshes an expired access token.
     *
     * @param request The refresh token request.
     * @return The new authentication response.
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    /**
     * Verifies an email address.
     *
     * @param request The email verification request.
     * @return The response confirming verification.
     */
    @PostMapping("/email-verification")
    public ResponseEntity<AuthResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(authService.verifyEmail(request));
    }

    /**
     * Verifies a phone number.
     *
     * @param request The phone verification request.
     * @return The response confirming verification.
     */
    @PostMapping("/phone-verification")
    public ResponseEntity<AuthResponse> verifyPhone(@Valid @RequestBody VerifyPhoneRequest request) {
        return ResponseEntity.ok(authService.verifyPhone(request));
    }

    /**
     * Initiates the password reset process.
     *
     * @param request The forgot password request.
     * @return The response indicating the code has been sent.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<AuthResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    /**
     * Resets the user's password.
     *
     * @param request The reset password request.
     * @return The response confirming the password reset.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<AuthResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }
}
