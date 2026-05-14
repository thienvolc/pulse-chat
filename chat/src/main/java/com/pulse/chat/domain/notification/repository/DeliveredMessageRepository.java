package com.pulse.chat.domain.notification.repository;

import com.pulse.chat.domain.notification.entity.DeliveredMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DeliveredMessageRepository extends JpaRepository<DeliveredMessageEntity, UUID> {
    boolean existsByMessageIdAndUserId(UUID messageId, UUID userId);
}
