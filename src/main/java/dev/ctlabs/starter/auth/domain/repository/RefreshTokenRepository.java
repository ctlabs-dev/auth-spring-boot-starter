package dev.ctlabs.starter.auth.domain.repository;

import dev.ctlabs.starter.auth.domain.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for managing {@link RefreshToken} entities.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    /**
     * Deletes all refresh tokens for a specific user.
     *
     * @param userId The ID of the user.
     */
    void deleteByUser_Id(UUID userId);

    /**
     * Finds all refresh tokens for a specific user.
     *
     * @param userId The ID of the user.
     * @return A list of refresh tokens.
     */
    List<RefreshToken> findAllByUser_Id(UUID userId);
}
