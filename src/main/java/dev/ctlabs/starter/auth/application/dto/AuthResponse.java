package dev.ctlabs.starter.auth.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Data Transfer Object for authentication response.
 *
 * @param accessToken  The access token.
 * @param refreshToken The refresh token.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(String accessToken, String refreshToken) {
    /**
     * Constructs an AuthResponse with the access token and no refresh token.
     *
     * @param accessToken The access token.
     */
    public AuthResponse(String accessToken) {
        this(accessToken, null);
    }
}
