package com.pulse.chat.domain.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@Getter
@AllArgsConstructor
public enum ResponseCode {
    BAD_CREDENTIALS("ERR_BAD_CREDENTIALS", "bad_credentials", UNAUTHORIZED),
    USERNAME_NOT_FOUND("ERR_USERNAME_NOT_FOUND", "username.not_found", NOT_FOUND),
    USER_ALREADY_EXISTS("ERR_USER_ALREADY_EXISTS", "user.already_exists", CONFLICT),
    INVALID_TOKEN("ERR_INVALID_TOKEN", "token.invalid", UNAUTHORIZED),
    CONVERSATION_NOT_FOUND("ERR_CONVERSATION_NOT_FOUND", "conversation.not_found", NOT_FOUND),
    FORBIDDEN_CONVERSATION_ACCESS("ERR_FORBIDDEN_CONVERSATION_ACCESS", "conversation.forbidden", FORBIDDEN),
    INVALID_DIRECT_CONVERSATION_PARTICIPANTS("ERR_INVALID_DIRECT_CONVERSATION_PARTICIPANTS", "conversation.direct.invalid_participants", BAD_REQUEST),
    INVALID_CONVERSATION_CREATE_REQUEST("ERR_INVALID_CONVERSATION_CREATE_REQUEST", "conversation.create.invalid_request", BAD_REQUEST),
    CONVERSATION_GROUP_REQUIRED("ERR_CONVERSATION_GROUP_REQUIRED", "conversation.group.required", BAD_REQUEST),
    CONVERSATION_OWNER_REQUIRED("ERR_CONVERSATION_OWNER_REQUIRED", "conversation.owner.required", FORBIDDEN),
    CONVERSATION_JOIN_UNSUPPORTED("ERR_CONVERSATION_JOIN_UNSUPPORTED", "conversation.join.unsupported", BAD_REQUEST),
    CONVERSATION_OWNER_CANNOT_LEAVE("ERR_CONVERSATION_OWNER_CANNOT_LEAVE", "conversation.owner.cannot_leave", BAD_REQUEST),
    INVALID_MESSAGE_CONTENT("ERR_INVALID_MESSAGE_CONTENT", "message.invalid_length_%s_%s", BAD_REQUEST),
    MESSAGE_DUPLICATED_TOO_FAST("ERR_MESSAGE_DUPLICATED_TOO_FAST", "message.duplicated_too_fast", TOO_MANY_REQUESTS),
    RATE_LIMIT_EXCEEDED("ERR_RATE_LIMIT_EXCEEDED", "rate_limit.exceeded_%s_%ss", TOO_MANY_REQUESTS),
    REBUILD_IN_PROGRESS("ERR_REBUILD_IN_PROGRESS", "projection.rebuild.in_progress", CONFLICT),
    ACCESS_DENIED("ERR_ACCESS_DENIED", "access_denied", FORBIDDEN),
    INTERNAL_EXCEPTION("ERR_INTERNAL_EXCEPTION", "internal_server_error", INTERNAL_SERVER_ERROR),
    SUCCESS("SUCCESS", "request.ok", OK);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus status;
}
