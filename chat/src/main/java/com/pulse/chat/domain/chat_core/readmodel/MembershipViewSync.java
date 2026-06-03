package com.pulse.chat.domain.chat_core.readmodel;

import com.pulse.chat.domain.chat_core.conversation.entity.ConversationEntity;
import com.pulse.chat.domain.chat_core.membership.entity.ConversationMemberEntity;
import com.pulse.chat.domain.chat_core.message.repository.ConversationReadStateRepository;
import com.pulse.chat.domain.chat_core.message.repository.MessageRepository;
import com.pulse.chat.domain.chat_core.readmodel.repository.ConversationListViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MembershipViewSync {

    private final ConversationViewSync conversationViewSync;

    private final ConversationListViewRepository conversationListViewRepository;
    private final ConversationReadStateRepository conversationReadStateRepository;
    private final MessageRepository messageRepository;

    public void syncForNewAddedMember(ConversationEntity conversation,
                                      ConversationMemberEntity member) {

        conversationViewSync.syncNewGroupConversation(conversation, List.of(member));
        backfillLatestVisibleState(conversation.getId(), member.getUserId());
    }

    public void syncForRemovedMember(UUID conversationId, UUID userId) {
        conversationListViewRepository.deleteByUserIdAndConversationId(userId, conversationId);
        conversationReadStateRepository.deleteByUserIdAndConversationId(userId, conversationId);
    }

    private void backfillLatestVisibleState(UUID conversationId, UUID userId) {
        var lastestMessage = messageRepository
                .findTopByConversationIdOrderByCreatedAtDesc(conversationId);

        lastestMessage.ifPresent(message ->
                conversationViewSync.backfillLatestVisibleStateForMember(userId, message));
    }
}
