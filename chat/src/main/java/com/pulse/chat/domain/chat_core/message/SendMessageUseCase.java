package com.pulse.chat.domain.chat_core.message;

import com.pulse.chat.domain.chat_core.message.dto.SendMessageCommand;
import com.pulse.chat.domain.chat_core.message.dto.response.MessageResponse;
import com.pulse.chat.domain.chat_core.message.lock.IdempotencyLockManager;
import com.pulse.chat.domain.chat_core.conversation.guard.ConversationGuard;
import com.pulse.chat.domain.chat_core.message.guard.MessageGuard;
import com.pulse.chat.domain.chat_core.message.guard.MessageIdempotencyGuard;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SendMessageUseCase {

    private final MessageGuard messageGuard;
    private final ConversationGuard conversationGuard;
    private final MessageIdempotencyGuard idempotencyGuard;

    private final SendMessageTxUseCase sendMessageTxUseCase;

    private final IdempotencyLockManager lockManager;

    public MessageResponse execute(UUID senderId,
                                   UUID conversationId,
                                   String content,
                                   @Nullable String idempotencyKey) {

        conversationGuard.memberBelongsToConversationOrError(senderId, conversationId);

        Optional<String> normalizedKey = idempotencyGuard.normalizeIdempotencyKey(idempotencyKey);
        String normalizedContent = messageGuard.normalizeContent(content);
        messageGuard.messageContentInRangeOrError(normalizedContent);

        SendMessageCommand command = SendMessageCommand.builder()
                .senderId(senderId)
                .conversationId(conversationId)
                .content(normalizedContent)
                .idempotencyKey(normalizedKey.orElse(null))
                .build();

        if (normalizedKey.isEmpty()) {
            return sendMessageTxUseCase.send(command);
        }

        String lockKey = MessageKeyFactory.buildLockMessageKey(command);
        return lockManager.executeWithLock(lockKey, () -> sendMessageTxUseCase.send(command));
    }
}
