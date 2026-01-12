package dev.ctlabs.starter.auth.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequest(
        @NotBlank(message = "Email or phone number is required")
        String username,

        @NotBlank(message = "Code is required")
        String code,

        @NotBlank(message = "New password is required")
        @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=\\S+$).{8,20}$", message = "Password must be 8-20 characters long, contain at least one digit, one lowercase, one uppercase letter and no whitespace")
        String newPassword
) {
}