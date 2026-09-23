package com.helpdesk.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record InsightRequest(
        @JsonProperty("total_tickets")
        long totalTickets,

        @JsonProperty("open_tickets")
        long openTickets,

        @JsonProperty("pending_tickets")
        long pendingTickets,

        @JsonProperty("resolved_tickets")
        long resolvedTickets,

        @JsonProperty("average_first_response_minutes")
        Double averageFirstResponseMinutes,

        @JsonProperty("average_resolution_minutes")
        Double averageResolutionMinutes,

        @JsonProperty("first_response_sla_breach_rate")
        double firstResponseSlaBreachRate,

        @JsonProperty("resolution_sla_breach_rate")
        double resolutionSlaBreachRate,

        @JsonProperty("tickets_by_priority")
        List<InsightBreakdown> ticketsByPriority,

        @JsonProperty("tickets_by_status")
        List<InsightBreakdown> ticketsByStatus,

        @JsonProperty("tickets_by_category")
        List<InsightBreakdown> ticketsByCategory,

        @JsonProperty("ticket_volume")
        List<InsightTimeSeries> ticketVolume,

        @JsonProperty("agent_workload")
        List<InsightAgentWorkload> agentWorkload
) {
}
