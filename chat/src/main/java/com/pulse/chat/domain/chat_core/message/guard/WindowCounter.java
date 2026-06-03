package com.pulse.chat.domain.chat_core.message.guard;

import java.util.concurrent.atomic.AtomicInteger;

public final class WindowCounter {
    private volatile long expiresAtMillis;
    private final AtomicInteger count = new AtomicInteger(0);
    private volatile String payload;
    private final int ttlSeconds;

    private WindowCounter(int ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
        this.expiresAtMillis = System.currentTimeMillis() + ttlSeconds * 1000L;
    }

    public static WindowCounter create(int ttlSeconds) {
        return new WindowCounter(ttlSeconds);
    }

    public int incrementAndGet() {
        resetIfExpired();
        return count.incrementAndGet();
    }

    public String getPayload() {
        resetIfExpired();
        return payload;
    }

    public void setPayload(String payload) {
        resetIfExpired();
        this.payload = payload;
    }

    private void resetIfExpired() {
        long now = System.currentTimeMillis();
        if (now > expiresAtMillis) {
            count.set(0);
            payload = null;
            expiresAtMillis = now + ttlSeconds * 1000L;
        }
    }
}