package dev.ctlabs.starter.auth.infrastructure.service;

import dev.ctlabs.starter.auth.infrastructure.service.phone.PhoneSenderStrategy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service for handling phone operations.
 * Uses the configured {@link PhoneSenderStrategy} to send messages.
 */
@Service
public class PhoneService {

    private final PhoneSenderStrategy phoneSenderStrategy;

    public PhoneService(PhoneSenderStrategy phoneSenderStrategy) {
        this.phoneSenderStrategy = phoneSenderStrategy;
    }

    @Async
    public void sendVerificationCode(String to, String code) {
        String message = "Your verification code is: " + code;
        phoneSenderStrategy.send(to, message);
    }
}
