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
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class MessageDeduplicator {

    private final MessagePolicyProperties props;
    private final StringRedisTemplate redis;
    private final Map<String, WindowCounter> localCounters = new ConcurrentHashMap<>();

    public void enforceCooldown(String key, String content) {
        try {
            enforceOnRedis(key, content);
        } catch (DataAccessException ignored) {
            enforceLocally(key, content);
        }
    }

    private void enforceOnRedis(String key, String content) {
        String lastContent = redis.opsForValue().get(key);

        contentDoesNotDuplicateOrError(content, lastContent);

        redis.opsForValue().set(key, content, Duration.ofSeconds(props.duplicateCooldownSeconds()));
    }

    private void enforceLocally(String key, String content) {
        WindowCounter counter = localCounters.computeIfAbsent(
                key, ignored -> WindowCounter.create(props.duplicateCooldownSeconds()));

        contentDoesNotDuplicateOrError(content, counter.getPayload());

        counter.setPayload(content);
    }

    private void contentDoesNotDuplicateOrError(String content, String candidate) {
        if (content.equals(candidate)) {
            throw new BusinessException(ResponseCode.MESSAGE_DUPLICATED_TOO_FAST);
        }
    }
}
