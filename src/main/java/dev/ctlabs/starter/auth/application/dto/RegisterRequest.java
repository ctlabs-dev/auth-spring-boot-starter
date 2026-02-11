package dev.ctlabs.starter.auth.application.dto;

import dev.ctlabs.starter.auth.application.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @Email(message = "Invalid email format")
        String email,

        String phoneNumber,

        @NotBlank(message = "Password is required")
        @ValidPassword
        String password
) {
}