package com.pulse.chat.infrastructure.config;

import com.pulse.chat.infrastructure.config.prop.KafkaConsumerTuningProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableConfigurationProperties(KafkaConsumerTuningProperties.class)
public class KafkaConsumerConfig {

    @Bean
    DefaultErrorHandler kafkaDefaultErrorHandler(KafkaOperations<String, String> kafkaOperations,
                                                 KafkaConsumerTuningProperties tuning) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaOperations,
                (record, ex) -> new org.apache.kafka.common.TopicPartition(record.topic() + ".dlt", record.partition())
        );
        FixedBackOff backOff = new FixedBackOff(tuning.retryBackoffMs(), tuning.retryMaxAttempts());
        return new DefaultErrorHandler(recoverer, backOff);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            DefaultErrorHandler kafkaDefaultErrorHandler,
            KafkaConsumerTuningProperties tuning
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(kafkaDefaultErrorHandler);
        factory.setConcurrency(tuning.concurrency());
        return factory;
    }
}
