package com.pulse.chat.domain.chat_core.repository;

import com.pulse.chat.domain.chat_core.entity.ConversationReadStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationReadStateRepository extends JpaRepository<ConversationReadStateEntity, UUID> {
    Optional<ConversationReadStateEntity> findByUserIdAndConversationId(UUID userId, UUID conversationId);
    List<ConversationReadStateEntity> findByConversationIdIn(List<UUID> conversationIds);
    void deleteByUserIdAndConversationId(UUID userId, UUID conversationId);
}
