package dev.ctlabs.starter.auth.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
        String token,
        String refreshToken
) {
    public AuthResponse(String token) {
        this(token, null);
    }
}