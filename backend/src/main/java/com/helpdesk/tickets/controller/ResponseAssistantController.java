package com.helpdesk.tickets.controller;

import com.helpdesk.ai.client.dto.ResponseAssistantResponse;
import com.helpdesk.tickets.dto.ApproveAiResponseRequest;
import com.helpdesk.tickets.dto.TicketMessageResponse;
import com.helpdesk.tickets.service.ResponseAssistantService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/orgs/{organizationId}/tickets")
public class ResponseAssistantController {

    private final ResponseAssistantService responseAssistantService;

    public ResponseAssistantController(
            ResponseAssistantService responseAssistantService
    ) {
        this.responseAssistantService = responseAssistantService;
    }

    @PostMapping("/{ticketId}/ai/suggest-response")
    public ResponseAssistantResponse suggestResponse(
            @PathVariable UUID organizationId,
            @PathVariable UUID ticketId
    ) {
        return responseAssistantService.suggestResponse(ticketId);
    }

    @PostMapping(
            value = "/{ticketId}/ai/stream-response",
            produces = "text/event-stream"
    )
    public SseEmitter streamResponse(
            @PathVariable UUID organizationId,
            @PathVariable UUID ticketId
    ) {
        return responseAssistantService.streamResponse(ticketId);
    }


    @PostMapping("/{ticketId}/ai/approve-response")
    public TicketMessageResponse approveResponse(
            @PathVariable UUID organizationId,
            @PathVariable UUID ticketId,
            @Valid @RequestBody ApproveAiResponseRequest request
    ) {
        return responseAssistantService.approveResponse(
                ticketId,
                request.generationId(),
                request.body()
        );
    }
}
