package com.pulse.chat.domain.chat_core.membership;

import com.pulse.chat.domain.chat_core.membership.entity.ConversationMemberEntity;
import com.pulse.chat.domain.chat_core.conversation.guard.ConversationGuard;
import com.pulse.chat.domain.chat_core.membership.repository.ConversationMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetGroupMembersUseCase {

    private final ConversationGuard conversationGuard;
    private final ConversationMemberRepository memberRepository;

    public List<UUID> execute(UUID requesterId, UUID conversationId) {
        conversationGuard.memberBelongsToConversationOrError(requesterId, conversationId);

        return memberRepository.findByConversationId(conversationId).stream()
                .map(ConversationMemberEntity::getUserId)
                .toList();
    }
}
