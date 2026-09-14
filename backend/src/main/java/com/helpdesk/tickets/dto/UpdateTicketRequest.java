package com.helpdesk.tickets.dto;

import com.helpdesk.tickets.entity.TicketPriority;
import com.helpdesk.tickets.entity.TicketStatus;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateTicketRequest(
        TicketStatus status,

        TicketPriority priority,

        @Size(max = 100)
        String category,

        UUID assignedAgentId
) {
}
