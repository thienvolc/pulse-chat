package com.pulse.chat.domain.presence;

import jakarta.annotation.Nullable;

public record OwnerInfo(
        @Nullable String instanceId,
        @Nullable Long updatedAtMs
) {
    public static OwnerInfo empty() {
        return new OwnerInfo(null, null);
    }
}
