package dev.ctlabs.starter.auth.application.dto;

/**
 * A generic DTO for returning simple messages from API endpoints.
 *
 * @param message The message to be returned.
 */
public record MessageResponse(String message) {}