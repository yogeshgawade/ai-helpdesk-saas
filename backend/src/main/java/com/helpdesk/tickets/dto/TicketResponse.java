package com.helpdesk.tickets.dto;

import com.helpdesk.tickets.entity.Ticket;
import com.helpdesk.tickets.entity.TicketPriority;
import com.helpdesk.tickets.entity.TicketStatus;

import java.time.Instant;
import java.util.UUID;

public record TicketResponse(
        UUID id,
        UUID organizationId,
        UUID customerId,
        UUID assignedAgentId,
        String subject,
        TicketStatus status,
        TicketPriority priority,
        String category,
        UUID slaPolicyId,
        Instant createdAt,
        Instant updatedAt,
        Instant resolvedAt
) {

    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getOrganizationId(),
                ticket.getCustomerId(),
                ticket.getAssignedAgentId(),
                ticket.getSubject(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getCategory(),
                ticket.getSlaPolicyId(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getResolvedAt()
        );
    }
}
