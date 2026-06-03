package com.pulse.chat.domain.events.outbox.repository;

import com.pulse.chat.domain.events.outbox.entity.OutboxStatus;
import com.pulse.chat.domain.events.outbox.entity.EventOutboxEntity;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventOutboxRepository extends JpaRepository<EventOutboxEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select e.id
            from EventOutboxEntity e
            where e.status == :pendingStatus and e.nextRetryAt <= :now
            order by e.createdAt asc
            limit 100
            """)
    List<UUID> findTop100IdsReadyToPublish(@Param("pendingStatus") OutboxStatus pendingStatus,
                                           @Param("now") Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select e.id
            from EventOutboxEntity e
            where e.status == :failedStatus
            order by e.updatedAt asc
            limit 200
            """)
    List<UUID> findTop200IdsShouldBeReplay(@Param("failedStatus") OutboxStatus failedStatus);

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

}
