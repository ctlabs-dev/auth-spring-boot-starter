package dev.ctlabs.starter.auth.application.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object representing user session information.
 * <p>
 * Contains details about an active user session.
 * </p>
 *
 * @param id          The unique identifier of the session.
 * @param deviceInfo  Information about the device used for the session.
 * @param ipAddress   The IP address from which the session was initiated.
 * @param createdAt   The timestamp when the session was created.
 * @param expiresAt   The timestamp when the session expires.
 */
public record SessionInfo(
        UUID id,
        String deviceInfo,
        String ipAddress,
        Instant createdAt,
        Instant expiresAt
) {
}
