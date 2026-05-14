package com.pulse.chat.domain.chat_core.repository;

import com.pulse.chat.domain.chat_core.entity.MessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<MessageEntity, UUID> {
    Page<MessageEntity> findByConversationIdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);
    Optional<MessageEntity> findTopByConversationIdOrderByCreatedAtDesc(UUID conversationId);
    List<MessageEntity> findByConversationIdIn(Collection<UUID> conversationIds);
    Optional<MessageEntity> findBySenderIdAndConversationIdAndIdempotencyKey(UUID senderId, UUID conversationId, String idempotencyKey);
    long countBySenderIdAndConversationIdAndIdempotencyKey(UUID senderId, UUID conversationId, String idempotencyKey);

    @Query("select max(m.createdAt) from MessageEntity m")
    java.time.Instant findLatestCreatedAt();

    @Query("""
            select m
            from MessageEntity m
            where m.conversationId in :conversationIds
              and m.createdAt = (
                select max(m2.createdAt)
                from MessageEntity m2
                where m2.conversationId = m.conversationId
              )
            """)
    List<MessageEntity> findLatestMessagesByConversationIds(Collection<UUID> conversationIds);

    @Query("""
            select m
            from MessageEntity m
            where exists (
                select 1
                from ConversationMemberEntity cm
                where cm.conversationId = m.conversationId
                  and cm.userId = :userId
            )
              and (:conversationId is null or m.conversationId = :conversationId)
              and (:senderId is null or m.senderId = :senderId)
              and (:fromTime is null or m.createdAt >= :fromTime)
              and (:toTime is null or m.createdAt <= :toTime)
              and lower(m.content) like lower(concat('%', :keyword, '%'))
            order by m.createdAt desc
            """)
    Page<MessageEntity> searchByKeyword(@Param("userId") UUID userId,
                                        @Param("keyword") String keyword,
                                        @Param("conversationId") UUID conversationId,
                                        @Param("senderId") UUID senderId,
                                        @Param("fromTime") java.time.Instant fromTime,
                                        @Param("toTime") java.time.Instant toTime,
                                        Pageable pageable);
}
