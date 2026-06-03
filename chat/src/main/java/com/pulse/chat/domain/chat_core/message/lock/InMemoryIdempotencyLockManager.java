package com.pulse.chat.domain.chat_core.message.lock;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

@Component
public class InMemoryIdempotencyLockManager implements IdempotencyLockManager {

    private final ConcurrentMap<String, Object> locks = new ConcurrentHashMap<>();

    @Override
    public <T> T executeWithLock(String lockKey, Supplier<T> action) {
        Object lock = locks.computeIfAbsent(lockKey, k -> new Object());
        synchronized (lock) {
            try {
                return action.get();
            } finally {
                locks.remove(lockKey, lock);
            }
        }
    }
}
