package com.pulse.chat.domain.events.dlt.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dlt_events", indexes = {
        @Index(name = "idx_dlt_status_created", columnList = "status,createdAt")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeadLetterEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String sourceTopic;

    @Column(nullable = false, length = 500)
    private String messageKey;

    @Column(nullable = false, length = 8000)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DeadLetterEventStatus status;

    @Column(nullable = false)
    private int replayCount;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(length = 500)
    private String lastError;

    public void markReplayed() {
        this.status = DeadLetterEventStatus.REPLAYED;
        this.replayCount += 1;
        this.updatedAt = Instant.now();
        this.lastError = null;
    }

    public void markFailed(String errorMessage) {
        this.status = DeadLetterEventStatus.FAILED;
        this.replayCount += 1;
        this.updatedAt = Instant.now();
        this.lastError = errorMessage;
    }
}
