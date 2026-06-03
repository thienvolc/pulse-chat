package com.pulse.chat.domain.events.dlt;

import com.pulse.chat.app.aop.BusinessException;
import com.pulse.chat.domain.common.constant.ResponseCode;
import com.pulse.chat.domain.events.dlt.entity.DeadLetterEventEntity;
import com.pulse.chat.domain.events.dlt.repository.DeadLetterEventRepository;
import com.pulse.chat.infrastructure.config.prop.EventPublisherProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeadLetterPublishExecutor {

    private final DeadLetterEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final EventPublisherProperties eventPublisherProperties;

    public boolean replay(UUID dltId) {
        var item = repository.findById(dltId)
                .orElseThrow(() -> new BusinessException(ResponseCode.DLT_EVENT_NOT_FOUND));

        try {
            publish(item);
            item.markReplayed();
            repository.save(item);

            return true;

        } catch (Exception ex) {
            item.markFailed(ex.getMessage());
            repository.save(item);

            return false;
        }
    }

    private void publish(DeadLetterEventEntity event) {
        try {
            kafkaTemplate.send(
                    eventPublisherProperties.topic(),
                    event.getMessageKey(),
                    event.getPayload()
            );
        } catch (Exception ex) {
            log.warn("Kafka publish failed, eventId={}", event.getMessageKey(), ex);
            throw ex;
        }
    }
}
