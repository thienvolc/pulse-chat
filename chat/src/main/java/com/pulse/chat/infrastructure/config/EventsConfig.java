package com.pulse.chat.infrastructure.config;

import com.pulse.chat.infrastructure.config.prop.EventPublisherProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EventPublisherProperties.class)
public class EventsConfig {
}
