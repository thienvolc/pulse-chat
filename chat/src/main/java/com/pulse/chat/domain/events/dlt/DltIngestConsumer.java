package com.pulse.chat.domain.events.dlt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@lombok.RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnExpression(
        "('${app.events.mode:local}'.toLowerCase() == 'kafka' || '${app.events.mode:local}'.toLowerCase() == 'hybrid')" +
                " && ('${app.runtime.role:all}'.toLowerCase() == 'all' || '${app.runtime.role:all}'.toLowerCase() == 'worker')"
)
public class DltIngestConsumer {
    private final DltCaptureService dltCaptureService;

    @KafkaListener(topics = "${app.events.topic}.dlt", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onDltMessage(String payload,
                             @org.springframework.messaging.handler.annotation.Header(name = org.springframework.kafka.support.KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
                             @org.springframework.messaging.handler.annotation.Header(name = org.springframework.kafka.support.KafkaHeaders.RECEIVED_KEY, required = false) String key) {
        String sourceTopic = topic == null ? "unknown" : topic;
        String messageKey = key == null ? "unknown" : key;
        dltCaptureService.capture(sourceTopic, messageKey, payload);
        log.warn("DLT message ingested. topic={}, key={}", sourceTopic, messageKey);
    }
}
