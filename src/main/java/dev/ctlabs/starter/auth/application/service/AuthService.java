package dev.ctlabs.starter.auth.application.service;

import dev.ctlabs.starter.auth.application.dto.AuthResponse;
import dev.ctlabs.starter.auth.application.dto.ForgotPasswordRequest;
import dev.ctlabs.starter.auth.application.dto.LoginRequest;
import dev.ctlabs.starter.auth.application.dto.RefreshTokenRequest;
import dev.ctlabs.starter.auth.application.dto.RegisterRequest;
import dev.ctlabs.starter.auth.application.dto.ResetPasswordRequest;
import dev.ctlabs.starter.auth.application.dto.SessionInfo;
import dev.ctlabs.starter.auth.application.dto.VerifyEmailRequest;
import dev.ctlabs.starter.auth.application.dto.VerifyPhoneRequest;
import dev.ctlabs.starter.auth.autoconfigure.AuthProperties;
import dev.ctlabs.starter.auth.domain.model.Permission;
import dev.ctlabs.starter.auth.domain.model.Profile;
import dev.ctlabs.starter.auth.domain.model.RefreshToken;
import dev.ctlabs.starter.auth.domain.model.Role;
import dev.ctlabs.starter.auth.domain.model.User;
import dev.ctlabs.starter.auth.domain.model.VerificationCode;
import dev.ctlabs.starter.auth.domain.repository.RefreshTokenRepository;
import dev.ctlabs.starter.auth.domain.repository.RoleRepository;
import dev.ctlabs.starter.auth.domain.repository.UserRepository;
import dev.ctlabs.starter.auth.domain.repository.VerificationCodeRepository;
import dev.ctlabs.starter.auth.infrastructure.security.JwtService;
import dev.ctlabs.starter.auth.infrastructure.service.EmailService;
import dev.ctlabs.starter.auth.infrastructure.service.PhoneService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Service for authentication operations.
 * Handles login, registration, verification, password reset, and token management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final PhoneService phoneService;
    private final AuthProperties authProperties;

    /**
     * Authenticates a user based on login request.
     *
     * @param request        The login request containing username and password.
     * @param servletRequest The HTTP servlet request to extract device info and IP.
     * @return An {@link AuthResponse} containing the JWT and refresh token.
     */
    public AuthResponse login(LoginRequest request, HttpServletRequest servletRequest) {
        String identifier = request.username();

        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Email or phone number must be provided for login.");
        }

        if (identifier.contains("@")) {
            identifier = identifier.trim().toLowerCase();
        }

        log.info("Login attempt for user: {}", identifier);
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(identifier, request.password()));
        var userDetails = (UserDetails) authentication.getPrincipal();

        final String finalIdentifier = identifier;
        User user = userRepository
                .findByEmail(finalIdentifier)
                .or(() -> userRepository.findByPhoneNumber(finalIdentifier))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("roles", user.getRoles().stream().map(Role::getName).toList());
        extraClaims.put(
                "permissions",
                user.getRoles().stream()
                        .flatMap(role -> role.getPermissions().stream())
                        .map(Permission::getSlug)
                        .distinct()
                        .toList());
        var jwt = jwtService.generateToken(extraClaims, userDetails);

        String rawRefreshToken = UUID.randomUUID().toString();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(passwordEncoder.encode(rawRefreshToken));
        refreshToken.setExpiresAt(Instant.now().plus(authProperties.getRefreshToken().getExpiration()));
        refreshToken.setDeviceInfo(servletRequest.getHeader("User-Agent"));
        refreshToken.setIpAddress(servletRequest.getRemoteAddr());
        refreshToken = refreshTokenRepository.save(refreshToken);

        String compositeToken = refreshToken.getId().toString() + ":" + rawRefreshToken;

        log.info("User authenticated successfully: {}", identifier);
        return new AuthResponse(jwt, compositeToken);
    }

    /**
     * Registers a new user.
     *
     * @param request The registration request containing user details.
     * @return An {@link AuthResponse} requiring verification.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email() != null ? request.email().trim().toLowerCase() : null;
        boolean hasEmail = email != null && !email.isBlank();
        boolean hasPhone =
                request.phoneNumber() != null && !request.phoneNumber().isBlank();

        log.info("Registration attempt. Email: {}, Phone: {}", email, request.phoneNumber());

        if (!hasEmail && !hasPhone) {
            throw new IllegalArgumentException("At least one contact method (email or phone) must be provided.");
        }

        if (hasEmail && userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email is already registered");
        }

        if (hasPhone) {
            if (!request.phoneNumber().matches("^\\+[1-9]\\d{1,14}$")) {
                throw new IllegalArgumentException("Phone number must be in E.164 format (e.g. +59170712345)");
            }
            if (userRepository.findByPhoneNumber(request.phoneNumber()).isPresent()) {
                throw new IllegalArgumentException("Phone number is already registered");
            }
        }

        User user = new User();
        user.setEmail(email);
        user.setPhoneNumber(request.phoneNumber());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setStatus("active");

        Profile profile = new Profile();
        profile.setFirstName(request.firstName());
        profile.setLastName(request.lastName());
        profile.setUser(user);
        user.setProfile(profile);

        String defaultRoleName = authProperties.getDefaultRole();
        Role role = roleRepository.findByName(defaultRoleName).orElseGet(() -> {
            Role newRole = new Role();
            newRole.setName(defaultRoleName);
            return roleRepository.save(newRole);
        });
        user.getRoles().add(role);

        boolean isEmailProviderNone =
                authProperties.getNotifications().getMail().getProvider()
                == AuthProperties.Notifications.Mail.Provider.NONE;
        if (hasEmail && isEmailProviderNone) {
            user.setEmailVerified(true);
        }

        boolean isPhoneProviderNone =
                authProperties.getNotifications().getPhone().getProvider()
                        == AuthProperties.Notifications.Phone.Provider.NONE;
        if (hasPhone && isPhoneProviderNone) {
            user.setPhoneVerified(true);
        }

        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("User already registered (Email or Phone conflict)");
        }

        if (hasEmail && !isEmailProviderNone) {
            String code = UUID.randomUUID().toString();
            Duration expiration = authProperties.getVerification().getEmailLinkExpiration();
            long minutes = expiration.toMinutes();

            long displayValue = minutes;
            String displayUnit = authProperties.getVerification().getUnitMinutes();
            if (minutes >= 60 && minutes % 60 == 0) {
                displayValue = minutes / 60;
                displayUnit = authProperties.getVerification().getUnitHours();
            }

            createVerificationCode(user, "EMAIL_VERIFICATION", code, expiration);
            emailService.sendVerificationEmail(
                    user.getEmail(), profile.getFirstName(), code, displayValue, displayUnit);
        } else if (hasPhone && !isPhoneProviderNone) {
            String code = String.valueOf(new Random().nextInt(900000) + 100000);
            Duration expiration = authProperties.getVerification().getPhoneCodeExpiration();
            createVerificationCode(user, "PHONE_VERIFICATION", code, expiration);
            phoneService.sendVerificationCode(user.getPhoneNumber(), code);
        }

        log.info("User registered successfully with ID: {}", user.getId());
        return new AuthResponse("User registered. Please verify your account.");
    }

    /**
     * Verifies a user's email address.
     *
     * @param request The email verification request containing email and code.
     * @return An {@link AuthResponse} confirming verification.
     */
    @Transactional
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        String email = request.email();
        if (email != null) {
            email = email.trim().toLowerCase();
        }

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with the provided email."));

        if (user.isEmailVerified()) {
            return new AuthResponse("Email is already verified.");
        }

        VerificationCode vc = verificationCodeRepository
                .findByUser_IdAndTypeAndCode(user.getId(), "EMAIL_VERIFICATION", request.code())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired email verification code."));

        if (vc.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Verification code has expired.");
        }

        user.setEmailVerified(true);
        userRepository.save(user);
        verificationCodeRepository.delete(vc);

        log.info("Email verified successfully for user: {}", email);
        return new AuthResponse("Email verified successfully.");
    }

    /**
     * Verifies a user's phone number.
     *
     * @param request The phone verification request containing phone number and code.
     * @return An {@link AuthResponse} confirming verification.
     */
    @Transactional
    public AuthResponse verifyPhone(VerifyPhoneRequest request) {
        User user = userRepository
                .findByPhoneNumber(request.phoneNumber())
                .orElseThrow(() -> new IllegalArgumentException("User not found with the provided phone number."));

        if (user.isPhoneVerified()) {
            return new AuthResponse("Phone is already verified.");
        }

        VerificationCode vc = verificationCodeRepository
                .findByUser_IdAndTypeAndCode(user.getId(), "PHONE_VERIFICATION", request.code())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired phone verification code."));

        if (vc.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Verification code has expired.");
        }

        user.setPhoneVerified(true);
        userRepository.save(user);
        verificationCodeRepository.delete(vc);

        log.info("Phone verified successfully for user: {}", request.phoneNumber());
        return new AuthResponse("Phone verified successfully.");
    }

    /**
     * Initiates the forgot password process.
     *
     * @param request The forgot password request containing the username.
     * @return An {@link AuthResponse} indicating the code has been sent.
     */
    @Transactional
    public AuthResponse forgotPassword(ForgotPasswordRequest request) {
        String identifier = request.username();
        if (identifier != null && identifier.contains("@")) {
            identifier = identifier.trim().toLowerCase();
        }

        final String finalIdentifier = identifier;
        User user = userRepository
                .findByEmail(finalIdentifier)
                .or(() -> userRepository.findByPhoneNumber(finalIdentifier))
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        String resetCode;
        Duration expiration;

        if (user.getEmail() != null) {
            resetCode = UUID.randomUUID().toString();
            expiration = authProperties.getVerification().getEmailLinkExpiration();
            long minutes = expiration.toMinutes();

            long displayValue = minutes;
            String displayUnit = authProperties.getVerification().getUnitMinutes();
            if (minutes >= 60 && minutes % 60 == 0) {
                displayValue = minutes / 60;
                displayUnit = authProperties.getVerification().getUnitHours();
            }

            if (authProperties.getNotifications().getMail().getProvider()
                    != AuthProperties.Notifications.Mail.Provider.NONE) {
                emailService.sendPasswordResetEmail(
                        user.getEmail(), user.getProfile().getFirstName(), resetCode, displayValue, displayUnit);
            }
        } else {
            resetCode = String.valueOf(new Random().nextInt(900000) + 100000);
            expiration = authProperties.getVerification().getPhoneCodeExpiration();

            if (authProperties.getNotifications().getPhone().getProvider()
                != AuthProperties.Notifications.Phone.Provider.NONE) {
                phoneService.sendVerificationCode(user.getPhoneNumber(), resetCode);
            }
        }

        createVerificationCode(user, "PASSWORD_RESET", resetCode, expiration);

        return new AuthResponse("Password reset code sent.");
    }

    /**
     * Resets the user's password.
     *
     * @param request The reset password request containing username, code, and new
     *                password.
     * @return An {@link AuthResponse} confirming the password reset.
     */
    @Transactional
    public AuthResponse resetPassword(ResetPasswordRequest request) {
        String identifier = request.username();
        if (identifier != null && identifier.contains("@")) {
            identifier = identifier.trim().toLowerCase();
        }

        final String finalIdentifier = identifier;
        User user = userRepository
                .findByEmail(finalIdentifier)
                .or(() -> userRepository.findByPhoneNumber(finalIdentifier))
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        VerificationCode vc = verificationCodeRepository
                .findByUser_IdAndTypeAndCode(user.getId(), "PASSWORD_RESET", request.code())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset code."));

        if (vc.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Reset code has expired.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        verificationCodeRepository.delete(vc);

        refreshTokenRepository.deleteByUser_Id(user.getId());

        return new AuthResponse("Password reset successfully.");
    }

    /**
     * Refreshes the access token using a refresh token.
     *
     * @param request The refresh token request.
     * @return An {@link AuthResponse} containing the new access token.
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String compositeToken = request.refreshToken();

        if (compositeToken == null || !compositeToken.contains(":")) {
            throw new IllegalArgumentException("Invalid refresh token format");
        }

        String[] parts = compositeToken.split(":");
        UUID tokenId = UUID.fromString(parts[0]);
        String rawToken = parts[1];

        RefreshToken tokenEntity = refreshTokenRepository
                .findById(tokenId)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token not found"));

        if (tokenEntity.getRevokedAt() != null) {
            throw new IllegalArgumentException("Refresh token has been revoked");
        }

        if (tokenEntity.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Refresh token expired");
        }

        if (!passwordEncoder.matches(rawToken, tokenEntity.getTokenHash())) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        User user = tokenEntity.getUser();
        if (!"active".equalsIgnoreCase(user.getStatus())) {
            throw new IllegalArgumentException("User is not active");
        }

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                user.getEmail() != null ? user.getEmail() : user.getPhoneNumber(),
                user.getPassword() != null ? user.getPassword() : "",
                true,
                true,
                true,
                true,
                Collections.emptyList());

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("roles", user.getRoles().stream().map(Role::getName).toList());
        extraClaims.put(
                "permissions",
                user.getRoles().stream()
                        .flatMap(role -> role.getPermissions().stream())
                        .map(Permission::getSlug)
                        .distinct()
                        .toList());
        String newJwt = jwtService.generateToken(extraClaims, userDetails);

        return new AuthResponse(newJwt, null);
    }

    /**
     * Retrieves all active sessions (refresh tokens) for a specific user.
     * Useful for showing a "Where you're logged in" list.
     *
     * @param userId The ID of the user.
     * @return A list of active session information.
     */
    @Transactional(readOnly = true)
    public List<SessionInfo> getActiveSessions(UUID userId) {
        return refreshTokenRepository.findAllByUser_Id(userId).stream()
                .filter(token ->
                        token.getRevokedAt() == null && token.getExpiresAt().isAfter(Instant.now()))
                .map(token -> new SessionInfo(
                        token.getId(),
                        token.getDeviceInfo(),
                        token.getIpAddress(),
                        token.getCreatedAt(),
                        token.getExpiresAt()))
                .toList();
    }

    /**
     * Revokes a specific session (refresh token) by ID.
     * Useful for a "Log out from this device" button.
     *
     * @param sessionId The ID of the session to revoke.
     * @param userId    The ID of the user who owns the session.
     */
    @Transactional
    public void revokeSession(UUID sessionId, UUID userId) {
        RefreshToken token = refreshTokenRepository
                .findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        if (!token.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Session does not belong to user");
        }

        refreshTokenRepository.delete(token);
        log.info("Session revoked. ID: {}", sessionId);
    }

    private void createVerificationCode(User user, String type, String code, Duration duration) {
        VerificationCode vc = new VerificationCode();
        vc.setUser(user);
        vc.setType(type);
        vc.setCode(code);
        vc.setExpiresAt(Instant.now().plus(duration));
        verificationCodeRepository.save(vc);
    }
}
