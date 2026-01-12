package dev.ctlabs.starter.auth.application.dto;

import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank(message = "Email or phone number is required")
        String username
) {
}