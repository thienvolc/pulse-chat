package com.pulse.chat.domain.events;

public final class EventConstants {
    private EventConstants() {
    }

    public static final String ENVELOPE_TYPE_MESSAGE_CREATED = "MESSAGE_CREATED";

    public static final String OUTBOX_EVENT_TYPE_MESSAGE_CREATED_V1 = "MESSAGE_CREATED_V1";

    public static final String VERSION_V1 = "v1";
    public static final String EVENT_KEY_PREFIX_MESSAGE_CREATED_V1 = "message-created-v1:";
}
