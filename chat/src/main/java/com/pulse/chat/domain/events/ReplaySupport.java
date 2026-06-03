package com.pulse.chat.domain.events;

import jakarta.annotation.Nullable;

public final class ReplaySupport {
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private ReplaySupport() {
    }

    public static int normalizeLimit(@Nullable Integer requestedLimit) {
        if (requestedLimit == null || requestedLimit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(MAX_LIMIT, requestedLimit);
    }
}
