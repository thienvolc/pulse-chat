package com.pulse.chat.domain.presence;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserPresenceService {

    private static final String ONLINE_VALUE = "online";
    private static final String PRESENCE_KEY_PREFIX = "presence:user:";

    private final StringRedisTemplate redis;

    @Value("${app.realtime.presence-ttl-seconds:120}")
    private int presenceTtlSeconds;

    public void markOnline(UUID userId) {
        redis.opsForValue().set(presenceKey(userId), ONLINE_VALUE, Duration.ofSeconds(presenceTtlSeconds));
    }

    public boolean isOnline(UUID userId) {
        return Boolean.TRUE.equals(redis.hasKey(presenceKey(userId)));
    }

    private String presenceKey(UUID userId) {
        return PRESENCE_KEY_PREFIX + userId;
    }
}
