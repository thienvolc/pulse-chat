package com.pulse.chat.domain.chat_core.message.guard;

import com.pulse.chat.app.aop.BusinessException;
import com.pulse.chat.domain.common.constant.ResponseCode;
import com.pulse.chat.infrastructure.config.prop.MessagePolicyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class MessageRateLimiter {

    private final MessagePolicyProperties props;
    private final StringRedisTemplate redis;
    private final Map<String, WindowCounter> localCounters = new ConcurrentHashMap<>();

    public void enforce(String key) {
        try {
            enforceOnRedis(key);
        } catch (DataAccessException ignored) {
            enforceLocally(key);
        }
    }

    private void enforceOnRedis(String key) {
        long count = Optional.ofNullable(redis.opsForValue().increment(key)).orElse(0L);
        if (count == 1L) {
            redis.expire(key, Duration.ofSeconds(props.windowSeconds()));
        }
        rateLimitIsNotExceededOrError(count);
    }

    private void enforceLocally(String key) {
        WindowCounter counter = localCounters.computeIfAbsent(
                key, ignored -> WindowCounter.create(props.windowSeconds()));

        int count = counter.incrementAndGet();
        rateLimitIsNotExceededOrError(count);
    }


    private void rateLimitIsNotExceededOrError(long count) {
        if (count > props.maxMessagesPerWindow()) {
            throw new BusinessException(ResponseCode.RATE_LIMIT_EXCEEDED,
                    props.maxMessagesPerWindow(), props.windowSeconds());
        }
    }
}
