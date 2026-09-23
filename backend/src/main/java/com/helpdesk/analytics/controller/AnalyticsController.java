package com.helpdesk.analytics.controller;

import com.helpdesk.analytics.dto.AnalyticsAiInsightResponse;
import com.helpdesk.analytics.dto.AnalyticsResponse;
import com.helpdesk.analytics.service.AnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/orgs/{organizationId}/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(
            AnalyticsService analyticsService
    ) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/ai-insight")
    public AnalyticsAiInsightResponse getLatestAiInsight(
            @PathVariable UUID organizationId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {
        return analyticsService.getLatestAiInsight(from, to);
    }

    @PostMapping("/ai-insight")
    public AnalyticsAiInsightResponse generateAiInsight(
            @PathVariable UUID organizationId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {
        return analyticsService.generateAiInsight(from, to);
    }

    @GetMapping
    public AnalyticsResponse getAnalytics(
            @PathVariable UUID organizationId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {
        return analyticsService.getAnalytics(from, to);
    }
}
