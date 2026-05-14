package com.pulse.chat.domain.chat_core.service;

import com.pulse.chat.domain.chat_core.entity.ConversationMemberEntity;
import com.pulse.chat.domain.chat_core.policy.ConversationAccessPolicyService;
import com.pulse.chat.domain.chat_core.repository.ConversationMemberRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@lombok.RequiredArgsConstructor
public class ConversationMembershipQueryService {
    private final ConversationAccessPolicyService accessPolicy;
    private final ConversationMemberRepository memberRepository;

    public List<UUID> listMembers(UUID requesterId, UUID conversationId) {
        accessPolicy.requireMember(conversationId, requesterId);
        return memberRepository.findByConversationId(conversationId).stream()
                .map(ConversationMemberEntity::getUserId)
                .toList();
    }
}
