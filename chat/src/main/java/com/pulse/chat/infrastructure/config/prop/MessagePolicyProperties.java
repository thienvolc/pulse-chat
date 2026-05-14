package com.pulse.chat.infrastructure.config.prop;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.message-policy")
public record MessagePolicyProperties(
        int minLength,
        int maxLength,
        int maxMessagesPerWindow,
        int windowSeconds,
        int duplicateCooldownSeconds
) {
    public MessagePolicyProperties {
        if (minLength <= 0) minLength = 1;
        if (maxLength <= 0) maxLength = 2000;
        if (maxMessagesPerWindow <= 0) maxMessagesPerWindow = 10;
        if (windowSeconds <= 0) windowSeconds = 10;
        if (duplicateCooldownSeconds <= 0) duplicateCooldownSeconds = 5;
    }
}
