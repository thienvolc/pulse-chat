package com.pulse.chat.domain.notification;

import com.pulse.chat.domain.notification.entity.DeliveredMessageEntity;
import com.pulse.chat.domain.notification.repository.DeliveredMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryRecorder {

    private final DeliveredMessageRepository deliveredMessageRepository;

    @Transactional
    public boolean recordIfNotDuplicate(UUID messageId, UUID userId) {
        if (deliveredMessageRepository.existsByMessageIdAndUserId(messageId, userId)) {
            return false;
        }
        deliveredMessageRepository.save(DeliveredMessageEntity.builder()
                .messageId(messageId)
                .userId(userId)
                .deliveredAt(Instant.now())
                .build());
        return true;
    }
}
