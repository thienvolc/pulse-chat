package com.pulse.chat.domain.chat_core.readmodel.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversation_list_view", uniqueConstraints = {
        @UniqueConstraint(name = "uk_conversation_list_user_conv", columnNames = {"userId", "conversationId"})
}, indexes = {
        @Index(name = "idx_conversation_list_user_last_msg", columnList = "userId,lastMessageAt")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationListViewEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID conversationId;

    @Column
    private UUID peerUserId;

    @Column(length = 50)
    private String peerUsername;

    @Column(length = 250)
    private String lastMessageSnippet;

    private Instant lastMessageAt;

    @Column(nullable = false)
    private int unreadCount;

    @Column(nullable = false)
    private Instant createdAt;
}
