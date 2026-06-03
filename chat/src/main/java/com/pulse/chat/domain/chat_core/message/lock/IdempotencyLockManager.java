package com.pulse.chat.domain.chat_core.message.lock;

import java.util.function.Supplier;

public interface IdempotencyLockManager {
    <T> T executeWithLock(String lockKey, Supplier<T> action);
}
