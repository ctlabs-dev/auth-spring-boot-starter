package dev.ctlabs.starter.auth.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object for email verification request.
 *
 * @param email The email address to verify.
 * @param code  The verification code.
 */
public record VerifyEmailRequest(
        @NotBlank(message = "Email is required") @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Code is required") String code) {
}
