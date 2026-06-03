package com.pulse.chat.domain.events.dlt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnExpression(
        "('${app.events.mode:local}'.toLowerCase() == 'kafka' || '${app.events.mode:local}'.toLowerCase() == 'hybrid')" +
                " && ('${app.runtime.role:all}'.toLowerCase() == 'all' || '${app.runtime.role:all}'.toLowerCase() == 'worker')"
)
public class DeadLetterIngestConsumer {

    private final DeadLetterCaptureService captureService;

    @KafkaListener(topics = "${app.events.topic}.dlt", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onDeadLetterMessage(String payload,
                                    @Header(name = KafkaHeaders.RECEIVED_TOPIC,
                                            required = false) String topic,
                                    @Header(name = KafkaHeaders.RECEIVED_KEY,
                                            required = false) String key) {

        String sourceTopic = topic == null ? "unknown" : topic;
        String messageKey = key == null ? "unknown" : key;
        captureService.capture(sourceTopic, messageKey, payload);
        log.warn("DLT message ingested. topic={}, key={}", sourceTopic, messageKey);
    }
}
