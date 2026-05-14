package com.pulse.chat.domain.chat_core.repository;

import com.pulse.chat.domain.chat_core.dto.ConversationListRow;
import com.pulse.chat.domain.chat_core.entity.ConversationMemberEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationMemberRepository extends JpaRepository<ConversationMemberEntity, UUID> {
    List<ConversationMemberEntity> findByUserId(UUID userId);
    List<ConversationMemberEntity> findByConversationId(UUID conversationId);
    List<ConversationMemberEntity> findByConversationIdIn(List<UUID> conversationIds);
    boolean existsByConversationIdAndUserId(UUID conversationId, UUID userId);
    Optional<ConversationMemberEntity> findByConversationIdAndUserId(UUID conversationId, UUID userId);

    @Query("""
            select new com.pulse.chat.domain.chat_core.dto.ConversationListRow(
                cm.conversationId, c.createdAt, peer.userId, u.username
            )
            from ConversationMemberEntity cm
            join ConversationEntity c on c.id = cm.conversationId
            join ConversationMemberEntity peer on peer.conversationId = cm.conversationId and peer.userId <> :userId
            join UserEntity u on u.id = peer.userId
            where cm.userId = :userId
            order by c.createdAt desc
            """)
    Page<ConversationListRow> findConversationRows(@Param("userId") UUID userId, Pageable pageable);
}
