package com.helpdesk.tickets.dto;

import java.util.List;

public record TicketListResponse(
        List<TicketResponse> tickets,
        String nextCursor,
        boolean hasMore
) {
}
