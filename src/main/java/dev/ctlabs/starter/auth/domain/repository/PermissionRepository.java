package dev.ctlabs.starter.auth.domain.repository;

import dev.ctlabs.starter.auth.domain.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing {@link Permission} entities.
 * <p>
 * Provides methods for CRUD operations and finding permissions by slug.
 * </p>
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Integer> {
    /**
     * Finds a permission by its unique slug.
     *
     * @param slug The permission slug (e.g., "users:read").
     * @return An {@link Optional} containing the Permission if found.
     */
    Optional<Permission> findBySlug(String slug);
}
