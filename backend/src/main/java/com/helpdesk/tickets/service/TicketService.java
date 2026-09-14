package com.helpdesk.tickets.service;

import com.helpdesk.auth.Membership;
import com.helpdesk.auth.MembershipRepository;
import com.helpdesk.auth.MembershipRole;
import com.helpdesk.auth.User;
import com.helpdesk.exception.ForbiddenException;
import com.helpdesk.notifications.service.NotificationService;
import com.helpdesk.orgs.OrganizationContext;
import com.helpdesk.orgs.OrganizationContextHolder;
import com.helpdesk.tickets.dto.CreateTicketMessageRequest;
import com.helpdesk.tickets.dto.CreateTicketRequest;
import com.helpdesk.tickets.dto.TicketMessageResponse;
import com.helpdesk.tickets.dto.TicketResponse;
import com.helpdesk.tickets.dto.UpdateTicketRequest;
import com.helpdesk.tickets.entity.Ticket;
import com.helpdesk.tickets.entity.TicketMessage;
import com.helpdesk.tickets.entity.TicketStatus;
import com.helpdesk.tickets.repository.TicketMessageRepository;
import com.helpdesk.tickets.repository.TicketRepository;
import com.helpdesk.websocket.WebSocketSessionManager;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final MembershipRepository membershipRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final WebSocketSessionManager webSocketSessionManager;
    private final NotificationService notificationService;

    public TicketService(
            TicketRepository ticketRepository,
            MembershipRepository membershipRepository,
            TicketMessageRepository ticketMessageRepository,
                WebSocketSessionManager webSocketSessionManager,
                NotificationService notificationService
    ) {
        this.ticketRepository = ticketRepository;
        this.membershipRepository = membershipRepository;
        this.ticketMessageRepository = ticketMessageRepository;
        this.webSocketSessionManager = webSocketSessionManager;
        this.notificationService = notificationService;
    }

    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request) {
        UUID organizationId = getCurrentOrganizationId();
        UUID currentUserId = getCurrentUserId();
        MembershipRole role = getCurrentRole();

        UUID customerId;

        if (role == MembershipRole.CUSTOMER) {
            customerId = currentUserId;
        } else {
            customerId = request.customerId();

            if (customerId == null) {
                throw new IllegalArgumentException(
                        "customerId is required when creating a ticket as an agent"
                );
            }
        }

        Ticket ticket = new Ticket(
                organizationId,
                customerId,
                request.subject(),
                request.priority(),
                request.category()
        );

        Ticket savedTicket = ticketRepository.save(ticket);

        TicketResponse response = TicketResponse.from(savedTicket);

        webSocketSessionManager.broadcastEvent(
            organizationId,
            "ticket.created",
            response
        );

        return response;
    }

    @Transactional
    public List<TicketResponse> getTickets() {
        UUID organizationId = getCurrentOrganizationId();
        UUID currentUserId = getCurrentUserId();
        MembershipRole role = getCurrentRole();

        List<Ticket> tickets;

        if (role == MembershipRole.CUSTOMER) {
            tickets = ticketRepository.findByOrganizationIdAndCustomerId(
                    organizationId,
                    currentUserId
            );
        } else {
            tickets = ticketRepository.findByOrganizationId(organizationId);
        }

        return tickets.stream()
                .map(TicketResponse::from)
                .toList();
    }

    @Transactional
    public TicketResponse getTicket(UUID ticketId) {
        UUID organizationId = getCurrentOrganizationId();
        UUID currentUserId = getCurrentUserId();
        MembershipRole role = getCurrentRole();

        Ticket ticket = ticketRepository
                .findByIdAndOrganizationId(ticketId, organizationId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Ticket not found")
                );

        if (role == MembershipRole.CUSTOMER
                && !ticket.getCustomerId().equals(currentUserId)) {
            throw new IllegalArgumentException("Ticket not found");
        }

        return TicketResponse.from(ticket);
    }

    @Transactional
    public TicketResponse updateTicket(
            UUID ticketId,
            UpdateTicketRequest request
    ) {
        UUID organizationId = getCurrentOrganizationId();
        MembershipRole role = getCurrentRole();

        if (role == MembershipRole.CUSTOMER) {
            throw new ForbiddenException(
                    "Customers cannot update tickets"
            );
        }

        Ticket ticket = ticketRepository
                .findByIdAndOrganizationId(ticketId, organizationId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Ticket not found")
                );

            UUID previousAssignedAgentId = ticket.getAssignedAgentId();

        if (request.status() != null) {
            ticket.setStatus(request.status());
        }

        if (request.priority() != null) {
            ticket.setPriority(request.priority());
        }

        if (request.category() != null) {
            ticket.setCategory(request.category());
        }

        if (request.assignedAgentId() != null) {
            validateAssignedAgent(
                    request.assignedAgentId(),
                    organizationId
            );

            ticket.setAssignedAgentId(request.assignedAgentId());
        }

        ticketRepository.saveAndFlush(ticket);

        UUID newAssignedAgentId = ticket.getAssignedAgentId();

        if (newAssignedAgentId != null
            && !newAssignedAgentId.equals(previousAssignedAgentId)
            && !newAssignedAgentId.equals(getCurrentUserId())) {

            notificationService.createNotification(
                newAssignedAgentId,
                "TICKET_ASSIGNED",
                Map.of(
                    "organizationId", organizationId.toString(),
                    "ticketId", ticket.getId().toString(),
                    "ticketSubject", ticket.getSubject(),
                    "assignedBy", getCurrentUserId().toString()
                )
            );
        }

        TicketResponse response = TicketResponse.from(ticket);

        webSocketSessionManager.broadcastEvent(
            organizationId,
            "ticket.updated",
            response
        );

        return response;
    }

    @Transactional
    public TicketMessageResponse createMessage(
            UUID ticketId,
            CreateTicketMessageRequest request
    ) {
        UUID organizationId = getCurrentOrganizationId();
        UUID currentUserId = getCurrentUserId();
        MembershipRole role = getCurrentRole();

        Ticket ticket = ticketRepository
                .findByIdAndOrganizationId(ticketId, organizationId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Ticket not found")
                );

        if (role == MembershipRole.CUSTOMER
                && !ticket.getCustomerId().equals(currentUserId)) {
            throw new IllegalArgumentException("Ticket not found");
        }

        if (request.internalNote()
                && role == MembershipRole.CUSTOMER) {
            throw new ForbiddenException(
                    "Customers cannot create internal notes"
            );
        }

        TicketMessage message = new TicketMessage(
                ticketId,
                currentUserId,
                request.body(),
                request.internalNote()
        );

        TicketMessage savedMessage =
            ticketMessageRepository.saveAndFlush(message);

        TicketMessageResponse response =
            TicketMessageResponse.from(savedMessage);

        webSocketSessionManager.broadcastEvent(
            organizationId,
            "message.created",
            response
        );

        return response;
    }

    @Transactional
    public List<TicketMessageResponse> getMessages(UUID ticketId) {
        UUID organizationId = getCurrentOrganizationId();
        UUID currentUserId = getCurrentUserId();
        MembershipRole role = getCurrentRole();

        Ticket ticket = ticketRepository
                .findByIdAndOrganizationId(ticketId, organizationId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Ticket not found")
                );

        List<TicketMessage> messages;

        if (role == MembershipRole.CUSTOMER) {

            if (!ticket.getCustomerId().equals(currentUserId)) {
                throw new IllegalArgumentException("Ticket not found");
            }

            messages = ticketMessageRepository
                    .findByTicketIdAndInternalNoteFalseOrderByCreatedAtAscIdAsc(
                            ticketId
                    );

        } else {
            messages = ticketMessageRepository
                    .findByTicketIdOrderByCreatedAtAscIdAsc(ticketId);
        }

        return messages.stream()
                .map(TicketMessageResponse::from)
                .toList();
    }

    private void validateAssignedAgent(
            UUID agentId,
            UUID organizationId
    ) {
        Membership membership = membershipRepository
                .findByUserIdAndOrganizationId(agentId, organizationId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Agent is not a member of this organization"
                        )
                );

        if (membership.getRole() == MembershipRole.CUSTOMER) {
            throw new IllegalArgumentException(
                    "Cannot assign a ticket to a customer"
            );
        }
    }

    private UUID getCurrentOrganizationId() {
        OrganizationContext context = OrganizationContextHolder.get();

        if (context == null) {
            throw new IllegalStateException("Organization context not set");
        }

        return context.getOrganizationId();
    }

    private MembershipRole getCurrentRole() {
        OrganizationContext context = OrganizationContextHolder.get();

        if (context == null) {
            throw new IllegalStateException("Organization context not set");
        }

        return context.getRole();
    }

    private UUID getCurrentUserId() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !(authentication.getPrincipal() instanceof User user)) {
            throw new IllegalStateException("User not authenticated");
        }

        return user.getId();
    }
}
