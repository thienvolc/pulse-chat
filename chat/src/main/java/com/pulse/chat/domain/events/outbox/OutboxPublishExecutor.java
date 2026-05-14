package com.pulse.chat.domain.events.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulse.chat.domain.events.model.MessageCreatedEvent;
import com.pulse.chat.domain.events.service.EventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Slf4j
@lombok.RequiredArgsConstructor
public class OutboxPublishExecutor {
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final OutboxRetryPolicy retryPolicy;

    public void processClaimed(EventOutboxEntity item) {
        try {
            publish(item);
        } catch (Exception ex) {
            handlePublishFailure(item, ex);
        }
    }

    private void publish(EventOutboxEntity item) {
        MessageCreatedEvent event = parsePayload(item.getPayload());
        eventPublisher.publishMessageCreated(event);
        markSent(item);
        log.debug("Outbox published, outboxId={}, eventKey={}", item.getId(), item.getEventKey());
    }

    private void markSent(EventOutboxEntity item) {
        item.setStatus(OutboxStatus.SENT);
        item.setUpdatedAt(Instant.now());
        item.setLastError(null);
    }

    private void handlePublishFailure(EventOutboxEntity item, Exception ex) {
        if (ex instanceof NonRetryableOutboxException) {
            markFailedWithoutRetry(item, ex);
            log.warn("Outbox publish non-retryable failure, outboxId={}, eventKey={}", item.getId(), item.getEventKey(), ex);
            return;
        }
        scheduleRetryOrFail(item, ex);
    }

    private void markFailedWithoutRetry(EventOutboxEntity item, Exception ex) {
        item.setStatus(OutboxStatus.FAILED);
        item.setRetryCount(item.getRetryCount() + 1);
        item.setUpdatedAt(Instant.now());
        item.setLastError(ex.getMessage());
    }

    private void scheduleRetryOrFail(EventOutboxEntity item, Exception ex) {
        int retries = incrementRetryCount(item, ex);
        if (retryPolicy.shouldFail(retries)) {
            markFailedAfterRetryExhausted(item);
            log.warn("Outbox publish failed permanently, outboxId={}, eventKey={}, retries={}", item.getId(), item.getEventKey(), retries);
            return;
        }
        rescheduleRetry(item, retries);
        log.warn("Outbox publish failed, retry scheduled, outboxId={}, eventKey={}, retries={}", item.getId(), item.getEventKey(), retries);
    }

    private int incrementRetryCount(EventOutboxEntity item, Exception ex) {
        int retries = item.getRetryCount() + 1;
        item.setRetryCount(retries);
        item.setUpdatedAt(Instant.now());
        item.setLastError(ex.getMessage());
        return retries;
    }

    private void markFailedAfterRetryExhausted(EventOutboxEntity item) {
        item.setStatus(OutboxStatus.FAILED);
    }

    private void rescheduleRetry(EventOutboxEntity item, int retries) {
        item.setStatus(OutboxStatus.PENDING);
        item.setNextRetryAt(retryPolicy.nextRetryAt(retries, Instant.now()));
    }

    private MessageCreatedEvent parsePayload(String payload) {
        try {
            OutboxMessageEnvelope envelope = objectMapper.readValue(payload, OutboxMessageEnvelope.class);
            if (envelope.data() == null) {
                throw new NonRetryableOutboxException("Outbox payload missing event data", null);
            }
            return envelope.data();
        } catch (Exception ex) {
            if (ex instanceof NonRetryableOutboxException) {
                throw (NonRetryableOutboxException) ex;
            }
            throw new NonRetryableOutboxException("Invalid outbox payload", ex);
        }
    }
}
