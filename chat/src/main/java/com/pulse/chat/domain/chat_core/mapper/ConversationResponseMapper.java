package com.pulse.chat.domain.chat_core.mapper;

import com.pulse.chat.domain.chat_core.dto.ConversationResponse;
import com.pulse.chat.domain.chat_core.entity.ConversationEntity;
import com.pulse.chat.domain.chat_core.entity.ConversationType;
import com.pulse.chat.domain.chat_core.readmodel.ConversationListViewEntity;
import org.springframework.stereotype.Component;

@Component
public class ConversationResponseMapper {

    public ConversationResponse fromListView(
            ConversationListViewEntity row,
            ConversationType type,
            ConversationEntity conversation
    ) {
        return new ConversationResponse(
                row.getConversationId(),
                type,
                row.getCreatedAt(),
                row.getPeerUserId(),
                row.getPeerUsername(),
                row.getLastMessageSnippet(),
                row.getLastMessageAt(),
                conversation != null ? conversation.getOwnerId() : null,
                conversation != null ? conversation.getRoomName() : null
        );
    }
}
