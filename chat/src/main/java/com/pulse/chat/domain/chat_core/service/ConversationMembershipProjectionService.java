package com.pulse.chat.domain.chat_core.service;

import com.pulse.chat.domain.chat_core.entity.ConversationEntity;
import com.pulse.chat.domain.chat_core.entity.ConversationMemberEntity;
import com.pulse.chat.domain.chat_core.readmodel.ConversationListProjectionService;
import com.pulse.chat.domain.chat_core.readmodel.ConversationListViewRepository;
import com.pulse.chat.domain.chat_core.repository.ConversationReadStateRepository;
import com.pulse.chat.domain.chat_core.repository.MessageRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@lombok.RequiredArgsConstructor
public class ConversationMembershipProjectionService {
    private final ConversationListProjectionService projectionService;
    private final ConversationListViewRepository conversationListViewRepository;
    private final ConversationReadStateRepository readStateRepository;
    private final MessageRepository messageRepository;

    public void initializeMemberProjection(ConversationEntity conversation, ConversationMemberEntity member) {
        projectionService.initializeConversation(
                conversation.getId(),
                conversation.getCreatedAt(),
                List.of(member)
        );
        backfillLatestVisibleState(conversation.getId(), member.getUserId());
    }

    public void removeMemberProjection(UUID conversationId, UUID userId) {
        conversationListViewRepository.deleteByUserIdAndConversationId(userId, conversationId);
        readStateRepository.deleteByUserIdAndConversationId(userId, conversationId);
    }

    private void backfillLatestVisibleState(UUID conversationId, UUID userId) {
        messageRepository.findTopByConversationIdOrderByCreatedAtDesc(conversationId)
                .ifPresent(message -> projectionService.backfillLatestVisibleStateForMember(
                        conversationId,
                        userId,
                        message.getContent(),
                        message.getCreatedAt()
                ));
    }
}
