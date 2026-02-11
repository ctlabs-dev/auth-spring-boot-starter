package dev.ctlabs.starter.auth.application.validation;

import dev.ctlabs.starter.auth.autoconfigure.AuthProperties;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordConstraintValidator implements ConstraintValidator<ValidPassword, String> {

    private final AuthProperties authProperties;

    public PasswordConstraintValidator(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return true;
        }

        if (password.matches(authProperties.getPassword().getValidationRegex())) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(authProperties.getPassword().getValidationMessage())
                .addConstraintViolation();
        return false;
    }
}