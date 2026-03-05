package dev.ctlabs.starter.auth.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Data Transfer Object for resend verification code request.
 *
 * @param username The email or phone number to identify the user.
 * @param channel The channel to resend the verification code through ("email" or "phone").
 */
public record ResendVerificationRequest(
        @NotBlank(message = "Username (email or phone) is required") String username,
        @NotBlank(message = "Channel is required")
        @Pattern(regexp = "^(email|phone)$", message = "Channel must be 'email' or 'phone'") String channel) {}


