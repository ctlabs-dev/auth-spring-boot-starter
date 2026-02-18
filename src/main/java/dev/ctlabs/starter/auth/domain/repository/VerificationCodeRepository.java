package dev.ctlabs.starter.auth.domain.repository;

import dev.ctlabs.starter.auth.domain.model.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing {@link VerificationCode} entities.
 */
@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, UUID> {
    /**
     * Finds a verification code by user ID, type, and code value.
     *
     * @param userId The ID of the user.
     * @param type   The type of verification (e.g., "EMAIL_VERIFICATION").
     * @param code   The code value.
     * @return An {@link Optional} containing the VerificationCode if found.
     */
    Optional<VerificationCode> findByUser_IdAndTypeAndCode(UUID userId, String type, String code);

    void deleteByUser_IdAndType(UUID userId, String type);
}
