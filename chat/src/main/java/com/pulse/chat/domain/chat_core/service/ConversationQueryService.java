package com.pulse.chat.domain.chat_core.service;

import com.pulse.chat.app.aop.BusinessException;
import com.pulse.chat.domain.chat_core.dto.ConversationResponse;
import com.pulse.chat.domain.chat_core.entity.ConversationEntity;
import com.pulse.chat.domain.chat_core.mapper.ConversationResponseMapper;
import com.pulse.chat.domain.chat_core.readmodel.ConversationListViewEntity;
import com.pulse.chat.domain.chat_core.readmodel.ConversationListViewRepository;
import com.pulse.chat.domain.chat_core.repository.ConversationRepository;
import com.pulse.chat.domain.common.constant.ResponseCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@lombok.RequiredArgsConstructor
public class ConversationQueryService {
    private final ConversationRepository conversationRepository;
    private final ConversationListViewRepository conversationListViewRepository;
    private final ConversationResponseMapper responseMapper;

    public Page<ConversationResponse> listConversations(UUID requesterId, Pageable pageable) {
        Page<ConversationListViewEntity> page =
                conversationListViewRepository.findByUserIdOrderByLastMessageAtDescCreatedAtDesc(requesterId, pageable);
        Set<UUID> conversationIds = page.getContent().stream()
                .map(ConversationListViewEntity::getConversationId)
                .collect(Collectors.toSet());
        Map<UUID, ConversationEntity> conversationsById = conversationRepository.findAllById(conversationIds).stream()
                .collect(Collectors.toMap(ConversationEntity::getId, conversation -> conversation));
        return page.map(row -> responseMapper.fromListView(
                row,
                requireConversationAggregate(conversationsById, row.getConversationId()).getType(),
                requireConversationAggregate(conversationsById, row.getConversationId())
        ));
    }

    private ConversationEntity requireConversationAggregate(
            Map<UUID, ConversationEntity> conversationsById,
            UUID conversationId
    ) {
        ConversationEntity conversation = conversationsById.get(conversationId);
        if (conversation == null) {
            throw new BusinessException(ResponseCode.CONVERSATION_NOT_FOUND);
        }
        return conversation;
    }
}
