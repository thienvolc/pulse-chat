package com.pulse.chat.domain.presence;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class InstanceOwnershipService {

    private static final String OWNER_KEY_PREFIX = "presence:owner:user:";
    private static final String INSTANCE_ID_FIELD = "instanceId";
    private static final String UPDATED_AT_MS_FIELD = "updatedAtMs";

    private final StringRedisTemplate redis;
    private final String instanceId;
    private final int ownerTtlSeconds;

    public InstanceOwnershipService(
            StringRedisTemplate redis,
            @Value("${app.realtime.instance-id:${random.uuid}}") String instanceId,
            @Value("${app.realtime.presence-ttl-seconds:120}") int ownerTtlSeconds) {
        this.redis = redis;
        this.instanceId = instanceId;
        this.ownerTtlSeconds = ownerTtlSeconds;
    }

    public void claimOwnership(UUID userId) {
        String ownerKey = ownerKey(userId);
        redis.opsForHash().putAll(ownerKey, Map.of(
                INSTANCE_ID_FIELD, instanceId,
                UPDATED_AT_MS_FIELD, String.valueOf(System.currentTimeMillis())
        ));
        redis.expire(ownerKey, Duration.ofSeconds(ownerTtlSeconds));
    }

    public boolean isOwnedByCurrentInstance(UUID userId) {
        String storeInstanceId = getOwnerInstanceId(userId);
        return instanceId.equals(storeInstanceId);
    }

    public List<UUID> filterOwnedByCurrentInstance(List<UUID> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }

        List<String> ownerInstanceIds = findOwnerInstanceIds(userIds);
        return filterMatchingUsers(userIds, ownerInstanceIds);
    }

    public OwnerInfo getOwnerInfo(UUID userId) {
        Map<Object, Object> entries = redis.opsForHash().entries(ownerKey(userId));

        if (entries.isEmpty()) {
            return OwnerInfo.empty();
        }

        String storedInstanceId = stringOrNull(entries.get(INSTANCE_ID_FIELD));
        Long updatedAtMs = parseLongOrNull(entries.get(UPDATED_AT_MS_FIELD));

        return new OwnerInfo(storedInstanceId, updatedAtMs);
    }

    private String getOwnerInstanceId(UUID userId) {
        Object value = redis.opsForHash().get(ownerKey(userId), INSTANCE_ID_FIELD);
        return stringOrNull(value);
    }

    private List<String> findOwnerInstanceIds(List<UUID> userIds) {
        RedisSerializer<String> stringSerializer = redis.getStringSerializer();
        byte[] instanceIdField = stringSerializer.serialize(INSTANCE_ID_FIELD);

        List<Object> results = redis.executePipelined((RedisCallback<Object>) connection -> {
                    for (UUID userId : userIds) {
                        byte[] ownerKey = stringSerializer.serialize(ownerKey(userId));
                        connection.hashCommands().hGet(ownerKey, instanceIdField);
                    }

                    return null;
                }
        );

        return results.stream()
                .map(this::toNullableString)
                .toList();
    }

    private List<UUID> filterMatchingUsers(List<UUID> userIds, List<String> ownerInstanceIds) {
        List<UUID> ownedUserIds = new ArrayList<>();

        for (int i = 0; i < userIds.size(); i++) {
            if (instanceId.equals(ownerInstanceIds.get(i))) {
                ownedUserIds.add(userIds.get(i));
            }
        }

        return ownedUserIds;
    }

    private String ownerKey(UUID userId) {
        return OWNER_KEY_PREFIX + userId;
    }

    private String toNullableString(Object value) {
        return value == null ? null : new String((byte[]) value);
    }

    private String stringOrNull(@Nullable Object value) {
        return value == null ? null : value.toString();
    }

    private Long parseLongOrNull(@Nullable Object value) {
        return value == null ? null : Long.parseLong(value.toString());
    }
}
