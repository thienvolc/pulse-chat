package com.pulse.chat.domain.events.dlt.repository;

import com.pulse.chat.domain.events.dlt.entity.DeadLetterEventEntity;
import com.pulse.chat.domain.events.dlt.entity.DeadLetterEventStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DeadLetterEventRepository extends JpaRepository<DeadLetterEventEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select e.id
            from DeadLetterEventEntity e
            where e.status == :pendingStatus
            order by e.createdAt asc
            limit 200
            """)
    List<UUID> findTop200IdsReadyToReplay(@Param("pendingStatus") DeadLetterEventStatus pendingStatus);

    long countByStatus(DeadLetterEventStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update DeadLetterEventEntity d
            set d.status = :processingStatus, d.updatedAt = :now
            where d.id = :id and d.status = :pendingStatus
            """)
    int claimPendingForReplay(@Param("id") UUID id,
                              @Param("pendingStatus") DeadLetterEventStatus pendingStatus,
                              @Param("processingStatus") DeadLetterEventStatus processingStatus,
                              @Param("now") Instant now);
}
