package com.pulse.chat.domain.chat_core.conversation.repository;

import com.pulse.chat.domain.chat_core.conversation.entity.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConversationRepository extends JpaRepository<ConversationEntity, UUID> {
}
