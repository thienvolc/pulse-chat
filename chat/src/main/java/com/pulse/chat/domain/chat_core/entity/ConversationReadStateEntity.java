package com.pulse.chat.domain.chat_core.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversation_read_state", uniqueConstraints = {
        @UniqueConstraint(name = "uk_conv_read_state_user_conv", columnNames = {"userId", "conversationId"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationReadStateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID conversationId;

    @Column
    private UUID lastReadMessageId;

    @Column(nullable = false)
    private Instant lastReadAt;
}
