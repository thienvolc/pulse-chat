package com.pulse.chat.domain.chat_core.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateGroupConversationRequest(
        @NotEmpty List<@NotNull UUID> initialMemberUserIds,
        @Size(max = 120) String roomName
) {
}
