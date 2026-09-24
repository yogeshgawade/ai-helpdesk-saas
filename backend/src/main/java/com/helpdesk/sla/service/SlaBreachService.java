package com.helpdesk.sla.service;

import com.helpdesk.notifications.service.NotificationService;
import com.helpdesk.orgs.Organization;
import com.helpdesk.orgs.OrganizationRepository;
import com.helpdesk.orgs.TenantTransactionExecutor;
import com.helpdesk.tickets.entity.Ticket;
import com.helpdesk.tickets.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class SlaBreachService {

    private static final Logger log =
            LoggerFactory.getLogger(SlaBreachService.class);

    private final TicketRepository ticketRepository;
    private final NotificationService notificationService;
    private final OrganizationRepository organizationRepository;
    private final TenantTransactionExecutor tenantTransactionExecutor;

    public SlaBreachService(
            TicketRepository ticketRepository,
            NotificationService notificationService,
            OrganizationRepository organizationRepository,
            TenantTransactionExecutor tenantTransactionExecutor
    ) {
        this.ticketRepository = ticketRepository;
        this.notificationService = notificationService;
        this.organizationRepository = organizationRepository;
        this.tenantTransactionExecutor = tenantTransactionExecutor;
    }

    public void checkForBreaches() {
        Instant now = Instant.now();

        for (Organization organization :
                organizationRepository.findAll()) {

            UUID organizationId = organization.getId();

            try {
                tenantTransactionExecutor.execute(
                        organizationId,
                        () -> checkForBreachesForOrganization(
                                organizationId,
                                now
                        )
                );
            } catch (Exception exception) {
                log.error(
                        "Failed to process SLA breaches for organizationId={}",
                        organizationId,
                        exception
                );
            }
        }
    }

    private void checkForBreachesForOrganization(
            UUID organizationId,
            Instant now
    ) {
        log.debug(
                "Checking SLA breaches organizationId={}",
                organizationId
        );

        checkFirstResponseBreaches(now);
        checkResolutionBreaches(now);
    }

    private void checkFirstResponseBreaches(Instant now) {
        for (Ticket ticket :
                ticketRepository.findTicketsWithBreachedFirstResponseSla(now)) {

            ticket.markFirstResponseBreached(now);
            ticketRepository.save(ticket);

            notifyAssignedAgent(
                    ticket,
                    "SLA_FIRST_RESPONSE_BREACHED",
                    "First response SLA breached"
            );
        }
    }

    private void checkResolutionBreaches(Instant now) {
        for (Ticket ticket :
                ticketRepository.findTicketsWithBreachedResolutionSla(now)) {

            ticket.markResolutionBreached(now);
            ticketRepository.save(ticket);

            notifyAssignedAgent(
                    ticket,
                    "SLA_RESOLUTION_BREACHED",
                    "Resolution SLA breached"
            );
        }
    }

    private void notifyAssignedAgent(
            Ticket ticket,
            String notificationType,
            String message
    ) {
        if (ticket.getAssignedAgentId() == null) {
            return;
        }

        notificationService.createNotification(
                ticket.getAssignedAgentId(),
                notificationType,
                Map.of(
                        "organizationId",
                        ticket.getOrganizationId().toString(),
                        "ticketId",
                        ticket.getId().toString(),
                        "ticketSubject",
                        ticket.getSubject(),
                        "message",
                        message
                )
        );
    }
}
