package com.helpdesk.tickets.dto;

import com.helpdesk.tickets.entity.TicketMessage;

import java.time.Instant;
import java.util.UUID;

public record TicketMessageResponse(
        UUID id,
        UUID ticketId,
        UUID authorId,
        String body,
        boolean internalNote,
        boolean aiGenerated,
        Instant createdAt
) {

    public static TicketMessageResponse from(TicketMessage message) {
        return new TicketMessageResponse(
                message.getId(),
                message.getTicketId(),
                message.getAuthorId(),
                message.getBody(),
                message.isInternalNote(),
                message.isAiGenerated(),
                message.getCreatedAt()
        );
    }
}
