package dev.ctlabs.starter.auth.application.service;

import dev.ctlabs.starter.auth.application.dto.AuthResponse;
import dev.ctlabs.starter.auth.application.dto.ForgotPasswordRequest;
import dev.ctlabs.starter.auth.application.dto.LoginRequest;
import dev.ctlabs.starter.auth.application.dto.RefreshTokenRequest;
import dev.ctlabs.starter.auth.application.dto.RegisterRequest;
import dev.ctlabs.starter.auth.application.dto.ResetPasswordRequest;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
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

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       VerificationCodeRepository verificationCodeRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager,
                       EmailService emailService,
                       PhoneService phoneService,
                       AuthProperties authProperties) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.verificationCodeRepository = verificationCodeRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.emailService = emailService;
        this.phoneService = phoneService;
        this.authProperties = authProperties;
    }

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
                new UsernamePasswordAuthenticationToken(identifier, request.password())
        );
        var userDetails = (UserDetails) authentication.getPrincipal();
        var jwt = jwtService.generateToken(userDetails);

        final String finalIdentifier = identifier;
        User user = userRepository.findByEmail(finalIdentifier)
                .or(() -> userRepository.findByPhoneNumber(finalIdentifier))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String rawRefreshToken = UUID.randomUUID().toString();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(passwordEncoder.encode(rawRefreshToken));
        refreshToken.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS)); // TODO: Parametrizar en AuthProperties
        refreshToken.setDeviceInfo(servletRequest.getHeader("User-Agent"));
        refreshToken.setIpAddress(servletRequest.getRemoteAddr());
        refreshToken = refreshTokenRepository.save(refreshToken);

        String compositeToken = refreshToken.getId().toString() + ":" + rawRefreshToken;

        log.info("User authenticated successfully: {}", identifier);
        return new AuthResponse(jwt, compositeToken);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email() != null ? request.email().trim().toLowerCase() : null;
        boolean hasEmail = email != null && !email.isBlank();
        boolean hasPhone = request.phoneNumber() != null && !request.phoneNumber().isBlank();

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
        Role role = roleRepository.findByName(defaultRoleName)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName(defaultRoleName);
                    return roleRepository.save(newRole);
                });
        user.getRoles().add(role);

        boolean isEmailProviderNone = authProperties.getNotifications().getMail().getProvider() == AuthProperties.Notifications.Mail.Provider.NONE;
        if (hasEmail && isEmailProviderNone) {
            user.setEmailVerified(true);
        }

        boolean isPhoneProviderNone = authProperties.getNotifications().getPhone().getProvider() == AuthProperties.Notifications.Phone.Provider.NONE;
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
            long expirationMinutes = authProperties.getVerification().getEmailLinkExpirationMinutes();

            long displayValue = expirationMinutes;
            String displayUnit = authProperties.getVerification().getUnitMinutes();
            if (expirationMinutes >= 60 && expirationMinutes % 60 == 0) {
                displayValue = expirationMinutes / 60;
                displayUnit = authProperties.getVerification().getUnitHours();
            }

            createVerificationCode(user, "EMAIL_VERIFICATION", code, expirationMinutes, ChronoUnit.MINUTES);
            emailService.sendVerificationEmail(user.getEmail(), profile.getFirstName(), code, displayValue, displayUnit);
        } else if (hasPhone && !isPhoneProviderNone) {
            String code = String.valueOf(new Random().nextInt(900000) + 100000);
            long expirationMinutes = authProperties.getVerification().getPhoneCodeExpirationMinutes();
            createVerificationCode(user, "PHONE_VERIFICATION", code, expirationMinutes, ChronoUnit.MINUTES);
            phoneService.sendVerificationCode(user.getPhoneNumber(), code);
        }

        log.info("User registered successfully with ID: {}", user.getId());
        return new AuthResponse("User registered. Please verify your account.");
    }

    @Transactional
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        String email = request.email();
        if (email != null) {
            email = email.trim().toLowerCase();
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with the provided email."));

        if (user.isEmailVerified()) {
            return new AuthResponse("Email is already verified.");
        }

        VerificationCode vc = verificationCodeRepository.findByUser_IdAndTypeAndCode(user.getId(), "EMAIL_VERIFICATION", request.code())
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

    @Transactional
    public AuthResponse verifyPhone(VerifyPhoneRequest request) {
        User user = userRepository.findByPhoneNumber(request.phoneNumber())
                .orElseThrow(() -> new IllegalArgumentException("User not found with the provided phone number."));

        if (user.isPhoneVerified()) {
            return new AuthResponse("Phone is already verified.");
        }

        VerificationCode vc = verificationCodeRepository.findByUser_IdAndTypeAndCode(user.getId(), "PHONE_VERIFICATION", request.code())
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

    @Transactional
    public AuthResponse forgotPassword(ForgotPasswordRequest request) {
        // TODO: Update logic for VerificationCode
        throw new UnsupportedOperationException("Update pending");
        /*
        var userOptional = userRepository.findByEmail(request.username())
                .or(() -> userRepository.findByPhoneNumber(request.username()));

        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("User not found.");
        }

        var user = userOptional.get();
        var verification = user.getVerification();
        if (verification == null) {
            verification = new Verification();
            user.setVerification(verification);
        }

        String resetCode = user.getEmail() != null ? UUID.randomUUID().toString() : String.valueOf(new Random().nextInt(1000000));
        verification.setResetCode(resetCode);
        userRepository.save(user);

        if (user.getEmail() != null && authProperties.getNotifications().getMail().getProvider() != AuthProperties.Notifications.Mail.Provider.NONE) {
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), resetCode, 1, "hora");
        } else if (user.getPhoneNumber() != null && authProperties.getNotifications().getPhone().getProvider() != AuthProperties.Notifications.Phone.Provider.NONE) {
            phoneVerificationService.sendVerificationCode(user.getPhoneNumber(), resetCode);
        }

        return new AuthResponse("Password reset code sent.");
        */
    }

    @Transactional
    public AuthResponse resetPassword(ResetPasswordRequest request) {
        // TODO: Update logic for VerificationCode
        throw new UnsupportedOperationException("Update pending");
        /*
        var user = userRepository.findByEmail(request.username())
                .or(() -> userRepository.findByPhoneNumber(request.username()))
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        if (user.getVerification() == null || user.getVerification().getResetCode() == null ||
                !user.getVerification().getResetCode().equals(request.code())) {
            throw new IllegalArgumentException("Invalid or expired reset code.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.getVerification().setResetCode(null);
        userRepository.save(user);

        return new AuthResponse("Password reset successfully.");
        */
    }

    @Transactional
    public AuthResponse updateRole(String username, String newRole) {
        // TODO: Update logic for Role entity
        throw new UnsupportedOperationException("Update pending");
        /*
        var user = userRepository.findByEmail(username)
                .or(() -> userRepository.findByPhoneNumber(username))
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        user.setRole(newRole);
        userRepository.save(user);
        log.info("Role updated for user: {} to {}", username, newRole);

        return new AuthResponse("Role updated successfully.");
        */
    }

    private void createVerificationCode(User user, String type, String code, long duration, ChronoUnit unit) {
        VerificationCode vc = new VerificationCode();
        vc.setUser(user);
        vc.setType(type);
        vc.setCode(code);
        vc.setExpiresAt(Instant.now().plus(duration, unit));
        verificationCodeRepository.save(vc);
    }

    /**
     * Changes the status of a user (e.g., "active", "suspended", "banned").
     * This method is intended to be used by administrative components.
     */
    @Transactional
    public void changeUserStatus(UUID userId, String newStatus) {
        User user = userRepository.findById(userId)
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
     * This preserves the user record for referential integrity but prevents login.
     */
    @Transactional
    public void deleteUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        user.setStatus("archived");
        userRepository.save(user);
        refreshTokenRepository.deleteByUser_Id(userId);
        log.info("User soft-deleted (status set to archived). ID: {}", userId);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String compositeToken = request.refreshToken();
        if (compositeToken == null || !compositeToken.contains(":")) {
            throw new IllegalArgumentException("Invalid refresh token format");
        }

        String[] parts = compositeToken.split(":");
        UUID tokenId = UUID.fromString(parts[0]);
        String rawToken = parts[1];

        RefreshToken tokenEntity = refreshTokenRepository.findById(tokenId)
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

        List<GrantedAuthority> authorities = new ArrayList<>();
        for (Role role : user.getRoles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
            for (Permission permission : role.getPermissions()) {
                authorities.add(new SimpleGrantedAuthority(permission.getSlug()));
            }
        }

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                user.getEmail() != null ? user.getEmail() : user.getPhoneNumber(),
                user.getPassword(),
                true, true, true, true,
                authorities
        );

        String newJwt = jwtService.generateToken(userDetails);

        return new AuthResponse(newJwt, null);
    }
}
