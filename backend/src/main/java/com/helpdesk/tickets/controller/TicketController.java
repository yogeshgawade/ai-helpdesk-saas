package com.helpdesk.tickets.controller;

import com.helpdesk.tickets.dto.CreateTicketMessageRequest;
import com.helpdesk.tickets.dto.CreateTicketRequest;
import com.helpdesk.tickets.dto.TicketMessageResponse;
import com.helpdesk.tickets.dto.TicketListRequest;
import com.helpdesk.tickets.dto.TicketListResponse;
import com.helpdesk.tickets.dto.TicketResponse;
import com.helpdesk.tickets.dto.UpdateTicketRequest;
import com.helpdesk.tickets.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orgs/{organizationId}/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse createTicket(
            @PathVariable UUID organizationId,
            @Valid @RequestBody CreateTicketRequest request
    ) {
        return ticketService.createTicket(request);
    }

    @GetMapping
    public TicketListResponse getTickets(
            @PathVariable UUID organizationId,
            @ModelAttribute TicketListRequest request
    ) {
        return ticketService.getTickets(request);
    }

    @GetMapping("/{ticketId}")
    public TicketResponse getTicket(
            @PathVariable UUID organizationId,
            @PathVariable UUID ticketId
    ) {
        return ticketService.getTicket(ticketId);
    }

    @PatchMapping("/{ticketId}")
    public TicketResponse updateTicket(
            @PathVariable UUID organizationId,
            @PathVariable UUID ticketId,
            @Valid @RequestBody UpdateTicketRequest request
    ) {
        return ticketService.updateTicket(ticketId, request);
    }

    @PostMapping("/{ticketId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketMessageResponse createMessage(
            @PathVariable UUID organizationId,
            @PathVariable UUID ticketId,
            @Valid @RequestBody CreateTicketMessageRequest request
    ) {
        return ticketService.createMessage(ticketId, request);
    }

    @GetMapping("/{ticketId}/messages")
    public List<TicketMessageResponse> getMessages(
            @PathVariable UUID organizationId,
            @PathVariable UUID ticketId
    ) {
        return ticketService.getMessages(ticketId);
    }
}
