package com.pulse.chat.domain.chat_core.readmodel;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationListViewRepository extends JpaRepository<ConversationListViewEntity, UUID> {
    Page<ConversationListViewEntity> findByUserIdOrderByLastMessageAtDescCreatedAtDesc(UUID userId, Pageable pageable);
    Optional<ConversationListViewEntity> findByUserIdAndConversationId(UUID userId, UUID conversationId);
    List<ConversationListViewEntity> findByConversationId(UUID conversationId);
    void deleteByUserIdAndConversationId(UUID userId, UUID conversationId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ConversationListViewEntity v
            set v.lastMessageSnippet = :snippet,
                v.lastMessageAt = :createdAt
            where v.conversationId = :conversationId
            """)
    int updateLastMessageForConversation(@Param("conversationId") UUID conversationId,
                                         @Param("snippet") String snippet,
                                         @Param("createdAt") java.time.Instant createdAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ConversationListViewEntity v
            set v.lastMessageSnippet = :snippet,
                v.lastMessageAt = :createdAt
            where v.conversationId = :conversationId and v.userId = :userId
            """)
    int updateLastMessageForUser(@Param("conversationId") UUID conversationId,
                                 @Param("userId") UUID userId,
                                 @Param("snippet") String snippet,
                                 @Param("createdAt") java.time.Instant createdAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ConversationListViewEntity v
            set v.unreadCount = v.unreadCount + 1
            where v.conversationId = :conversationId and v.userId <> :senderId
            """)
    int incrementUnreadForRecipients(@Param("conversationId") UUID conversationId,
                                     @Param("senderId") UUID senderId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ConversationListViewEntity v
            set v.unreadCount = 0
            where v.conversationId = :conversationId and v.userId = :userId
            """)
    int resetUnreadForUser(@Param("conversationId") UUID conversationId,
                           @Param("userId") UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ConversationListViewEntity v
            set v.unreadCount = :unreadCount
            where v.conversationId = :conversationId and v.userId = :userId
            """)
    int setUnreadCountForUser(@Param("conversationId") UUID conversationId,
                              @Param("userId") UUID userId,
                              @Param("unreadCount") int unreadCount);

    @Query("select max(v.lastMessageAt) from ConversationListViewEntity v")
    java.time.Instant findLatestProjectedMessageAt();
}
