package com.pulse.chat.domain.chat_core.policy;

import com.pulse.chat.app.aop.BusinessException;
import com.pulse.chat.domain.chat_core.entity.ConversationType;
import com.pulse.chat.domain.common.constant.ResponseCode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class ConversationCreationPolicy {

    public Set<UUID> resolveDirectMembers(UUID requesterId, UUID targetUserId) {
        if (targetUserId == null || requesterId.equals(targetUserId)) {
            throw new BusinessException(ResponseCode.INVALID_DIRECT_CONVERSATION_PARTICIPANTS);
        }
        return new LinkedHashSet<>(List.of(requesterId, targetUserId));
    }

    public Set<UUID> resolveGroupMembers(UUID requesterId, List<UUID> initialMemberUserIds) {
        Set<UUID> members = new LinkedHashSet<>();
        members.add(requesterId);
        if (initialMemberUserIds != null) {
            members.addAll(initialMemberUserIds);
        }
        if (members.size() < 2) {
            throw new BusinessException(ResponseCode.CONVERSATION_GROUP_REQUIRED);
        }
        return members;
    }

    public void validateMetadata(ConversationType type, UUID ownerId, String roomName) {
        if (type == ConversationType.DIRECT && (ownerId != null || hasText(roomName))) {
            throw new BusinessException(ResponseCode.INVALID_CONVERSATION_CREATE_REQUEST);
        }
        if (type == ConversationType.GROUP && ownerId == null) {
            throw new BusinessException(ResponseCode.INVALID_CONVERSATION_CREATE_REQUEST);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
