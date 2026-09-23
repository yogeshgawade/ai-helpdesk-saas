package com.helpdesk.analytics.dto;

import java.time.Instant;
import java.util.UUID;

public record AnalyticsAiInsightResponse(
        UUID insightId,
        String insight,
        String model,
        Instant createdAt
) {
}
