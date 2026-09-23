package com.helpdesk.tickets.service;

import com.helpdesk.tickets.dto.TicketListRequest;
import com.helpdesk.tickets.dto.TicketListResponse;
import com.helpdesk.tickets.repository.TicketSpecifications;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Base64;
import com.helpdesk.auth.Membership;
import com.helpdesk.auth.MembershipRepository;
import com.helpdesk.auth.MembershipRole;
import com.helpdesk.auth.User;
import com.helpdesk.exception.ForbiddenException;
import com.helpdesk.notifications.service.NotificationService;
import com.helpdesk.redis.TicketClassificationProducer;
import com.helpdesk.redis.TicketSummarizationProducer;
import com.helpdesk.sla.repository.SlaPolicyRepository;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
    private final TicketClassificationProducer ticketClassificationProducer;
    private final TicketSummarizationProducer ticketSummarizationProducer;
    private final SlaPolicyRepository slaPolicyRepository;

    public TicketService(
            TicketRepository ticketRepository,
            MembershipRepository membershipRepository,
            TicketMessageRepository ticketMessageRepository,
                WebSocketSessionManager webSocketSessionManager,
                NotificationService notificationService,
                TicketClassificationProducer ticketClassificationProducer,
                TicketSummarizationProducer ticketSummarizationProducer,
            SlaPolicyRepository slaPolicyRepository
    ) {
        this.ticketRepository = ticketRepository;
        this.membershipRepository = membershipRepository;
        this.ticketMessageRepository = ticketMessageRepository;
        this.webSocketSessionManager = webSocketSessionManager;
        this.notificationService = notificationService;
        this.ticketClassificationProducer = ticketClassificationProducer;
        this.ticketSummarizationProducer = ticketSummarizationProducer;
        this.slaPolicyRepository = slaPolicyRepository;
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

        slaPolicyRepository
                .findByOrganizationIdAndPriority(
                        organizationId,
                        savedTicket.getPriority()
                )
                .ifPresent(policy -> {
                    Instant createdAt = savedTicket.getCreatedAt();

                    savedTicket.setSlaPolicyId(policy.getId());

                    savedTicket.setFirstResponseDueAt(
                            createdAt.plusSeconds(
                                    policy.getFirstResponseMinutes() * 60L
                            )
                    );

                    savedTicket.setResolutionDueAt(
                            createdAt.plusSeconds(
                                    policy.getResolutionMinutes() * 60L
                            )
                    );

                    ticketRepository.save(savedTicket);
                });

        ticketClassificationProducer.publish(
                savedTicket.getId().toString(),
                organizationId.toString(),
                savedTicket.getSubject(),
                savedTicket.getPriority().name(),
                savedTicket.getCategory()
        );

        TicketResponse response = TicketResponse.from(savedTicket);

        webSocketSessionManager.broadcastEvent(
            organizationId,
            "ticket.created",
            response
        );

        return response;
    }

    @Transactional
    public TicketListResponse getTickets(TicketListRequest request) {
        UUID organizationId = getCurrentOrganizationId();
        UUID currentUserId = getCurrentUserId();
        MembershipRole role = getCurrentRole();

        int limit = request.limit() == null
                ? 20
                : Math.min(Math.max(request.limit(), 1), 100);

        Specification<Ticket> specification =
                TicketSpecifications.belongsToOrganization(organizationId);

        if (role == MembershipRole.CUSTOMER) {
            specification = specification.and(
                    TicketSpecifications.belongsToCustomer(currentUserId)
            );
        }

        if (request.search() != null && !request.search().isBlank()) {
            specification = specification.and(
                    TicketSpecifications.subjectContains(
                            request.search().trim()
                    )
            );
        }

        if (request.status() != null) {
            specification = specification.and(
                    TicketSpecifications.hasStatus(request.status())
            );
        }

        if (request.priority() != null) {
            specification = specification.and(
                    TicketSpecifications.hasPriority(request.priority())
            );
        }

        if (request.category() != null && !request.category().isBlank()) {
            specification = specification.and(
                    TicketSpecifications.hasCategory(
                            request.category().trim()
                    )
            );
        }

        String sort = request.sort() == null || request.sort().isBlank()
                ? "newest"
                : request.sort().trim().toLowerCase();

        if (!sort.equals("newest") && !sort.equals("oldest")) {
            throw new IllegalArgumentException(
                    "Invalid sort. Supported values: newest, oldest"
            );
        }

        boolean ascending = sort.equals("oldest");

        if (request.cursor() != null && !request.cursor().isBlank()) {
            String decoded;

            try {
                decoded = new String(
                        Base64.getUrlDecoder().decode(request.cursor()),
                        java.nio.charset.StandardCharsets.UTF_8
                );
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid ticket cursor");
            }

            String[] parts = decoded.split("\\|", 3);

            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid ticket cursor");
            }

            if (!parts[0].equals(sort)) {
                throw new IllegalArgumentException(
                        "Cursor does not match requested sort"
                );
            }

            Instant cursorCreatedAt;
            UUID cursorId;

            try {
                cursorCreatedAt = Instant.parse(parts[1]);
                cursorId = UUID.fromString(parts[2]);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid ticket cursor");
            }

            specification = specification.and(
                    (root, query, cb) -> {
                        if (ascending) {
                            return cb.or(
                                    cb.greaterThan(
                                            root.get("createdAt"),
                                            cursorCreatedAt
                                    ),
                                    cb.and(
                                            cb.equal(
                                                    root.get("createdAt"),
                                                    cursorCreatedAt
                                            ),
                                            cb.greaterThan(
                                                    root.get("id"),
                                                    cursorId
                                            )
                                    )
                            );
                        }

                        return cb.or(
                                cb.lessThan(
                                        root.get("createdAt"),
                                        cursorCreatedAt
                                ),
                                cb.and(
                                        cb.equal(
                                                root.get("createdAt"),
                                                cursorCreatedAt
                                        ),
                                        cb.lessThan(
                                                root.get("id"),
                                                cursorId
                                        )
                                )
                        );
                    }
            );
        }

        Sort sortOrder = ascending
                ? Sort.by(
                        Sort.Order.asc("createdAt"),
                        Sort.Order.asc("id")
                )
                : Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                );

        PageRequest pageRequest = PageRequest.of(
                0,
                limit + 1,
                sortOrder
        );

        List<Ticket> tickets = ticketRepository
                .findAll(specification, pageRequest)
                .getContent();

        boolean hasMore = tickets.size() > limit;

        if (hasMore) {
            tickets = tickets.subList(0, limit);
        }

        String nextCursor = null;

        if (hasMore && !tickets.isEmpty()) {
            Ticket lastTicket = tickets.get(tickets.size() - 1);

            String cursorValue =
                    sort
                            + "|"
                            + lastTicket.getCreatedAt()
                            + "|"
                            + lastTicket.getId();

            nextCursor = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(
                            cursorValue.getBytes(
                                    java.nio.charset.StandardCharsets.UTF_8
                            )
                    );
        }

        return new TicketListResponse(
                tickets.stream()
                        .map(TicketResponse::from)
                        .toList(),
                nextCursor,
                hasMore
        );
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
        return createMessage(ticketId, request, false);
    }

    @Transactional
    public TicketMessageResponse createMessage(
            UUID ticketId,
            CreateTicketMessageRequest request,
            boolean aiGenerated
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
                request.internalNote(),
                aiGenerated
        );

        TicketMessage savedMessage =
            ticketMessageRepository.saveAndFlush(message);

        if (!request.internalNote()
                && role != MembershipRole.CUSTOMER
                && ticket.getFirstRespondedAt() == null
                && ticket.getFirstResponseDueAt() != null) {

            ticket.markFirstResponded(savedMessage.getCreatedAt());
            ticketRepository.save(ticket);
        }

        if (!request.internalNote()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            ticketSummarizationProducer.publish(
                                    ticketId.toString(),
                                    organizationId.toString()
                            );
                        }
                    }
            );
        }

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
