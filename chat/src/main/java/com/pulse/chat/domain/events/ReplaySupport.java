package com.pulse.chat.domain.events;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public final class ReplaySupport {
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private ReplaySupport() {
    }

    public static int normalizeLimit(Integer requestedLimit) {
        if (requestedLimit == null || requestedLimit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(MAX_LIMIT, requestedLimit);
    }

    public static <ID, ENTITY> ENTITY loadClaimed(
            ID id,
            Function<ID, Optional<ENTITY>> finder,
            Predicate<ENTITY> isClaimedState
    ) {
        return finder.apply(id)
                .filter(isClaimedState)
                .orElse(null);
    }
}
