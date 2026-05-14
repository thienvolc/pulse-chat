package com.pulse.chat.domain.chat_core.service;

public interface IdempotencyLockManager {
    <T> T executeWithLock(String lockKey, java.util.function.Supplier<T> action);
}
