package dev.ctlabs.starter.auth.application.dto;

/**
 * Data Transfer Object for refresh token request.
 *
 * @param refreshToken The refresh token.
 */
public record RefreshTokenRequest(String refreshToken) {}
