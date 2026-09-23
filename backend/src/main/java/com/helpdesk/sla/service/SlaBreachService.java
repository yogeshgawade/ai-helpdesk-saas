package com.helpdesk.sla.service;

import com.helpdesk.notifications.service.NotificationService;
import com.helpdesk.tickets.entity.Ticket;
import com.helpdesk.tickets.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class SlaBreachService {

    private final TicketRepository ticketRepository;
    private final NotificationService notificationService;

    public SlaBreachService(
            TicketRepository ticketRepository,
            NotificationService notificationService
    ) {
        this.ticketRepository = ticketRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public void checkForBreaches() {
        Instant now = Instant.now();

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
