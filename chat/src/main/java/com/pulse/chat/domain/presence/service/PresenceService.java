package com.pulse.chat.domain.presence.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@lombok.RequiredArgsConstructor
public class PresenceService {
    private static final Duration TTL = Duration.ofMinutes(2);
    private static final String ONLINE_VALUE = "online";
    private static final String PRESENCE_KEY_PREFIX = "presence:user:";
    private static final String OWNER_KEY_PREFIX = "presence:owner:user:";
    private static final String OWNER_INSTANCE_ID_FIELD = "instanceId";
    private static final String OWNER_UPDATED_AT_FIELD = "updatedAt";

    private final StringRedisTemplate redis;

    @Value("${app.realtime.instance-id:${random.uuid}}")
    private String instanceId;

    public void markOnline(UUID userId) {
        markOnlinePresence(userId);
        writeOwnerInfo(userId, OwnershipInfo.forCurrentInstance(instanceId, System.currentTimeMillis()));
    }

    public boolean isOnline(UUID userId) {
        return Boolean.TRUE.equals(redis.hasKey(presenceKey(userId)));
    }

    public boolean isOwnedByCurrentInstance(UUID userId) {
        return readOwnerInfo(userId)
                .instanceIdValue()
                .filter(instanceId::equals)
                .isPresent();
    }

    public Map<Object, Object> getOwnerInfo(UUID userId) {
        OwnershipInfo ownerInfo = readOwnerInfo(userId);
        Map<Object, Object> owner = new HashMap<>();
        ownerInfo.instanceIdValue().ifPresent(value -> owner.put(OWNER_INSTANCE_ID_FIELD, value));
        ownerInfo.updatedAtValue().ifPresent(value -> owner.put(OWNER_UPDATED_AT_FIELD, value));
        return owner;
    }

    private void markOnlinePresence(UUID userId) {
        redis.opsForValue().set(presenceKey(userId), ONLINE_VALUE, TTL);
    }

    private void writeOwnerInfo(UUID userId, OwnershipInfo ownerInfo) {
        String ownerKey = ownerKey(userId);
        redis.opsForHash().put(ownerKey, OWNER_INSTANCE_ID_FIELD, ownerInfo.instanceId());
        redis.opsForHash().put(ownerKey, OWNER_UPDATED_AT_FIELD, ownerInfo.updatedAt());
        redis.expire(ownerKey, TTL);
    }

    private OwnershipInfo readOwnerInfo(UUID userId) {
        Map<Object, Object> entries = redis.opsForHash().entries(ownerKey(userId));
        return new OwnershipInfo(
                stringValue(entries.get(OWNER_INSTANCE_ID_FIELD)),
                stringValue(entries.get(OWNER_UPDATED_AT_FIELD))
        );
    }

    private String presenceKey(UUID userId) {
        return PRESENCE_KEY_PREFIX + userId;
    }

    private String ownerKey(UUID userId) {
        return OWNER_KEY_PREFIX + userId;
    }

    private String stringValue(Object value) {
        return value != null ? value.toString() : null;
    }

    private record OwnershipInfo(String instanceId, String updatedAt) {
        private static OwnershipInfo forCurrentInstance(String instanceId, long updatedAtEpochMs) {
            return new OwnershipInfo(instanceId, String.valueOf(updatedAtEpochMs));
        }

        private java.util.Optional<String> instanceIdValue() {
            return java.util.Optional.ofNullable(instanceId);
        }

        private java.util.Optional<String> updatedAtValue() {
            return java.util.Optional.ofNullable(updatedAt);
        }
    }
}


