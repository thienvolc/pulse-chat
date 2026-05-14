package com.pulse.chat.infrastructure.config;

import com.pulse.chat.infrastructure.config.prop.OutboxRetryProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OutboxRetryProperties.class)
public class OutboxConfig {
}
