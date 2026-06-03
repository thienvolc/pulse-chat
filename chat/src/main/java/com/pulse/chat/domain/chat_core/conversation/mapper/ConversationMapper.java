package com.pulse.chat.domain.chat_core.conversation.mapper;

import com.pulse.chat.domain.chat_core.conversation.dto.response.ConversationResponse;
import com.pulse.chat.domain.chat_core.conversation.entity.ConversationEntity;
import com.pulse.chat.domain.chat_core.membership.entity.ConversationMemberEntity;
import com.pulse.chat.domain.chat_core.membership.entity.ConversationMemberRole;
import com.pulse.chat.domain.chat_core.readmodel.entity.ConversationListViewEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ConversationMapper {

    public ConversationResponse toResponse(ConversationEntity conversation) {
        return ConversationResponse.created(
                conversation.getId(),
                conversation.getType(),
                conversation.getCreatedAt(),
                conversation.getOwnerId(),
                conversation.getRoomName()
        );
    }

    public ConversationResponse fromListView(
            ConversationListViewEntity row,
            ConversationEntity conversation) {

        return new ConversationResponse(
                row.getConversationId(),
                conversation.getType(),
                row.getCreatedAt(),
                row.getPeerUserId(),
                row.getPeerUsername(),
                row.getLastMessageSnippet(),
                row.getLastMessageAt(),
                conversation.getOwnerId(),
                conversation.getRoomName()
        );
    }

    public ConversationMemberEntity toMemberEntity(UUID userId,
                                                   ConversationEntity conversation,
                                                   ConversationMemberRole role) {

        return ConversationMemberEntity.builder()
                .conversationId(conversation.getId())
                .userId(userId)
                .role(role)
                .joinedAt(conversation.getCreatedAt())
                .build();
    }
}
