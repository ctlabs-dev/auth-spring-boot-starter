package dev.ctlabs.starter.auth.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object for forgot password request.
 *
 * @param username The email or phone number of the user.
 */
public record ForgotPasswordRequest(
        @NotBlank(message = "Email or phone number is required")
        String username) {
}
