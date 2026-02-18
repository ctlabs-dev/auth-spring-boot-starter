package dev.ctlabs.starter.auth.domain.repository;

import dev.ctlabs.starter.auth.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing {@link User} entities.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by their email address.
     *
     * @param email The email address to search for.
     * @return An {@link Optional} containing the User if found.
     */
    Optional<User> findByEmail(String email);

    /**
     * Finds a user by their phone number.
     *
     * @param phoneNumber The phone number to search for.
     * @return An {@link Optional} containing the User if found.
     */
    Optional<User> findByPhoneNumber(String phoneNumber);
}
