package com.helpdesk.analytics.dto;

import java.time.LocalDate;

public record AnalyticsTimeSeriesResponse(
        LocalDate date,
        long count
) {
}
