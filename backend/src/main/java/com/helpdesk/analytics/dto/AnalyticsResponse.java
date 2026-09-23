package com.helpdesk.analytics.dto;

import java.util.List;

public record AnalyticsResponse(
        AnalyticsOverviewResponse overview,
        List<AnalyticsBreakdownResponse> ticketsByPriority,
        List<AnalyticsBreakdownResponse> ticketsByStatus,
        List<AnalyticsBreakdownResponse> ticketsByCategory,
        List<AnalyticsTimeSeriesResponse> ticketVolume,
        List<AnalyticsAgentWorkloadResponse> agentWorkload
) {
}
