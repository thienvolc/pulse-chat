package com.pulse.chat.domain.chat_core.readmodel.rebuild;

import com.pulse.chat.domain.chat_core.conversation.entity.ConversationEntity;
import com.pulse.chat.domain.chat_core.membership.entity.ConversationMemberEntity;
import com.pulse.chat.domain.chat_core.message.entity.MessageEntity;
import com.pulse.chat.domain.chat_core.readmodel.ConversationViewGuard;
import com.pulse.chat.domain.chat_core.readmodel.repository.ConversationListViewRepository;
import com.pulse.chat.domain.chat_core.readmodel.ConversationViewSync;
import com.pulse.chat.domain.chat_core.membership.repository.ConversationMemberRepository;
import com.pulse.chat.domain.chat_core.message.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationViewRebuildExecutor {

    private final ConversationViewSync conversationViewSync;
    private final ConversationViewGuard conversationViewGuard;

    private final ConversationMemberRepository memberRepository;
    private final MessageRepository messageRepository;
    private final ConversationListViewRepository conversationListViewRepository;

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
        List<UUID> conversationIds =
                conversations.stream().map(ConversationEntity::getId).toList();

        return new PageRebuildData(
                groupMembersByConversation(conversationIds),
                mapLatestMessagesByConversation(conversationIds));
    }

    private int rebuildConversation(ConversationEntity conversation,
                                    PageRebuildData pageData) {

        List<ConversationMemberEntity> members = pageData.membersByConversation()
                .getOrDefault(conversation.getId(), Collections.emptyList());

        if (members.isEmpty()) {
            return 0;
        }
        conversationViewSync.syncConversation(conversation, members);
        applyConversationState(members, pageData.latestMessages().get(conversation.getId()));

        return members.size();
    }

    private void applyConversationState(List<ConversationMemberEntity> members,
                                        MessageEntity latestMessage) {

        if (latestMessage == null) {
            return;
        }
        UUID conversationId = members.getFirst().getConversationId();
        String snippet = conversationViewGuard.toSnippet(latestMessage.getContent());
        conversationListViewRepository.updateLastMessageForConversation(
                conversationId,
                snippet,
                latestMessage.getCreatedAt());

        messageRepository.refreshUnreadCountForConversation(conversationId);
    }

    private Map<UUID, List<ConversationMemberEntity>> groupMembersByConversation(
            List<UUID> conversationIds) {

        var members = memberRepository.findByConversationIdIn(conversationIds);
        return members.stream()
                .collect(Collectors.groupingBy(ConversationMemberEntity::getConversationId));
    }

    private Map<UUID, MessageEntity> mapLatestMessagesByConversation(List<UUID> conversationIds) {
        var messages = messageRepository.findLatestMessagesByConversationIds(conversationIds);
        return messages.stream()
                .collect(Collectors.toMap(
                        MessageEntity::getConversationId,
                        Function.identity())
                );
    }

    private record PageRebuildData(
            Map<UUID, List<ConversationMemberEntity>> membersByConversation,
            Map<UUID, MessageEntity> latestMessages
    ) {
    }
}
