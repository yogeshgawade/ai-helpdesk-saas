package com.helpdesk.tickets.dto;

import com.helpdesk.tickets.entity.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateTicketRequest(
        @NotBlank
        @Size(max = 500)
        String subject,

        TicketPriority priority,

        @Size(max = 100)
        String category,

        UUID customerId
) {
}
