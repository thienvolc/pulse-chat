package com.pulse.chat.domain.chat_core.message.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "messages",
        indexes = {
                @Index(name = "idx_messages_conversation_created",
                        columnList = "conversationId,createdAt DESC, id DESC")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_message_sender_conv_idempotency", columnNames = {
                        "senderId", "conversationId", "idempotencyKey"
                })
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID conversationId;

    @Column(nullable = false)
    private UUID senderId;

    @Column(nullable = false, length = 4000)
    private String content;

    @Column(length = 120)
    private String idempotencyKey;

    @Column(nullable = false)
    private Instant createdAt;
}
