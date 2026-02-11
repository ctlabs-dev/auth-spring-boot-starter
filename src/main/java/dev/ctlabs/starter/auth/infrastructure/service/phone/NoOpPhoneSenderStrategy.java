package dev.ctlabs.starter.auth.infrastructure.service.phone;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NoOpPhoneSenderStrategy implements PhoneSenderStrategy {
    @Override
    public void send(String to, String message) {
        log.warn("Phone sending is disabled (Provider: NONE). Message '{}' to '{}' was not sent.", message, to);
    }
}