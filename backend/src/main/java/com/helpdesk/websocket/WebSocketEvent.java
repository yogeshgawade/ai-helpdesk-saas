package com.helpdesk.websocket;

import java.util.UUID;

public record WebSocketEvent(
        String type,
        UUID organizationId,
        Object data
) {
}
