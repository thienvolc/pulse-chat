package com.pulse.chat.domain.chat_core.message.guard;

import com.pulse.chat.app.aop.BusinessException;
import com.pulse.chat.domain.chat_core.message.dto.SendMessageCommand;
import com.pulse.chat.domain.chat_core.message.MessageKeyFactory;
import com.pulse.chat.domain.common.constant.ResponseCode;
import com.pulse.chat.infrastructure.config.prop.MessagePolicyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MessageGuard {

    private final MessagePolicyProperties properties;

    private final MessageRateLimiter messageRateLimiter;
    private final MessageDeduplicator messageDeduplicator;

    public String normalizeContent(String content) {
        return Optional.ofNullable(content)
                .map(String::trim)
                .orElse("");
    }

    public void messageContentInRangeOrError(String content) {
        if (isContentLengthOutOfRange(content)) {
            throw new BusinessException(ResponseCode.INVALID_MESSAGE_CONTENT,
                    properties.minLength(), properties.maxLength());
        }
    }

    public void enforceSendMessagePolicy(SendMessageCommand command) {
        var userKey = MessageKeyFactory.buildMessageUserKey(command);
        var dedupKey = userKey.toDeduplicationKey();
        var rateLimitKey = userKey.toRateLimitKey();

        messageDeduplicator.enforceCooldown(dedupKey, command.getContent());
        messageRateLimiter.enforce(rateLimitKey);
    }

    private boolean isContentLengthOutOfRange(String content) {
        var length = content.length();
        return length < properties.minLength() || length > properties.maxLength();
    }
}
