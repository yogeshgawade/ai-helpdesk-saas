package com.helpdesk.analytics.service;

import com.helpdesk.ai.client.AiServiceClient;
import com.helpdesk.ai.client.dto.InsightAgentWorkload;
import com.helpdesk.ai.client.dto.InsightBreakdown;
import com.helpdesk.ai.client.dto.InsightRequest;
import com.helpdesk.ai.client.dto.InsightResponse;
import com.helpdesk.ai.client.dto.InsightTimeSeries;
import com.helpdesk.analytics.dto.AnalyticsAgentWorkloadResponse;
import com.helpdesk.analytics.dto.AnalyticsAiInsightResponse;
import com.helpdesk.analytics.dto.AnalyticsBreakdownResponse;
import com.helpdesk.analytics.dto.AnalyticsOverviewResponse;
import com.helpdesk.analytics.dto.AnalyticsResponse;
import com.helpdesk.analytics.dto.AnalyticsTimeSeriesResponse;
import com.helpdesk.analytics.entity.AnalyticsAiInsight;
import com.helpdesk.analytics.repository.AnalyticsAiInsightRepository;
import com.helpdesk.analytics.repository.AnalyticsRepository;
import com.helpdesk.auth.MembershipRole;
import com.helpdesk.orgs.OrganizationContext;
import com.helpdesk.orgs.OrganizationContextHolder;
import com.helpdesk.tickets.entity.TicketPriority;
import com.helpdesk.tickets.entity.TicketStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;
    private final AiServiceClient aiServiceClient;
    private final AnalyticsAiInsightRepository analyticsAiInsightRepository;

    public AnalyticsService(
            AnalyticsRepository analyticsRepository,
            AiServiceClient aiServiceClient,
            AnalyticsAiInsightRepository analyticsAiInsightRepository
    ) {
        this.analyticsRepository = analyticsRepository;
        this.aiServiceClient = aiServiceClient;
        this.analyticsAiInsightRepository = analyticsAiInsightRepository;
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(
            LocalDate from,
            LocalDate to
    ) {

        OrganizationContext context =
                OrganizationContextHolder.get();

        if (context == null) {
            throw new IllegalStateException(
                    "Organization context not set"
            );
        }

        UUID organizationId = context.getOrganizationId();

        if (context.getRole() != MembershipRole.AGENT
                && context.getRole() != MembershipRole.ADMIN
                && context.getRole() != MembershipRole.OWNER) {
            throw new AccessDeniedException(
                    "Insufficient permissions for analytics"
            );
        }

        LocalDate effectiveFrom =
                from != null ? from : LocalDate.of(1970, 1, 1);

        LocalDate effectiveTo =
                to != null ? to : LocalDate.now();

        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new IllegalArgumentException(
                    "Analytics 'from' date must not be after 'to' date"
            );
        }

        Instant fromInstant =
                effectiveFrom.atStartOfDay(ZoneOffset.UTC).toInstant();

        Instant toInstant =
                effectiveTo.plusDays(1)
                        .atStartOfDay(ZoneOffset.UTC)
                        .toInstant();

        long totalTickets =
                analyticsRepository.countTickets(
                        organizationId,
                        fromInstant,
                        toInstant
                );

        long openTickets =
                analyticsRepository.countTicketsByStatus(
                        organizationId,
                        TicketStatus.OPEN,
                        fromInstant,
                        toInstant
                );

        long pendingTickets =
                analyticsRepository.countTicketsByStatus(
                        organizationId,
                        TicketStatus.PENDING,
                        fromInstant,
                        toInstant
                );

        long resolvedTickets =
                analyticsRepository.countTicketsByStatus(
                        organizationId,
                        TicketStatus.RESOLVED,
                        fromInstant,
                        toInstant
                );

        Double averageFirstResponseMinutes =
                analyticsRepository.averageFirstResponseMinutes(
                        organizationId,
                        fromInstant,
                        toInstant
                );

        Double averageResolutionMinutes =
                analyticsRepository.averageResolutionMinutes(
                        organizationId,
                        fromInstant,
                        toInstant
                );

        long firstResponseSlaTickets =
                analyticsRepository.countTicketsWithFirstResponseSla(
                        organizationId,
                        fromInstant,
                        toInstant
                );

        long firstResponseSlaBreaches =
                analyticsRepository.countFirstResponseSlaBreaches(
                        organizationId,
                        fromInstant,
                        toInstant
                );

        long resolutionSlaTickets =
                analyticsRepository.countTicketsWithResolutionSla(
                        organizationId,
                        fromInstant,
                        toInstant
                );

        long resolutionSlaBreaches =
                analyticsRepository.countResolutionSlaBreaches(
                        organizationId,
                        fromInstant,
                        toInstant
                );

        double firstResponseSlaBreachRate =
                calculateRate(
                        firstResponseSlaBreaches,
                        firstResponseSlaTickets
                );

        double resolutionSlaBreachRate =
                calculateRate(
                        resolutionSlaBreaches,
                        resolutionSlaTickets
                );

        AnalyticsOverviewResponse overview =
                new AnalyticsOverviewResponse(
                        totalTickets,
                        openTickets,
                        pendingTickets,
                        resolvedTickets,
                        averageFirstResponseMinutes,
                        averageResolutionMinutes,
                        firstResponseSlaBreachRate,
                        resolutionSlaBreachRate
                );

        List<AnalyticsBreakdownResponse> ticketsByPriority =
                analyticsRepository
                        .countTicketsByPriority(
                                organizationId,
                                fromInstant,
                                toInstant
                        )
                        .stream()
                        .map(row ->
                                new AnalyticsBreakdownResponse(
                                        ((TicketPriority) row[0]).name(),
                                        ((Number) row[1]).longValue()
                                )
                        )
                        .toList();

        List<AnalyticsBreakdownResponse> ticketsByStatus =
                analyticsRepository
                        .countTicketsByStatus(
                                organizationId,
                                fromInstant,
                                toInstant
                        )
                        .stream()
                        .map(row ->
                                new AnalyticsBreakdownResponse(
                                        ((TicketStatus) row[0]).name(),
                                        ((Number) row[1]).longValue()
                                )
                        )
                        .toList();

        List<AnalyticsBreakdownResponse> ticketsByCategory =
                analyticsRepository
                        .countTicketsByCategory(
                                organizationId,
                                fromInstant,
                                toInstant
                        )
                        .stream()
                        .map(row ->
                                new AnalyticsBreakdownResponse(
                                        (String) row[0],
                                        ((Number) row[1]).longValue()
                                )
                        )
                        .toList();

        List<AnalyticsTimeSeriesResponse> ticketVolume =
                analyticsRepository
                        .getTicketVolume(
                                organizationId,
                                fromInstant,
                                toInstant
                        )
                        .stream()
                        .map(row ->
                                new AnalyticsTimeSeriesResponse(
                                        toLocalDate(row[0]),
                                        ((Number) row[1]).longValue()
                                )
                        )
                        .toList();

        List<AnalyticsAgentWorkloadResponse> agentWorkload =
                analyticsRepository
                        .getAgentWorkload(
                                organizationId,
                                fromInstant,
                                toInstant
                        )
                        .stream()
                        .map(row ->
                                new AnalyticsAgentWorkloadResponse(
                                        (UUID) row[0],
                                        ((Number) row[1]).longValue(),
                                        ((Number) row[2]).longValue(),
                                        ((Number) row[3]).longValue()
                                )
                        )
                        .toList();

        return new AnalyticsResponse(
                overview,
                ticketsByPriority,
                ticketsByStatus,
                ticketsByCategory,
                ticketVolume,
                agentWorkload
        );
    }

    @Transactional(readOnly = true)
    public AnalyticsAiInsightResponse getLatestAiInsight(
            LocalDate from,
            LocalDate to
    ) {
        OrganizationContext context =
                OrganizationContextHolder.get();

        if (context == null) {
            throw new IllegalStateException(
                    "Organization context not set"
            );
        }

        if (context.getRole() != MembershipRole.AGENT
                && context.getRole() != MembershipRole.ADMIN
                && context.getRole() != MembershipRole.OWNER) {
            throw new AccessDeniedException(
                    "Insufficient permissions for analytics"
            );
        }

        LocalDate effectiveFrom =
                from != null ? from : LocalDate.of(1970, 1, 1);

        LocalDate effectiveTo =
                to != null ? to : LocalDate.now();

        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new IllegalArgumentException(
                    "Analytics 'from' date must not be after 'to' date"
            );
        }

        return analyticsAiInsightRepository
                .findFirstByOrganizationIdAndFromDateAndToDateOrderByCreatedAtDesc(
                        context.getOrganizationId(),
                        effectiveFrom,
                        effectiveTo
                )
                .map(insight ->
                        new AnalyticsAiInsightResponse(
                                insight.getId(),
                                insight.getInsight(),
                                insight.getModel(),
                                insight.getCreatedAt()
                        )
                )
                .orElse(null);
    }

    @Transactional
    public AnalyticsAiInsightResponse generateAiInsight(
            LocalDate from,
            LocalDate to
    ) {
        OrganizationContext context =
                OrganizationContextHolder.get();

        if (context == null) {
            throw new IllegalStateException(
                    "Organization context not set"
            );
        }

        if (context.getRole() != MembershipRole.AGENT
                && context.getRole() != MembershipRole.ADMIN
                && context.getRole() != MembershipRole.OWNER) {
            throw new AccessDeniedException(
                    "Insufficient permissions for analytics"
            );
        }

        LocalDate effectiveFrom =
                from != null ? from : LocalDate.of(1970, 1, 1);

        LocalDate effectiveTo =
                to != null ? to : LocalDate.now();

        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new IllegalArgumentException(
                    "Analytics 'from' date must not be after 'to' date"
            );
        }

        AnalyticsResponse analytics =
                getAnalytics(effectiveFrom, effectiveTo);

        AnalyticsOverviewResponse overview =
                analytics.overview();

        InsightRequest request =
                new InsightRequest(
                        overview.totalTickets(),
                        overview.openTickets(),
                        overview.pendingTickets(),
                        overview.resolvedTickets(),
                        overview.averageFirstResponseMinutes(),
                        overview.averageResolutionMinutes(),
                        overview.firstResponseSlaBreachRate(),
                        overview.resolutionSlaBreachRate(),
                        analytics.ticketsByPriority()
                                .stream()
                                .map(item ->
                                        new InsightBreakdown(
                                                item.name(),
                                                item.count()
                                        )
                                )
                                .toList(),
                        analytics.ticketsByStatus()
                                .stream()
                                .map(item ->
                                        new InsightBreakdown(
                                                item.name(),
                                                item.count()
                                        )
                                )
                                .toList(),
                        analytics.ticketsByCategory()
                                .stream()
                                .map(item ->
                                        new InsightBreakdown(
                                                item.name(),
                                                item.count()
                                        )
                                )
                                .toList(),
                        analytics.ticketVolume()
                                .stream()
                                .map(item ->
                                        new InsightTimeSeries(
                                                item.date().toString(),
                                                item.count()
                                        )
                                )
                                .toList(),
                        analytics.agentWorkload()
                                .stream()
                                .map(item ->
                                        new InsightAgentWorkload(
                                                item.agentId(),
                                                item.openTickets(),
                                                item.resolvedTickets(),
                                                item.totalTickets()
                                        )
                                )
                                .toList()
                );

        InsightResponse response =
                aiServiceClient.generateInsight(request);

        UUID organizationId =
                context.getOrganizationId();

        AnalyticsAiInsight savedInsight =
                analyticsAiInsightRepository.save(
                        new AnalyticsAiInsight(
                                organizationId,
                                effectiveFrom,
                                effectiveTo,
                                response.insight(),
                                response.model()
                        )
                );

        return new AnalyticsAiInsightResponse(
                savedInsight.getId(),
                savedInsight.getInsight(),
                savedInsight.getModel(),
                savedInsight.getCreatedAt()
        );
    }

    private double calculateRate(
            long breaches,
            long total
    ) {
        if (total == 0) {
            return 0.0;
        }

        return (breaches * 100.0) / total;
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof Date date) {
            return date.toLocalDate();
        }

        if (value instanceof LocalDate localDate) {
            return localDate;
        }

        return LocalDate.parse(value.toString());
    }
}
