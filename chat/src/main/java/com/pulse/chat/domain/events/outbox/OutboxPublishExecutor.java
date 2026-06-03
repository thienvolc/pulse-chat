package com.pulse.chat.domain.events.outbox;

import com.pulse.chat.app.aop.BusinessException;
import com.pulse.chat.domain.common.constant.ResponseCode;
import com.pulse.chat.domain.events.MessageCreatedEvent;
import com.pulse.chat.domain.events.outbox.entity.EventOutboxEntity;
import com.pulse.chat.domain.events.publish.EventPublisher;
import com.pulse.chat.domain.events.outbox.repository.EventOutboxRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxPublishExecutor {

    private final OutboxRetryPolicy retryPolicy;
    private final EventOutboxRepository repository;
    private final EventPublisher publisher;
    private final OutboxPayloadParser payloadParser;

    public boolean processClaimed(UUID itemId) {
        var item = repository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ResponseCode.OUTBOX_EVENT_NOT_FOUND));

        try {
            publish(item);
            item.markSent();
            repository.save(item);

            return true;

        } catch (NonRetryableOutboxException ex) {

            item.markFailedWithoutRetry(ex.getMessage());
            repository.save(item);
            log.warn("Outbox publish non-retryable failure, outboxId={}, eventKey={}",
                    item.getId(),
                    item.getEventKey(),
                    ex
            );
        } catch (Exception ex) {
            scheduleRetryOrFail(item, ex.getMessage());
        }

        return false;
    }

    public void publish(EventOutboxEntity item) throws NonRetryableOutboxException {
        MessageCreatedEvent event = payloadParser.tryParsePayload(item.getPayload());
        publisher.publishMessageCreated(event);
        log.debug("Outbox published, outboxId={}, eventKey={}", item.getId(), item.getEventKey());
    }

    private void scheduleRetryOrFail(EventOutboxEntity item, String errorMessage) {
        item.incrementRetryCount(errorMessage);
        var retries = item.getRetryCount();

        if (retryPolicy.shouldFail(retries)) {
            item.markFailedPermanently();
            log.warn("Outbox publish failed permanently, outboxId={}, eventKey={}, retries={}",
                    item.getId(),
                    item.getEventKey(),
                    item.getRetryCount()
            );
        } else {
            var nextRetryAt = retryPolicy.nextRetryAt(retries, Instant.now());
            item.recordFailureAndScheduleRetry(nextRetryAt);
            log.warn("Outbox publish failed, retry scheduled, outboxId={}, eventKey={}, retries={}",
                    item.getId(),
                    item.getEventKey(),
                    item.getRetryCount()
            );
        }
        repository.save(item);
    }
}
