package com.pulse.chat.domain.events.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventOutboxRepository extends JpaRepository<EventOutboxEntity, UUID> {
    List<EventOutboxEntity> findTop100ByStatusAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(OutboxStatus status, Instant now);
    List<EventOutboxEntity> findTop200ByStatusOrderByUpdatedAtAsc(OutboxStatus status);
    Optional<EventOutboxEntity> findTopByStatusOrderByCreatedAtDesc(OutboxStatus status);
    boolean existsByEventKey(String eventKey);
    long countByStatus(OutboxStatus status);
    Optional<EventOutboxEntity> findFirstByStatusOrderByUpdatedAtAsc(OutboxStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update EventOutboxEntity e
            set e.status = :processingStatus, e.updatedAt = :now
            where e.id = :id and e.status = :pendingStatus and e.nextRetryAt <= :now
            """)
    int claimForProcessing(@Param("id") UUID id,
                           @Param("pendingStatus") OutboxStatus pendingStatus,
                           @Param("processingStatus") OutboxStatus processingStatus,
                           @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update EventOutboxEntity e
            set e.status = :processingStatus, e.updatedAt = :now
            where e.id = :id and e.status = :failedStatus
            """)
    int claimFailedForReplay(@Param("id") UUID id,
                             @Param("failedStatus") OutboxStatus failedStatus,
                             @Param("processingStatus") OutboxStatus processingStatus,
                             @Param("now") Instant now);
}
