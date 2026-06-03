package com.pulse.chat.domain.chat_core.membership.repository;

import com.pulse.chat.domain.chat_core.membership.entity.ConversationMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationMemberRepository extends JpaRepository<ConversationMemberEntity, UUID> {

    List<ConversationMemberEntity> findByConversationId(UUID conversationId);

    List<ConversationMemberEntity> findByConversationIdIn(List<UUID> conversationIds);

    boolean existsByConversationIdAndUserId(UUID conversationId, UUID userId);

    Optional<ConversationMemberEntity> findByConversationIdAndUserId(UUID conversationId, UUID userId);

    @Query("""
            select exists (
                select 1
                from ConversationEntity c
                join ConversationMemberEntity m on m.conversationId = c.id
                  and m.userId in (:userId1, :userId2)
                  and c.type = ConversationType.DIRECT
                group by m.id
                having count(distinct m.id) = 2
            )
            """)
    boolean existsDirectConversationBetweenUsers(UUID userId1, UUID userId2);
}
