package com.helpdesk.analytics.service;

import com.helpdesk.ai.client.AiServiceClient;
import com.helpdesk.ai.client.dto.InsightRequest;
import com.helpdesk.ai.client.dto.InsightResponse;
import com.helpdesk.analytics.entity.AnalyticsAiInsight;
import com.helpdesk.analytics.repository.AnalyticsAiInsightRepository;
import com.helpdesk.analytics.repository.AnalyticsRepository;
import com.helpdesk.auth.MembershipRole;
import com.helpdesk.orgs.OrganizationContext;
import com.helpdesk.orgs.OrganizationContextHolder;
import com.helpdesk.tickets.entity.TicketStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private AnalyticsRepository analyticsRepository;

    @Mock
    private AiServiceClient aiServiceClient;

    @Mock
    private AnalyticsAiInsightRepository analyticsAiInsightRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    @AfterEach
    void tearDown() {
        OrganizationContextHolder.clear();
    }

    @Test
    void getAnalyticsUsesOrganizationAndDateRangeFromContext() {
        UUID organizationId = UUID.randomUUID();

        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 22);

        Instant fromInstant =
                from.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();

        Instant toInstant =
                to.plusDays(1)
                        .atStartOfDay(java.time.ZoneOffset.UTC)
                        .toInstant();

        OrganizationContextHolder.set(
                new OrganizationContext(
                        organizationId,
                        MembershipRole.AGENT
                )
        );

        when(analyticsRepository.countTickets(
                organizationId,
                fromInstant,
                toInstant
        )).thenReturn(10L);

        when(analyticsRepository.countTicketsByStatus(
                organizationId,
                TicketStatus.OPEN,
                fromInstant,
                toInstant
        )).thenReturn(3L);

        when(analyticsRepository.countTicketsByStatus(
                organizationId,
                TicketStatus.PENDING,
                fromInstant,
                toInstant
        )).thenReturn(2L);

        when(analyticsRepository.countTicketsByStatus(
                organizationId,
                TicketStatus.RESOLVED,
                fromInstant,
                toInstant
        )).thenReturn(5L);

        when(analyticsRepository.averageFirstResponseMinutes(
                organizationId,
                fromInstant,
                toInstant
        )).thenReturn(12.5);

        when(analyticsRepository.averageResolutionMinutes(
                organizationId,
                fromInstant,
                toInstant
        )).thenReturn(120.0);

        when(analyticsRepository.countTicketsWithFirstResponseSla(
                organizationId,
                fromInstant,
                toInstant
        )).thenReturn(10L);

        when(analyticsRepository.countFirstResponseSlaBreaches(
                organizationId,
                fromInstant,
                toInstant
        )).thenReturn(2L);

        when(analyticsRepository.countTicketsWithResolutionSla(
                organizationId,
                fromInstant,
                toInstant
        )).thenReturn(10L);

        when(analyticsRepository.countResolutionSlaBreaches(
                organizationId,
                fromInstant,
                toInstant
        )).thenReturn(1L);

        when(analyticsRepository.countTicketsByPriority(
                organizationId,
                fromInstant,
                toInstant
        )).thenReturn(List.of());

        when(analyticsRepository.countTicketsByStatus(
                organizationId,
                fromInstant,
                toInstant
        )).thenReturn(List.of());

        when(analyticsRepository.countTicketsByCategory(
                organizationId,
                fromInstant,
                toInstant
        )).thenReturn(List.of());

        when(analyticsRepository.getTicketVolume(
                organizationId,
                fromInstant,
                toInstant
        )).thenReturn(List.of());

        when(analyticsRepository.getAgentWorkload(
                organizationId,
                fromInstant,
                toInstant
        )).thenReturn(List.of());

        var response = analyticsService.getAnalytics(from, to);

        assertEquals(10L, response.overview().totalTickets());
        assertEquals(3L, response.overview().openTickets());
        assertEquals(2L, response.overview().pendingTickets());
        assertEquals(5L, response.overview().resolvedTickets());

        assertEquals(
                20.0,
                response.overview().firstResponseSlaBreachRate()
        );

        assertEquals(
                10.0,
                response.overview().resolutionSlaBreachRate()
        );

        verify(analyticsRepository).countTickets(
                organizationId,
                fromInstant,
                toInstant
        );

        verify(analyticsRepository).countTicketsByStatus(
                organizationId,
                TicketStatus.OPEN,
                fromInstant,
                toInstant
        );

        verify(analyticsRepository).countTicketsByStatus(
                organizationId,
                TicketStatus.PENDING,
                fromInstant,
                toInstant
        );

        verify(analyticsRepository).countTicketsByStatus(
                organizationId,
                TicketStatus.RESOLVED,
                fromInstant,
                toInstant
        );
    }

    @Test
    void generateAiInsightUsesAnalyticsAndPersistsInsight() {
        UUID organizationId = UUID.randomUUID();

        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 22);

        Instant fromInstant =
                from.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();

        Instant toInstant =
                to.plusDays(1)
                        .atStartOfDay(java.time.ZoneOffset.UTC)
                        .toInstant();

        OrganizationContextHolder.set(
                new OrganizationContext(
                        organizationId,
                        MembershipRole.ADMIN
                )
        );

        when(analyticsRepository.countTickets(
                organizationId,
                fromInstant,
                toInstant
        )).thenReturn(10L);

        when(analyticsRepository.countTicketsByStatus(
                organizationId,
                TicketStatus.OPEN,
                fromInstant,
                toInstant
        )).thenReturn(3L);

        when(analyticsRepository.countTicketsByStatus(
                organizationId,
                TicketStatus.PENDING,
                fromInstant,
                toInstant
        )).thenReturn(2L);

        when(analyticsRepository.countTicketsByStatus(
                organizationId,
                TicketStatus.RESOLVED,
                fromInstant,
                toInstant
        )).thenReturn(5L);

        when(analyticsRepository.averageFirstResponseMinutes(
                organizationId,
                fromInstant,
                toInstant
        )).thenReturn(12.5);

        when(analyticsRepository.averageResolutionMinutes(
                organizationId,
                fromInstant,
                toInstant
        )).thenReturn(120.0);

        when(analyticsRepository.countTicketsWithFirstResponseSla(
                organizationId,
                fromInstant,
                toInstant
        )).thenReturn(10L);

        when(analyticsRepository.countFirstResponseSlaBreaches(
                organizationId,
                fromInstant,
                toInstant
        )).thenReturn(2L);

        when(analyticsRepository.countTicketsWithResolutionSla(
                organizationId,
                fromInstant,
                toInstant
        )).thenReturn(10L);

        when(analyticsRepository.countResolutionSlaBreaches(
                organizationId,
                fromInstant,
                toInstant
        )).thenReturn(1L);

        when(analyticsRepository.countTicketsByPriority(
                organizationId,
                fromInstant,
                toInstant
        )).thenReturn(List.of());

        when(analyticsRepository.countTicketsByStatus(
                organizationId,
                fromInstant,
                toInstant
        )).thenReturn(List.of());

        when(analyticsRepository.countTicketsByCategory(
                organizationId,
                fromInstant,
                toInstant
        )).thenReturn(List.of());

        when(analyticsRepository.getTicketVolume(
                organizationId,
                fromInstant,
                toInstant
        )).thenReturn(List.of());

        when(analyticsRepository.getAgentWorkload(
                organizationId,
                fromInstant,
                toInstant
        )).thenReturn(List.of());

        when(aiServiceClient.generateInsight(any(InsightRequest.class)))
                .thenReturn(
                        new InsightResponse(
                                "SLA performance requires attention.",
                                "gemini-test"
                        )
                );

        AnalyticsAiInsight savedInsight =
                new AnalyticsAiInsight(
                        organizationId,
                        from,
                        to,
                        "SLA performance requires attention.",
                        "gemini-test"
                );

        when(analyticsAiInsightRepository.save(
                any(AnalyticsAiInsight.class)
        )).thenReturn(savedInsight);

        var response =
                analyticsService.generateAiInsight(from, to);

        assertEquals(
                "SLA performance requires attention.",
                response.insight()
        );

        assertEquals(
                "gemini-test",
                response.model()
        );

        verify(aiServiceClient).generateInsight(
                any(InsightRequest.class)
        );

        verify(analyticsAiInsightRepository).save(
                any(AnalyticsAiInsight.class)
        );
    }

    @Test
    void getAnalyticsFailsWhenOrganizationContextIsMissing() {
        assertThrows(
                IllegalStateException.class,
                () -> analyticsService.getAnalytics(
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 22)
                )
        );
    }

    @Test
    void getAnalyticsRejectsCustomerRole() {
        UUID organizationId = UUID.randomUUID();

        OrganizationContextHolder.set(
                new OrganizationContext(
                        organizationId,
                        MembershipRole.CUSTOMER
                )
        );

        assertThrows(
                org.springframework.security.access.AccessDeniedException.class,
                () -> analyticsService.getAnalytics(
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 22)
                )
        );
    }

    @Test
    void getAnalyticsRejectsInvalidDateRange() {
        UUID organizationId = UUID.randomUUID();

        OrganizationContextHolder.set(
                new OrganizationContext(
                        organizationId,
                        MembershipRole.AGENT
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> analyticsService.getAnalytics(
                        LocalDate.of(2026, 9, 23),
                        LocalDate.of(2026, 9, 22)
                )
        );
    }
}
