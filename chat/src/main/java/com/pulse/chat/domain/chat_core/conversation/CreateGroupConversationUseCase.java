package com.pulse.chat.domain.chat_core.conversation;

import com.pulse.chat.domain.chat_core.conversation.dto.response.ConversationResponse;
import com.pulse.chat.domain.chat_core.conversation.mapper.ConversationMapper;
import com.pulse.chat.domain.chat_core.conversation.entity.ConversationEntity;
import com.pulse.chat.domain.chat_core.membership.entity.ConversationMemberEntity;
import com.pulse.chat.domain.chat_core.membership.entity.ConversationMemberRole;
import com.pulse.chat.domain.chat_core.conversation.entity.ConversationType;
import com.pulse.chat.domain.chat_core.conversation.guard.ConversationGuard;
import com.pulse.chat.domain.chat_core.readmodel.ConversationViewSync;
import com.pulse.chat.domain.chat_core.membership.repository.ConversationMemberRepository;
import com.pulse.chat.domain.chat_core.conversation.repository.ConversationRepository;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

import static com.pulse.chat.domain.chat_core.membership.entity.ConversationMemberRole.MEMBER;
import static com.pulse.chat.domain.chat_core.membership.entity.ConversationMemberRole.OWNER;

@Service
@RequiredArgsConstructor
public class CreateGroupConversationUseCase {

    private final ConversationViewSync conversationViewSync;
    private final ConversationGuard conversationGuard;

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;

    private final ConversationMapper conversationMapper;

    @Transactional
    public ConversationResponse execute(UUID requesterId,
                                        @NotEmpty List<UUID> initialMemberUserIds,
                                        @Nullable String roomName) {

        conversationGuard.initialGroupHasEnoughMembersOrError(initialMemberUserIds);
        ConversationEntity conversation = createConversation(requesterId, roomName);

        Set<UUID> memberUserIds = new LinkedHashSet<>(initialMemberUserIds);
        memberUserIds.add(requesterId);
        List<ConversationMemberEntity> members = createGroupMembers(memberUserIds, conversation);

        conversationViewSync.syncNewGroupConversation(conversation, members);

        return conversationMapper.toResponse(conversation);
    }

    private ConversationEntity createConversation(UUID ownerId, @Nullable String roomName) {
        var normalizedName = normalizeRoomName(roomName);

        var conversation = ConversationEntity.builder()
                .type(ConversationType.GROUP)
                .ownerId(ownerId)
                .roomName(normalizedName)
                .createdAt(Instant.now())
                .build();
        return conversationRepository.save(conversation);
    }


    private List<ConversationMemberEntity> createGroupMembers(
            Set<UUID> memberUserIds, ConversationEntity conversation) {

        var members = memberUserIds.stream()
                .map(userId -> conversationMapper.toMemberEntity(
                        userId, conversation, resolveRole(userId, conversation.getOwnerId())))
                .toList();

        return memberRepository.saveAll(members);
    }

    private ConversationMemberRole resolveRole(UUID userId, UUID ownerId) {
        return userId.equals(ownerId) ? OWNER : MEMBER;
    }

    private String normalizeRoomName(@Nullable String roomName) {
        return Optional.ofNullable(roomName)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(null);
    }
}
