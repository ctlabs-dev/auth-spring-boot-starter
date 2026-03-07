package dev.ctlabs.starter.auth.application.service;

import dev.ctlabs.starter.auth.autoconfigure.AuthProperties;
import dev.ctlabs.starter.auth.domain.model.Permission;
import dev.ctlabs.starter.auth.domain.model.Profile;
import dev.ctlabs.starter.auth.domain.model.Role;
import dev.ctlabs.starter.auth.domain.model.User;
import dev.ctlabs.starter.auth.domain.repository.PermissionRepository;
import dev.ctlabs.starter.auth.domain.repository.RefreshTokenRepository;
import dev.ctlabs.starter.auth.domain.repository.RoleRepository;
import dev.ctlabs.starter.auth.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for administrative user management operations.
 * Handles user status changes, role assignment, and permission management.
 * Intended to be used by admin dashboards or internal tools.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Changes the status of a user (e.g., "active", "suspended", "banned").
     */
    @Transactional
    public void changeUserStatus(UUID userId, String newStatus) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        user.setStatus(newStatus);
        userRepository.save(user);

        if (!"active".equalsIgnoreCase(newStatus)) {
            refreshTokenRepository.deleteByUser_Id(userId);
            log.info("Revoked refresh tokens for user: {}", userId);
        }
        log.info("User status changed. ID: {}, New Status: {}", userId, newStatus);
    }

    /**
     * Soft deletes a user by changing their status to 'archived'.
     */
    @Transactional
    public void deleteUser(UUID userId) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        user.setStatus("archived");
        userRepository.save(user);
        refreshTokenRepository.deleteByUser_Id(userId);
        log.info("User soft-deleted (status set to archived). ID: {}", userId);
    }

    /**
     * Creates a new role in the system.
     */
    @Transactional
    public void createRole(String roleName, String description) {
        if (roleRepository.findByName(roleName).isPresent()) {
            throw new IllegalArgumentException("Role already exists: " + roleName);
        }
        Role role = new Role();
        role.setName(roleName);
        role.setDescription(description);
        roleRepository.save(role);
        log.info("Role created: {}", roleName);
    }

    /**
     * Assigns a role to a user.
     */
    @Transactional
    public void assignRole(UUID userId, String roleName) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        Role role = roleRepository
                .findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));

        user.getRoles().add(role);
        userRepository.save(user);
        log.info("Role '{}' assigned to user ID: {}", roleName, userId);
    }

    /**
     * Removes a role from a user.
     */
    @Transactional
    public void removeRole(UUID userId, String roleName) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        Role role = roleRepository
                .findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));

        user.getRoles().remove(role);
        userRepository.save(user);
        log.info("Role '{}' removed from user ID: {}", roleName, userId);
    }

    /**
     * Creates a new permission in the system.
     */
    @Transactional
    public void createPermission(String slug, String description) {
        if (permissionRepository.findBySlug(slug).isPresent()) {
            throw new IllegalArgumentException("Permission already exists: " + slug);
        }
        Permission permission = new Permission();
        permission.setSlug(slug);
        permission.setDescription(description);
        permissionRepository.save(permission);
        log.info("Permission created: {}", slug);
    }

    /**
     * Assigns a permission to a role.
     */
    @Transactional
    public void assignPermissionToRole(String roleName, String permissionSlug) {
        Role role = roleRepository
                .findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));

        Permission permission = permissionRepository
                .findBySlug(permissionSlug)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + permissionSlug));

        role.getPermissions().add(permission);
        roleRepository.save(role);
        log.info("Permission '{}' assigned to role '{}'", permissionSlug, roleName);
    }

    /**
     * Removes a permission from a role.
     */
    @Transactional
    public void removePermissionFromRole(String roleName, String permissionSlug) {
        Role role = roleRepository
                .findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));

        Permission permission = permissionRepository
                .findBySlug(permissionSlug)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + permissionSlug));

        role.getPermissions().remove(permission);
        roleRepository.save(role);
        log.info("Permission '{}' removed from role '{}'", permissionSlug, roleName);
    }

    /**
     * Creates an initial admin user based on configuration properties if it doesn't exist.
     * This method is transactional.
     *
     * @param adminProps The admin configuration properties.
     */
    @Transactional
    public void createAdminUserIfNotExists(AuthProperties.Admin adminProps) {
        if (userRepository.findByEmail(adminProps.getEmail()).isPresent()) {
            log.debug("Admin user {} already exists. Skipping creation.", adminProps.getEmail());
            return;
        }

        log.info("Creating initial admin user: {}", adminProps.getEmail());
        User admin = new User();
        admin.setEmail(adminProps.getEmail());
        admin.setPassword(passwordEncoder.encode(adminProps.getPassword()));
        admin.setEmailVerified(true);
        admin.setPhoneVerified(true);
        admin.setStatus("active");

        Profile profile = new Profile();
        profile.setFirstName(adminProps.getFirstName());
        profile.setLastName(adminProps.getLastName());
        profile.setUser(admin);
        admin.setProfile(profile);

        Role role = getOrCreateRole(adminProps.getRole());

        if (adminProps.getPermissions() != null && !adminProps.getPermissions().isEmpty()) {
            boolean roleModified = false;
            for (String permSlug : adminProps.getPermissions()) {
                Permission permission = getOrCreatePermission(permSlug);

                if (!role.getPermissions().contains(permission)) {
                    role.getPermissions().add(permission);
                    roleModified = true;
                }
            }
            if (roleModified) {
                roleRepository.save(role);
            }
        }

        admin.getRoles().add(role);

        userRepository.save(admin);
        log.info("Initial admin user created successfully.");
    }

    private Role getOrCreateRole(String name) {
        return roleRepository.findByName(name).orElseGet(() -> {
            log.info("Role {} not found, creating it.", name);
            Role newRole = new Role();
            newRole.setName(name);
            newRole.setDescription("Administrator role");
            return roleRepository.save(newRole);
        });
    }

    private Permission getOrCreatePermission(String slug) {
        return permissionRepository.findBySlug(slug).orElseGet(() -> {
            log.info("Permission {} not found, creating it.", slug);
            Permission newPerm = new Permission();
            newPerm.setSlug(slug);
            newPerm.setDescription("Auto-generated permission");
            return permissionRepository.save(newPerm);
        });
    }
}