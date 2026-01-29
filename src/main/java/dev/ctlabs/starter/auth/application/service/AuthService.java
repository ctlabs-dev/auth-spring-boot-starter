package dev.ctlabs.starter.auth.application.service;

import dev.ctlabs.starter.auth.application.dto.AuthResponse;
import dev.ctlabs.starter.auth.application.dto.ForgotPasswordRequest;
import dev.ctlabs.starter.auth.application.dto.LoginRequest;
import dev.ctlabs.starter.auth.application.dto.RegisterRequest;
import dev.ctlabs.starter.auth.application.dto.ResetPasswordRequest;
import dev.ctlabs.starter.auth.application.dto.VerifyEmailRequest;
import dev.ctlabs.starter.auth.application.dto.VerifyPhoneRequest;
import dev.ctlabs.starter.auth.application.mapper.UserMapper;
import dev.ctlabs.starter.auth.autoconfigure.AuthProperties;
import dev.ctlabs.starter.auth.domain.model.Verification;
import dev.ctlabs.starter.auth.domain.repository.UserRepository;
import dev.ctlabs.starter.auth.infrastructure.security.JwtService;
import dev.ctlabs.starter.auth.infrastructure.service.EmailService;
import dev.ctlabs.starter.auth.infrastructure.service.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;
    private final EmailService emailService;
    private final SmsService smsService;
    private final AuthProperties authProperties;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager,
                       UserMapper userMapper,
                       EmailService emailService,
                       SmsService smsService,
                       AuthProperties authProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userMapper = userMapper;
        this.emailService = emailService;
        this.smsService = smsService;
        this.authProperties = authProperties;
    }

    public AuthResponse login(LoginRequest request) {
        String identifier = request.username();

        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Email or phone number must be provided for login.");
        }

        log.info("Login attempt for user: {}", identifier);
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(identifier, request.password())
        );
        var userDetails = (UserDetails) authentication.getPrincipal();
        var jwt = jwtService.generateToken(userDetails);
        log.info("User authenticated successfully: {}", identifier);
        return new AuthResponse(jwt);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        boolean hasEmail = request.email() != null && !request.email().isBlank();
        boolean hasPhone = request.phoneNumber() != null && !request.phoneNumber().isBlank();

        log.info("Registration attempt. Email: {}, Phone: {}", request.email(), request.phoneNumber());

        if (!hasEmail && !hasPhone) {
            throw new IllegalArgumentException("At least one contact method (email or phone) must be provided.");
        }

        if (hasEmail && userRepository.findByEmail(request.email()).isPresent()) {
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

        var user = userMapper.toEntity(request);

        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(authProperties.getDefaultRole());

        var verification = new Verification();
        if (hasEmail) verification.setEmailVerificationCode(UUID.randomUUID().toString());
        if (hasPhone) verification.setPhoneVerificationCode(String.valueOf(new Random().nextInt(1000000)));
        user.setVerification(verification);

        userRepository.save(user);

        if (hasEmail && authProperties.getNotifications().getMail().isEnabled()) {
            emailService.sendVerificationEmail(user.getEmail(), user.getFirstName(), verification.getEmailVerificationCode());
        }

        if (hasPhone && (authProperties.getNotifications().getSms().isEnabled() || authProperties.getNotifications().getWhatsapp().isEnabled())) {
            smsService.sendVerificationCode(user.getPhoneNumber(), verification.getPhoneVerificationCode(), authProperties.getNotifications().getWhatsapp().isEnabled(), authProperties.getNotifications().getSms().isEnabled());
        }

        log.info("User registered successfully with ID: {}", user.getId());
        return new AuthResponse("User registered. Please verify your account.");
    }

    @Transactional
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        var user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("User not found with the provided email."));

        var verification = user.getVerification();
        if (verification == null || verification.getEmailVerificationCode() == null ||
                !verification.getEmailVerificationCode().equals(request.code())) {
            throw new IllegalArgumentException("Invalid or expired email verification code.");
        }

        verification.setEmailVerified(true);
        verification.setEmailVerificationCode(null);
        userRepository.save(user);

        log.info("Email verified successfully for user: {}", request.email());
        return new AuthResponse("Email verified successfully.");
    }

    @Transactional
    public AuthResponse verifyPhone(VerifyPhoneRequest request) {
        var user = userRepository.findByPhoneNumber(request.phoneNumber())
                .orElseThrow(() -> new IllegalArgumentException("User not found with the provided phone number."));

        var verification = user.getVerification();
        if (verification == null || verification.getPhoneVerificationCode() == null ||
                !verification.getPhoneVerificationCode().equals(request.code())) {
            throw new IllegalArgumentException("Invalid or expired phone verification code.");
        }

        verification.setPhoneVerified(true);
        verification.setPhoneVerificationCode(null);
        userRepository.save(user);

        log.info("Phone verified successfully for user: {}", request.phoneNumber());
        return new AuthResponse("Phone verified successfully.");
    }

    @Transactional
    public AuthResponse forgotPassword(ForgotPasswordRequest request) {
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

        if (user.getEmail() != null && authProperties.getNotifications().getMail().isEnabled()) {
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), resetCode);
        } else if (user.getPhoneNumber() != null && (authProperties.getNotifications().getSms().isEnabled() || authProperties.getNotifications().getWhatsapp().isEnabled())) {
            smsService.sendPasswordResetCode(user.getPhoneNumber(), resetCode, authProperties.getNotifications().getWhatsapp().isEnabled(), authProperties.getNotifications().getSms().isEnabled());
        }

        return new AuthResponse("Password reset code sent.");
    }

    @Transactional
    public AuthResponse resetPassword(ResetPasswordRequest request) {
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
    }

    @Transactional
    public AuthResponse updateRole(String username, String newRole) {
        var user = userRepository.findByEmail(username)
                .or(() -> userRepository.findByPhoneNumber(username))
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        user.setRole(newRole);
        userRepository.save(user);
        log.info("Role updated for user: {} to {}", username, newRole);

        return new AuthResponse("Role updated successfully.");
    }
}
