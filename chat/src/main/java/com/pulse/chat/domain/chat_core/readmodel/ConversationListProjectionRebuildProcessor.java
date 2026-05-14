package com.pulse.chat.domain.chat_core.readmodel;

import com.pulse.chat.domain.chat_core.entity.ConversationEntity;
import com.pulse.chat.domain.chat_core.entity.ConversationMemberEntity;
import com.pulse.chat.domain.chat_core.entity.ConversationReadStateEntity;
import com.pulse.chat.domain.chat_core.entity.MessageEntity;
import com.pulse.chat.domain.chat_core.policy.ConversationProjectionPolicy;
import com.pulse.chat.domain.chat_core.repository.ConversationMemberRepository;
import com.pulse.chat.domain.chat_core.repository.ConversationReadStateRepository;
import com.pulse.chat.domain.chat_core.repository.MessageRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@lombok.RequiredArgsConstructor
public class ConversationListProjectionRebuildProcessor {
    private final ConversationListViewRepository repository;
    private final ConversationMemberRepository memberRepository;
    private final ConversationReadStateRepository readStateRepository;
    private final MessageRepository messageRepository;
    private final ConversationListProjectionUpdater updater;
    private final ConversationProjectionPolicy projectionPolicy;

    public PageRebuildResult rebuildPage(List<ConversationEntity> conversations) {
        if (conversations.isEmpty()) {
            return PageRebuildResult.empty();
        }
        PageRebuildData pageData = loadPageData(conversations);
        int rebuiltRows = 0;
        int processedConversations = 0;
        for (ConversationEntity conversation : conversations) {
            int memberCount = rebuildConversation(conversation, pageData);
            if (memberCount == 0) {
                continue;
            }
            rebuiltRows += memberCount;
            processedConversations++;
        }
        return new PageRebuildResult(rebuiltRows, processedConversations);
    }

    private PageRebuildData loadPageData(List<ConversationEntity> conversations) {
        List<UUID> conversationIds = conversations.stream().map(ConversationEntity::getId).toList();
        return new PageRebuildData(
                groupMembersByConversation(conversationIds),
                mapLatestMessagesByConversation(conversationIds),
                groupMessagesByConversation(conversationIds),
                mapReadStates(conversationIds)
        );
    }

    private int rebuildConversation(ConversationEntity conversation, PageRebuildData pageData) {
        List<ConversationMemberEntity> members = pageData.membersByConversation().getOrDefault(
                conversation.getId(),
                Collections.emptyList()
        );
        if (members.isEmpty()) {
            return 0;
        }
        updater.initializeConversation(conversation.getId(), conversation.getCreatedAt(), members);
        applyConversationState(
                conversation,
                members,
                pageData.latestMessages().get(conversation.getId()),
                pageData.messagesByConversation().getOrDefault(conversation.getId(), Collections.emptyList()),
                pageData.readStates()
        );
        return members.size();
    }

    private void applyConversationState(
            ConversationEntity conversation,
            List<ConversationMemberEntity> members,
            MessageEntity latestMessage,
            List<MessageEntity> messages,
            Map<ReadStateKey, ConversationReadStateEntity> readStates
    ) {
        if (latestMessage == null) {
            return;
        }
        String snippet = projectionPolicy.toSnippet(latestMessage.getContent());
        repository.updateLastMessageForConversation(conversation.getId(), snippet, latestMessage.getCreatedAt());
        for (ConversationMemberEntity member : members) {
            int unreadCount = projectionPolicy.countUnreadMessages(
                    member.getUserId(),
                    member.getJoinedAt(),
                    messages,
                    readStates.get(new ReadStateKey(member.getUserId(), conversation.getId()))
            );
            repository.setUnreadCountForUser(conversation.getId(), member.getUserId(), unreadCount);
        }
    }

    private Map<UUID, List<ConversationMemberEntity>> groupMembersByConversation(Collection<UUID> conversationIds) {
        Map<UUID, List<ConversationMemberEntity>> grouped = new HashMap<>();
        for (ConversationMemberEntity member : memberRepository.findByConversationIdIn(conversationIds.stream().toList())) {
            grouped.computeIfAbsent(member.getConversationId(), ignored -> new java.util.ArrayList<>()).add(member);
        }
        return grouped;
    }

    private Map<UUID, MessageEntity> mapLatestMessagesByConversation(Collection<UUID> conversationIds) {
        Map<UUID, MessageEntity> latestByConversation = new HashMap<>();
        for (MessageEntity message : messageRepository.findLatestMessagesByConversationIds(conversationIds)) {
            latestByConversation.putIfAbsent(message.getConversationId(), message);
        }
        return latestByConversation;
    }

    private Map<UUID, List<MessageEntity>> groupMessagesByConversation(Collection<UUID> conversationIds) {
        Map<UUID, List<MessageEntity>> grouped = new HashMap<>();
        for (MessageEntity message : messageRepository.findByConversationIdIn(conversationIds)) {
            grouped.computeIfAbsent(message.getConversationId(), ignored -> new java.util.ArrayList<>()).add(message);
        }
        return grouped;
    }

    private Map<ReadStateKey, ConversationReadStateEntity> mapReadStates(Collection<UUID> conversationIds) {
        Map<ReadStateKey, ConversationReadStateEntity> readStates = new HashMap<>();
        for (ConversationReadStateEntity state : readStateRepository.findByConversationIdIn(conversationIds.stream().toList())) {
            readStates.put(new ReadStateKey(state.getUserId(), state.getConversationId()), state);
        }
        return readStates;
    }

    public record PageRebuildResult(int rebuiltRows, int processedConversations) {
        private static PageRebuildResult empty() {
            return new PageRebuildResult(0, 0);
        }
    }

    private record PageRebuildData(
            Map<UUID, List<ConversationMemberEntity>> membersByConversation,
            Map<UUID, MessageEntity> latestMessages,
            Map<UUID, List<MessageEntity>> messagesByConversation,
            Map<ReadStateKey, ConversationReadStateEntity> readStates
    ) {
    }

    private record ReadStateKey(UUID userId, UUID conversationId) {
    }
}
