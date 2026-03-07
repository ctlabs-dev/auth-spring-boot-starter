package dev.ctlabs.starter.auth.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object for logout request.
 *
 * @param refreshToken The refresh token to invalidate.
 */
public record LogoutRequest(@NotBlank(message = "Refresh token is required") String refreshToken) {}

