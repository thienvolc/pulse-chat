package com.pulse.chat.domain.chat_core.service;

import com.pulse.chat.domain.chat_core.dto.ConversationResponse;
import com.pulse.chat.domain.chat_core.entity.ConversationEntity;
import com.pulse.chat.domain.chat_core.entity.ConversationMemberEntity;
import com.pulse.chat.domain.chat_core.entity.ConversationMemberRole;
import com.pulse.chat.domain.chat_core.entity.ConversationType;
import com.pulse.chat.domain.chat_core.policy.ConversationCreationPolicy;
import com.pulse.chat.domain.chat_core.readmodel.ConversationListProjectionService;
import com.pulse.chat.domain.chat_core.repository.ConversationMemberRepository;
import com.pulse.chat.domain.chat_core.repository.ConversationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@lombok.RequiredArgsConstructor
public class ConversationCommandService {
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final ConversationListProjectionService projectionService;
    private final ConversationCreationPolicy creationPolicy;

    @Transactional
    public ConversationResponse createDirectConversation(UUID requesterId, UUID targetUserId) {
        Set<UUID> members = creationPolicy.resolveDirectMembers(requesterId, targetUserId);
        ConversationEntity conversation = saveConversation(ConversationType.DIRECT, null, null);
        List<ConversationMemberEntity> savedMembers = saveMembers(
                conversation.getId(),
                members,
                conversation.getType(),
                conversation.getOwnerId(),
                conversation.getCreatedAt()
        );
        projectionService.initializeConversation(conversation.getId(), conversation.getCreatedAt(), savedMembers);
        return ConversationResponse.created(
                conversation.getId(),
                conversation.getType(),
                conversation.getCreatedAt(),
                conversation.getOwnerId(),
                conversation.getRoomName()
        );
    }

    @Transactional
    public ConversationResponse createGroupConversation(UUID requesterId, List<UUID> initialMemberUserIds, String roomName) {
        Set<UUID> members = creationPolicy.resolveGroupMembers(requesterId, initialMemberUserIds);
        ConversationEntity conversation = saveConversation(ConversationType.GROUP, requesterId, normalizeRoomName(roomName));
        List<ConversationMemberEntity> savedMembers = saveMembers(
                conversation.getId(),
                members,
                conversation.getType(),
                conversation.getOwnerId(),
                conversation.getCreatedAt()
        );
        projectionService.initializeConversation(conversation.getId(), conversation.getCreatedAt(), savedMembers);
        return ConversationResponse.created(
                conversation.getId(),
                conversation.getType(),
                conversation.getCreatedAt(),
                conversation.getOwnerId(),
                conversation.getRoomName()
        );
    }

    private ConversationEntity saveConversation(ConversationType type, UUID ownerId, String roomName) {
        creationPolicy.validateMetadata(type, ownerId, roomName);
        return conversationRepository.save(ConversationEntity.builder()
                .createdAt(Instant.now())
                .type(type)
                .ownerId(ownerId)
                .roomName(roomName)
                .build());
    }

    private List<ConversationMemberEntity> saveMembers(
            UUID conversationId,
            Set<UUID> memberUserIds,
            ConversationType type,
            UUID ownerId,
            Instant conversationCreatedAt
    ) {
        return memberRepository.saveAll(memberUserIds.stream()
                .map(userId -> ConversationMemberEntity.builder()
                        .conversationId(conversationId)
                        .userId(userId)
                        .role(resolveInitialRole(userId, type, ownerId))
                        .joinedAt(resolveJoinedAt(conversationCreatedAt))
                        .build())
                .toList());
    }

    private Instant resolveJoinedAt(Instant conversationCreatedAt) {
        return conversationCreatedAt;
    }

    private ConversationMemberRole resolveInitialRole(UUID userId, ConversationType type, UUID ownerId) {
        if (type == ConversationType.GROUP && userId.equals(ownerId)) {
            return ConversationMemberRole.OWNER;
        }
        return ConversationMemberRole.MEMBER;
    }

    private String normalizeRoomName(String roomName) {
        if (roomName == null) {
            return null;
        }
        String normalized = roomName.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
