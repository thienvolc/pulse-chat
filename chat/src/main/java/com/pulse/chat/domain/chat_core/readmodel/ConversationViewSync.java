package com.pulse.chat.domain.chat_core.readmodel;

import com.pulse.chat.app.aop.BusinessException;
import com.pulse.chat.domain.chat_core.conversation.entity.ConversationEntity;
import com.pulse.chat.domain.chat_core.membership.entity.ConversationMemberEntity;
import com.pulse.chat.domain.chat_core.message.entity.MessageEntity;
import com.pulse.chat.domain.chat_core.conversation.entity.ConversationType;
import com.pulse.chat.domain.chat_core.readmodel.entity.ConversationListViewEntity;
import com.pulse.chat.domain.chat_core.readmodel.repository.ConversationListViewRepository;
import com.pulse.chat.domain.common.constant.ResponseCode;
import com.pulse.chat.domain.events.MessageCreatedEvent;
import com.pulse.chat.domain.user.entity.UserEntity;
import com.pulse.chat.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationViewSync {

    private final ConversationViewGuard conversationViewGuard;

    private final ConversationListViewRepository conversationListViewRepository;
    private final UserRepository userRepository;

    @Transactional
    public void syncConversation(ConversationEntity conversation,
                                 List<ConversationMemberEntity> members) {

        if (members.getFirst().isSelfConversation(members.getLast())) {
            syncNewSelfConversation(conversation, members.getFirst());
        } else if (conversation.getType() == ConversationType.DIRECT) {
            syncNewDirectConversation(conversation, members);
        } else if (conversation.getType() == ConversationType.GROUP) {
            syncNewGroupConversation(conversation, members);
        } else {
            throw new IllegalStateException();
        }
    }

    @Transactional
    public void syncNewDirectConversation(ConversationEntity conversation,
                                          List<ConversationMemberEntity> members) {

        var user1 = getUserByIdOrThrow(members.get(0).getUserId());
        var user2 = getUserByIdOrThrow(members.get(1).getUserId());

        var viewForUser1 = buildViewEntity(
                conversation,
                user1.getId(),
                PeerDescriptor.from(user2));

        var viewForUser2 = buildViewEntity(
                conversation,
                user2.getId(),
                PeerDescriptor.from(user1));

        conversationListViewRepository.saveAll(List.of(viewForUser1, viewForUser2));
    }

    @Transactional
    public void syncNewSelfConversation(ConversationEntity conversation,
                                        ConversationMemberEntity member) {

        var user = getUserByIdOrThrow(member.getUserId());

        var viewForUser = buildViewEntity(
                conversation,
                user.getId(),
                PeerDescriptor.from(user));

        conversationListViewRepository.save(viewForUser);
    }

    @Transactional
    public void syncNewGroupConversation(ConversationEntity conversation,
                                         List<ConversationMemberEntity> members) {

        List<ConversationListViewEntity> viewForUsers = new ArrayList<>();

        for (var member : members) {
            var viewForUser = buildViewEntity(
                    conversation,
                    member.getUserId(),
                    PeerDescriptor.empty());

            viewForUsers.add(viewForUser);
        }

        conversationListViewRepository.saveAll(viewForUsers);
    }

    @Transactional
    public void onMessageCreated(MessageCreatedEvent event) {
        String snippet = conversationViewGuard.toSnippet(event.content());
        conversationListViewRepository.updateLastMessageForConversation(
                event.conversationId(),
                snippet,
                event.createdAt());
        conversationListViewRepository.incrementUnreadForRecipients(
                event.conversationId(),
                event.senderId());
    }

    @Transactional
    public void onConversationRead(UUID conversationId, UUID userId) {
        conversationListViewRepository.resetUnreadForUser(conversationId, userId);
    }

    @Transactional
    public void backfillLatestVisibleStateForMember(UUID userId, MessageEntity message) {
        String snippet = conversationViewGuard.toSnippet(message.getContent());
        conversationListViewRepository.updateLastMessageForUser(
                message.getConversationId(),
                userId,
                snippet,
                message.getCreatedAt());

        conversationListViewRepository.setUnreadCountForUser(
                message.getConversationId(),
                userId,
                0);
    }

    private UserEntity getUserByIdOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResponseCode.USER_NOT_FOUND));
    }

    private ConversationListViewEntity buildViewEntity(ConversationEntity conversation,
                                                       UUID userId,
                                                       PeerDescriptor peer) {

        return ConversationListViewEntity.builder()
                .userId(userId)
                .conversationId(conversation.getId())
                .peerUserId(peer.peerUserId())
                .peerUsername(peer.peerUsername())
                .lastMessageSnippet(null)
                .lastMessageAt(null)
                .unreadCount(0)
                .createdAt(conversation.getCreatedAt())
                .build();

    }

    private record PeerDescriptor(UUID peerUserId, String peerUsername) {

        public PeerDescriptor(UUID peerUserId, String peerUsername) {
            this.peerUserId = peerUserId;
            this.peerUsername = ConversationViewGuard.normalizeUsername(peerUsername);
        }

        private static PeerDescriptor empty() {
            return new PeerDescriptor(null, null);
        }

        public static PeerDescriptor from(UserEntity user) {
            return new PeerDescriptor(user.getId(), user.getUsername());
        }
    }
}
