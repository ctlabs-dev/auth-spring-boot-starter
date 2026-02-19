package dev.ctlabs.starter.auth.infrastructure.service.phone;

import lombok.extern.slf4j.Slf4j;

/**
 * No-operation phone sender strategy.
 * Used when phone notifications are disabled; logs the message attempt instead
 * of sending.
 */
@Slf4j
public class NoOpPhoneSenderStrategy implements PhoneSenderStrategy {
    @Override
    public void send(String to, String message) {
        log.warn("Phone sending is disabled (Provider: NONE). Message '{}' to '{}' was not sent.", message, to);
    }
}
