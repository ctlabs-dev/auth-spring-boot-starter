package dev.ctlabs.starter.auth.infrastructure.service.phone;

public interface PhoneSenderStrategy {
    void send(String to, String message);
}