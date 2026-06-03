package com.pulse.chat.domain.chat_core.membership.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddGroupMemberRequest(
        @NotNull UUID userId
) {
}
