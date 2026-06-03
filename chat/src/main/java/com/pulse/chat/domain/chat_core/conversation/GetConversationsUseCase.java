package com.pulse.chat.domain.chat_core.conversation;

import com.pulse.chat.app.aop.BusinessException;
import com.pulse.chat.domain.chat_core.conversation.dto.response.ConversationResponse;
import com.pulse.chat.domain.chat_core.conversation.mapper.ConversationMapper;
import com.pulse.chat.domain.chat_core.conversation.entity.ConversationEntity;
import com.pulse.chat.domain.chat_core.readmodel.entity.ConversationListViewEntity;
import com.pulse.chat.domain.chat_core.readmodel.repository.ConversationListViewRepository;
import com.pulse.chat.domain.chat_core.conversation.repository.ConversationRepository;
import com.pulse.chat.domain.common.constant.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetConversationsUseCase {

    private final ConversationRepository conversationRepository;
    private final ConversationListViewRepository conversationListViewRepository;

    private final ConversationMapper conversationMapper;

    public Page<ConversationResponse> execute(UUID requesterId, Pageable pageable) {
        Page<ConversationListViewEntity> page = getConversationListViews(requesterId, pageable);
        Set<UUID> conversationIds = extractConversationIds(page);
        Map<UUID, ConversationEntity> conversationsById = mapConversationsById(conversationIds);

        return page.map(row -> conversationMapper.fromListView(
                row, mapConversationByIdOrThrow(conversationsById, row.getConversationId())));
    }

    private Page<ConversationListViewEntity> getConversationListViews(UUID userId,
                                                                      Pageable pageable) {

        return conversationListViewRepository
                .findByUserIdOrderByLastMessageAtDescCreatedAtDesc(userId, pageable);
    }

    private Set<UUID> extractConversationIds(Page<ConversationListViewEntity> page) {
        return page.getContent().stream()
                .map(ConversationListViewEntity::getConversationId)
                .collect(Collectors.toSet());
    }

    private Map<UUID, ConversationEntity> mapConversationsById(Set<UUID> conversationIds) {
        return conversationRepository.findAllById(conversationIds).stream()
                .collect(Collectors.toMap(
                        ConversationEntity::getId,
                        conversation -> conversation));
    }

    private ConversationEntity mapConversationByIdOrThrow(
            Map<UUID, ConversationEntity> conversationsById,
            UUID conversationId) {

        var conversation = conversationsById.get(conversationId);
        if (conversation == null) {
            throw new BusinessException(ResponseCode.CONVERSATION_NOT_FOUND);
        }

        return conversation;
    }
}
