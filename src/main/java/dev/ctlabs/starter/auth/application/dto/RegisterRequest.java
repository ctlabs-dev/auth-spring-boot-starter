package dev.ctlabs.starter.auth.application.dto;

import dev.ctlabs.starter.auth.application.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object for user registration request.
 *
 * @param firstName   The first name of the user.
 * @param lastName    The last name of the user.
 * @param email       The email address of the user.
 * @param phoneNumber The phone number of the user.
 * @param password    The password for the user content.
 */
public record RegisterRequest(
        @NotBlank(message = "First name is required") String firstName,

        @NotBlank(message = "Last name is required") String lastName,

        @Email(message = "Invalid email format") String email,

        String phoneNumber,

        @NotBlank(message = "Password is required") @ValidPassword
        String password) {}
