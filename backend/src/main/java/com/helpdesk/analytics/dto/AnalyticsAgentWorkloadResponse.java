package com.helpdesk.analytics.dto;

import java.util.UUID;

public record AnalyticsAgentWorkloadResponse(
        UUID agentId,
        long openTickets,
        long resolvedTickets,
        long totalTickets
) {
}
