package com.pulse.chat.domain.chat_core.membership;

import com.pulse.chat.app.aop.BusinessException;
import com.pulse.chat.domain.common.constant.ResponseCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class JoinGroupMemberUseCase {

    @Transactional
    public void execute(UUID requesterId, UUID conversationId) {
        throw new BusinessException(ResponseCode.CONVERSATION_JOIN_UNSUPPORTED);
    }
}
