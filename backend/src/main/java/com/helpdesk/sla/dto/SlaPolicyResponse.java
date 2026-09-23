package com.helpdesk.sla.dto;

import com.helpdesk.sla.entity.SlaPolicy;
import com.helpdesk.tickets.entity.TicketPriority;

import java.util.UUID;

public record SlaPolicyResponse(
        UUID id,
        UUID organizationId,
        String name,
        Integer firstResponseMinutes,
        Integer resolutionMinutes,
        TicketPriority priority
) {

    public static SlaPolicyResponse from(
            SlaPolicy policy
    ) {
        return new SlaPolicyResponse(
                policy.getId(),
                policy.getOrganizationId(),
                policy.getName(),
                policy.getFirstResponseMinutes(),
                policy.getResolutionMinutes(),
                policy.getPriority()
        );
    }
}
