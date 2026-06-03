package com.pulse.chat.domain.events.outbox.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "event_outbox", indexes = {
        @Index(name = "idx_outbox_status_next_retry", columnList = "status,nextRetryAt")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_outbox_event_key", columnNames = "eventKey")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventOutboxEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, length = 120)
    private String eventKey;

    @Column(nullable = false)
    private UUID aggregateId;

    @Column(nullable = false, length = 8000)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OutboxStatus status;

    @Column(nullable = false)
    private int retryCount;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private Instant nextRetryAt;

    @Column(length = 500)
    private String lastError;

    public void markSent() {
        this.status = OutboxStatus.SENT;
        this.updatedAt = Instant.now();
        this.lastError = null;
    }

    public void markFailedWithoutRetry(String errorMessage) {
        incrementRetryCount(errorMessage);
        this.status = OutboxStatus.FAILED;
    }

    public void markFailedPermanently() {
        this.status = OutboxStatus.FAILED;
    }

    public void incrementRetryCount(String errorMessage) {
        this.retryCount++;
        this.updatedAt = Instant.now();
        this.lastError = errorMessage;
    }

    public void recordFailureAndScheduleRetry(Instant nextRetryAt) {
        this.status = OutboxStatus.PENDING;
        this.nextRetryAt = nextRetryAt;
    }
}
