package com.pulse.chat.domain.chat_core.conversation;

import com.pulse.chat.domain.chat_core.conversation.dto.response.ConversationResponse;
import com.pulse.chat.domain.chat_core.conversation.entity.ConversationEntity;
import com.pulse.chat.domain.chat_core.conversation.entity.ConversationType;
import com.pulse.chat.domain.chat_core.conversation.guard.ConversationGuard;
import com.pulse.chat.domain.chat_core.conversation.mapper.ConversationMapper;
import com.pulse.chat.domain.chat_core.conversation.repository.ConversationRepository;
import com.pulse.chat.domain.chat_core.membership.entity.ConversationMemberEntity;
import com.pulse.chat.domain.chat_core.membership.repository.ConversationMemberRepository;
import com.pulse.chat.domain.chat_core.readmodel.ConversationViewSync;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.pulse.chat.domain.chat_core.membership.entity.ConversationMemberRole.MEMBER;

@Service
@RequiredArgsConstructor
public class CreateDirectConversationUseCase {

    private final ConversationViewSync conversationViewSync;

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final ConversationGuard conversationGuard;

    private final ConversationMapper conversationMapper;

    @Transactional
    public ConversationResponse execute(UUID requesterId, UUID targetUserId) {
        conversationGuard.directConversationDoesNotExistsOrError(requesterId, targetUserId);

        ConversationEntity conversation = createConversation();
        List<ConversationMemberEntity> members = createDirectMembers(
                List.of(requesterId, targetUserId), conversation);

        syncNewConversationView(conversation, members);

        return conversationMapper.toResponse(conversation);
    }

    private ConversationEntity createConversation() {
        var conversation = ConversationEntity.builder()
                .type(ConversationType.DIRECT)
                .ownerId(null)
                .roomName(null)
                .createdAt(Instant.now())
                .build();
        return conversationRepository.save(conversation);
    }

    private void syncNewConversationView(ConversationEntity conversation,
                                         List<ConversationMemberEntity> members) {

        if (members.getFirst().isSelfConversation(members.getLast())) {
            conversationViewSync.syncNewSelfConversation(conversation, members.getFirst());
        } else {
            conversationViewSync.syncNewDirectConversation(conversation, members);
        }
    }

    private List<ConversationMemberEntity> createDirectMembers(
            List<UUID> memberUserIds, ConversationEntity conversation) {

        var members = memberUserIds.stream()
                .map(userId -> conversationMapper.toMemberEntity(userId, conversation, MEMBER))
                .toList();

        return memberRepository.saveAll(members);
    }
}
