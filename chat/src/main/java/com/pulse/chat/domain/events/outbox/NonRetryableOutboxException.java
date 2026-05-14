package com.pulse.chat.domain.events.outbox;

public class NonRetryableOutboxException extends RuntimeException {
    public NonRetryableOutboxException(String message, Throwable cause) {
        super(message, cause);
    }
}
