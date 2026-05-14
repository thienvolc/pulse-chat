package com.pulse.chat.domain.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "delivered_messages", uniqueConstraints = {
        @UniqueConstraint(name = "uk_delivered_message_user", columnNames = {"messageId", "userId"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveredMessageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID messageId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private Instant deliveredAt;
}
