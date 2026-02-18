package dev.ctlabs.starter.auth.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object for login request.
 *
 * @param username The email or phone number of the user.
 * @param password The password of the user.
 */
public record LoginRequest(
        @NotBlank(message = "Email/Phone is required") String username,

        @NotBlank(message = "Password is required") String password) {
}
