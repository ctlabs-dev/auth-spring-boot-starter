package dev.ctlabs.starter.auth.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Data Transfer Object for authentication response.
 *
 * @param token        The access token.
 * @param refreshToken The refresh token.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(String token, String refreshToken) {
    /**
     * Constructs an AuthResponse with the token and no refresh token.
     *
     * @param token The access token.
     */
    public AuthResponse(String token) {
        this(token, null);
    }
}
