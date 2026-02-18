package dev.ctlabs.starter.auth.infrastructure.service.phone;

/**
 * Strategy interface for sending phone messages (SMS/WhatsApp).
 * Implementations define the logic for specific providers.
 */
public interface PhoneSenderStrategy {
    /**
     * Sends a phone message.
     *
     * @param to      The recipient's phone number.
     * @param message The message content.
     */
    void send(String to, String message);
}
