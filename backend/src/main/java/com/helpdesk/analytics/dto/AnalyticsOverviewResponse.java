package com.helpdesk.analytics.dto;

public record AnalyticsOverviewResponse(
        long totalTickets,
        long openTickets,
        long pendingTickets,
        long resolvedTickets,
        Double averageFirstResponseMinutes,
        Double averageResolutionMinutes,
        Double firstResponseSlaBreachRate,
        Double resolutionSlaBreachRate
) {
}
