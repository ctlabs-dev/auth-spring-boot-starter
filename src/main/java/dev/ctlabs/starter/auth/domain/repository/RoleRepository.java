package dev.ctlabs.starter.auth.domain.repository;

import dev.ctlabs.starter.auth.domain.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing {@link Role} entities.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    /**
     * Finds a role by its name.
     *
     * @param name The name of the role (e.g., "ADMIN").
     * @return An {@link Optional} containing the Role if found.
     */
    Optional<Role> findByName(String name);
}
