package com.pulse.chat.domain.chat_core.message.repository;

import com.pulse.chat.domain.chat_core.message.entity.ConversationReadStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationReadStateRepository extends JpaRepository<ConversationReadStateEntity, UUID> {
    Optional<ConversationReadStateEntity> findByConversationIdAndUserId(UUID conversationId,
                                                                        UUID userId);

    List<ConversationReadStateEntity> findByConversationIdIn(List<UUID> conversationIds);

    void deleteByUserIdAndConversationId(UUID userId, UUID conversationId);
}
