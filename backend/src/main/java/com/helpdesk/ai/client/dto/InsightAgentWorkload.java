package com.helpdesk.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record InsightAgentWorkload(
        @JsonProperty("agent_id")
        UUID agentId,

        @JsonProperty("open_tickets")
        long openTickets,

        @JsonProperty("resolved_tickets")
        long resolvedTickets,

        @JsonProperty("total_tickets")
        long totalTickets
) {
}
