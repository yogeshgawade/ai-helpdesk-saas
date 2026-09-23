package com.helpdesk.sla.dto;

import com.helpdesk.tickets.entity.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SlaPolicyRequest(

        @NotBlank(message = "Name is required")
        String name,

        @NotNull(message = "First response minutes is required")
        @Positive(message = "First response minutes must be greater than zero")
        Integer firstResponseMinutes,

        @NotNull(message = "Resolution minutes is required")
        @Positive(message = "Resolution minutes must be greater than zero")
        Integer resolutionMinutes,

        @NotNull(message = "Priority is required")
        TicketPriority priority
) {
}
