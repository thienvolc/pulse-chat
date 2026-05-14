package com.pulse.chat.domain.chat_core.readmodel;

import com.pulse.chat.domain.chat_core.entity.ConversationEntity;
import com.pulse.chat.domain.chat_core.entity.ConversationMemberEntity;
import com.pulse.chat.domain.chat_core.entity.ConversationType;
import com.pulse.chat.domain.chat_core.policy.ConversationProjectionPolicy;
import com.pulse.chat.domain.chat_core.repository.ConversationRepository;
import com.pulse.chat.domain.user.entity.UserEntity;
import com.pulse.chat.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@lombok.RequiredArgsConstructor
public class ConversationListProjectionUpdater {
    private static final String UNKNOWN_DIRECT_PEER_USERNAME = "unknown";

    private final ConversationListViewRepository repository;
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationProjectionPolicy projectionPolicy;

    @Transactional
    public void initializeConversation(UUID conversationId, Instant createdAt, List<ConversationMemberEntity> members) {
        Map<UUID, UserEntity> usersById = loadUsersById(members);
        ConversationType conversationType = loadConversationType(conversationId);
        for (ConversationMemberEntity member : members) {
            PeerDescriptor peer = resolvePeerDescriptor(conversationType, members, member.getUserId(), usersById);
            repository.save(buildProjectionRow(conversationId, createdAt, member.getUserId(), peer));
        }
    }

    @Transactional
    public void onMessageCreated(UUID conversationId, UUID senderId, String content, Instant createdAt) {
        String snippet = projectionPolicy.toSnippet(content);
        repository.updateLastMessageForConversation(conversationId, snippet, createdAt);
        repository.incrementUnreadForRecipients(conversationId, senderId);
    }

    @Transactional
    public void onConversationRead(UUID conversationId, UUID userId) {
        repository.resetUnreadForUser(conversationId, userId);
    }

    @Transactional
    public void backfillLatestVisibleStateForMember(
            UUID conversationId,
            UUID userId,
            String content,
            Instant createdAt
    ) {
        String snippet = projectionPolicy.toSnippet(content);
        repository.updateLastMessageForUser(conversationId, userId, snippet, createdAt);
        repository.setUnreadCountForUser(conversationId, userId, 0);
    }

    private Map<UUID, UserEntity> loadUsersById(List<ConversationMemberEntity> members) {
        return userRepository.findAllById(members.stream().map(ConversationMemberEntity::getUserId).toList())
                .stream()
                .collect(Collectors.toMap(UserEntity::getId, user -> user));
    }

    private ConversationType loadConversationType(UUID conversationId) {
        return conversationRepository.findById(conversationId)
                .map(ConversationEntity::getType)
                .orElse(ConversationType.DIRECT);
    }

    private PeerDescriptor resolvePeerDescriptor(
            ConversationType conversationType,
            List<ConversationMemberEntity> members,
            UUID currentUserId,
            Map<UUID, UserEntity> usersById
    ) {
        if (conversationType != ConversationType.DIRECT) {
            return PeerDescriptor.empty();
        }
        ConversationMemberEntity peerMember = findDirectPeerMember(members, currentUserId).orElseThrow();
        return new PeerDescriptor(
                peerMember.getUserId(),
                resolvePeerUsername(peerMember.getUserId(), usersById)
        );
    }

    private Optional<ConversationMemberEntity> findDirectPeerMember(
            List<ConversationMemberEntity> members,
            UUID currentUserId
    ) {
        return members.stream()
                .filter(member -> !member.getUserId().equals(currentUserId))
                .findFirst()
                .or(() -> members.stream().filter(member -> member.getUserId().equals(currentUserId)).findFirst());
    }

    private String resolvePeerUsername(UUID peerUserId, Map<UUID, UserEntity> usersById) {
        UserEntity peerUser = usersById.get(peerUserId);
        return peerUser != null ? peerUser.getUsername() : UNKNOWN_DIRECT_PEER_USERNAME;
    }

    private ConversationListViewEntity buildProjectionRow(
            UUID conversationId,
            Instant createdAt,
            UUID userId,
            PeerDescriptor peer
    ) {
        return ConversationListViewEntity.builder()
                .userId(userId)
                .conversationId(conversationId)
                .peerUserId(peer.peerUserId())
                .peerUsername(peer.peerUsername())
                .lastMessageSnippet(null)
                .lastMessageAt(null)
                .unreadCount(0)
                .createdAt(createdAt)
                .build();
    }

    private record PeerDescriptor(UUID peerUserId, String peerUsername) {
        private static PeerDescriptor empty() {
            return new PeerDescriptor(null, null);
        }
    }
}
