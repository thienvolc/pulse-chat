package com.pulse.chat.domain.chat_core.policy;

import com.pulse.chat.app.aop.BusinessException;
import com.pulse.chat.domain.common.constant.ResponseCode;
import com.pulse.chat.infrastructure.config.prop.MessagePolicyProperties;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@lombok.RequiredArgsConstructor
public class MessagePolicyService {

    private final MessagePolicyProperties properties;
    private final StringRedisTemplate redis;
    private final Map<String, WindowCounter> localCounters = new ConcurrentHashMap<>();

    

    public String validateAndNormalize(String userKey, String content) {
        String normalized = content == null ? "" : content.trim();
        validateLength(normalized);
        enforceDuplicateCooldown(userKey, normalized);
        enforceRateLimit(userKey);
        return normalized;
    }

    private void validateLength(String content) {
        if (content.length() < properties.minLength() || content.length() > properties.maxLength()) {
            throw new BusinessException(ResponseCode.INVALID_MESSAGE_CONTENT,
                    properties.minLength(), properties.maxLength());
        }
    }

    private void enforceDuplicateCooldown(String userKey, String content) {
        String key = "policy:dup:" + userKey;
        try {
            String latest = redis.opsForValue().get(key);
            if (content.equals(latest)) {
                throw new BusinessException(ResponseCode.MESSAGE_DUPLICATED_TOO_FAST);
            }
            redis.opsForValue().set(key, content, Duration.ofSeconds(properties.duplicateCooldownSeconds()));
            return;
        } catch (DataAccessException ignored) {
            // fallback local
        }

        String localKey = key;
        WindowCounter counter = localCounters.computeIfAbsent(localKey, k -> WindowCounter.create(properties.duplicateCooldownSeconds()));
        if (counter.samePayload(content)) {
            throw new BusinessException(ResponseCode.MESSAGE_DUPLICATED_TOO_FAST);
        }
        counter.setPayload(content);
    }

    private void enforceRateLimit(String userKey) {
        String key = "policy:rate:" + userKey;
        try {
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redis.expire(key, Duration.ofSeconds(properties.windowSeconds()));
            }
            if (count != null && count > properties.maxMessagesPerWindow()) {
                throw new BusinessException(ResponseCode.RATE_LIMIT_EXCEEDED,
                        properties.maxMessagesPerWindow(), properties.windowSeconds());
            }
            return;
        } catch (DataAccessException ignored) {
            // fallback local
        }

        WindowCounter counter = localCounters.computeIfAbsent(key, k -> WindowCounter.create(properties.windowSeconds()));
        int value = counter.incrementAndGet();
        if (value > properties.maxMessagesPerWindow()) {
            throw new BusinessException(ResponseCode.RATE_LIMIT_EXCEEDED,
                    properties.maxMessagesPerWindow(), properties.windowSeconds());
        }
    }

    private static final class WindowCounter {
        private volatile long expiresAtMillis;
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile String payload;
        private final int ttlSeconds;

        private WindowCounter(int ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
            this.expiresAtMillis = System.currentTimeMillis() + ttlSeconds * 1000L;
        }

        static WindowCounter create(int ttlSeconds) {
            return new WindowCounter(ttlSeconds);
        }

        int incrementAndGet() {
            resetIfExpired();
            return count.incrementAndGet();
        }

        boolean samePayload(String candidate) {
            resetIfExpired();
            return candidate.equals(payload);
        }

        void setPayload(String payload) {
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
}


