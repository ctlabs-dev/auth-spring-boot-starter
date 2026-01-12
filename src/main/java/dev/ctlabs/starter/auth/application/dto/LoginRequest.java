package dev.ctlabs.starter.auth.application.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Email/Phone is required")
        String username,

        @NotBlank(message = "Password is required")
        String password
) {
}