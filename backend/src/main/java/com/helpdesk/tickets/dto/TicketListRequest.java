package com.helpdesk.tickets.dto;

import com.helpdesk.tickets.entity.TicketPriority;
import com.helpdesk.tickets.entity.TicketStatus;

public record TicketListRequest(
        String search,
        TicketStatus status,
        TicketPriority priority,
        String category,
        String sort,
        String cursor,
        Integer limit
) {
}
