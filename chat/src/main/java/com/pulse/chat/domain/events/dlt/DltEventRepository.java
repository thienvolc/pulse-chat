package com.pulse.chat.domain.events.dlt;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DltEventRepository extends JpaRepository<DltEventEntity, UUID> {
    List<DltEventEntity> findTop200ByStatusOrderByCreatedAtAsc(DltEventStatus status);
    long countByStatus(DltEventStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update DltEventEntity d
            set d.status = :processingStatus, d.updatedAt = :now
            where d.id = :id and d.status = :pendingStatus
            """)
    int claimPendingForReplay(@Param("id") UUID id,
                              @Param("pendingStatus") DltEventStatus pendingStatus,
                              @Param("processingStatus") DltEventStatus processingStatus,
                              @Param("now") Instant now);
}
