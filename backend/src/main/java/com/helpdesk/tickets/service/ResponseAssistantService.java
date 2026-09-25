package com.helpdesk.tickets.service;

import com.helpdesk.ai.client.AiServiceClient;
import com.helpdesk.ai.client.dto.ResponseAssistantRequest;
import com.helpdesk.ai.client.dto.ResponseAssistantResponse;
import com.helpdesk.ai.entity.AiGeneration;
import com.helpdesk.ai.repository.AiGenerationRepository;
import com.helpdesk.auth.MembershipRole;
import com.helpdesk.exception.ForbiddenException;
import com.helpdesk.kb.repository.KbChunkSearchResult;
import com.helpdesk.metrics.HelpdeskMetrics;
import com.helpdesk.kb.service.KnowledgeBaseSearchService;
import com.helpdesk.orgs.OrganizationContext;
import com.helpdesk.orgs.OrganizationContextHolder;
import com.helpdesk.tickets.dto.CreateTicketMessageRequest;
import com.helpdesk.tickets.dto.TicketMessageResponse;
import com.helpdesk.tickets.entity.Ticket;
import com.helpdesk.tickets.entity.TicketMessage;
import com.helpdesk.tickets.repository.TicketMessageRepository;
import com.helpdesk.tickets.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.helpdesk.auth.User;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class ResponseAssistantService {

    private final TicketRepository ticketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final KnowledgeBaseSearchService knowledgeBaseSearchService;
    private final AiServiceClient aiServiceClient;
    private final AiGenerationRepository aiGenerationRepository;
    private final TicketService ticketService;
    private final double similarityThreshold;
    private final HelpdeskMetrics helpdeskMetrics;

    public ResponseAssistantService(
            TicketRepository ticketRepository,
            TicketMessageRepository ticketMessageRepository,
            KnowledgeBaseSearchService knowledgeBaseSearchService,
            AiServiceClient aiServiceClient,
            AiGenerationRepository aiGenerationRepository,
            TicketService ticketService,
            @Value("${app.kb.response-assistant.similarity-threshold}") double similarityThreshold,
            HelpdeskMetrics helpdeskMetrics
    ) {
        this.ticketRepository = ticketRepository;
        this.ticketMessageRepository = ticketMessageRepository;
        this.knowledgeBaseSearchService = knowledgeBaseSearchService;
        this.aiServiceClient = aiServiceClient;
        this.aiGenerationRepository = aiGenerationRepository;
        this.ticketService = ticketService;
        this.similarityThreshold = similarityThreshold;
        this.helpdeskMetrics = helpdeskMetrics;
    }

    @Transactional
    public ResponseAssistantResponse suggestResponse(UUID ticketId) {
        UUID organizationId = getCurrentOrganizationId();
        ensureAgentAccess();

        Ticket ticket = ticketRepository
                .findByIdAndOrganizationId(ticketId, organizationId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Ticket not found"));

        List<TicketMessage> messages =
                ticketMessageRepository
                        .findByTicketIdOrderByCreatedAtAscIdAsc(ticketId);

        if (messages.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot generate a response without ticket messages"
            );
        }

        String searchQuery = buildSearchQuery(ticket, messages);

        List<KbChunkSearchResult> searchResults =
                knowledgeBaseSearchService.search(
                        organizationId,
                        searchQuery,
                        5
                );

        List<KbChunkSearchResult> relevantResults =
                searchResults.stream()
                        .filter(result ->
                                result.similarity() >= similarityThreshold)
                        .toList();

        if (relevantResults.isEmpty()) {
            throw new IllegalArgumentException(
                    "No relevant knowledge-base content found"
            );
        }

        List<ResponseAssistantRequest.ResponseAssistantMessage>
                requestMessages =
                messages.stream()
                        .map(message ->
                                new ResponseAssistantRequest
                                        .ResponseAssistantMessage(
                                                message.getBody(),
                                                message.isInternalNote()
                                        )
                        )
                        .toList();

        List<ResponseAssistantRequest.ResponseAssistantChunk>
                requestChunks =
                relevantResults.stream()
                        .map(result ->
                                new ResponseAssistantRequest
                                        .ResponseAssistantChunk(
                                                result.id().toString(),
                                                result.documentId(),
                                                result.documentTitle(),
                                                result.chunkText(),
                                                result.chunkIndex(),
                                                result.similarity()
                                        )
                        )
                        .toList();

        ResponseAssistantRequest request =
                new ResponseAssistantRequest(
                        ticket.getSubject(),
                        ticket.getPriority().name(),
                        ticket.getCategory(),
                        requestMessages,
                        requestChunks
                );

        long startTime = System.nanoTime();

        ResponseAssistantResponse response =
                aiServiceClient.generateResponseAssistant(request);

        long latencyMs =
                (System.nanoTime() - startTime) / 1_000_000;

        AiGeneration generation = new AiGeneration(
                ticketId,
                AiGeneration.Kind.RESPONSE_SUGGESTION,
                response.answer(),
                response.model(),
                latencyMs
        );

        aiGenerationRepository.save(generation);
        helpdeskMetrics.recordAiResponseGeneration(
                Duration.ofMillis(latencyMs)
        );

        return response;
    }

    @Transactional
    public SseEmitter streamResponse(UUID ticketId) {
        UUID organizationId = getCurrentOrganizationId();
        ensureAgentAccess();

        Ticket ticket = ticketRepository
                .findByIdAndOrganizationId(ticketId, organizationId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Ticket not found"));

        List<TicketMessage> messages =
                ticketMessageRepository
                        .findByTicketIdOrderByCreatedAtAscIdAsc(ticketId);

        if (messages.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot generate a response without ticket messages"
            );
        }

        String searchQuery = buildSearchQuery(ticket, messages);

        List<KbChunkSearchResult> searchResults =
                knowledgeBaseSearchService.search(
                        organizationId,
                        searchQuery,
                        5
                );

        List<KbChunkSearchResult> relevantResults =
                searchResults.stream()
                        .filter(result ->
                                result.similarity() >= similarityThreshold)
                        .toList();

        if (relevantResults.isEmpty()) {
            throw new IllegalArgumentException(
                    "No relevant knowledge-base content found"
            );
        }

        List<ResponseAssistantRequest.ResponseAssistantMessage>
                requestMessages =
                messages.stream()
                        .map(message ->
                                new ResponseAssistantRequest
                                        .ResponseAssistantMessage(
                                                message.getBody(),
                                                message.isInternalNote()
                                        )
                        )
                        .toList();

        List<ResponseAssistantRequest.ResponseAssistantChunk>
                requestChunks =
                relevantResults.stream()
                        .map(result ->
                                new ResponseAssistantRequest
                                        .ResponseAssistantChunk(
                                                result.id().toString(),
                                                result.documentId(),
                                                result.documentTitle(),
                                                result.chunkText(),
                                                result.chunkIndex(),
                                                result.similarity()
                                        )
                        )
                        .toList();

        ResponseAssistantRequest request =
                new ResponseAssistantRequest(
                        ticket.getSubject(),
                        ticket.getPriority().name(),
                        ticket.getCategory(),
                        requestMessages,
                        requestChunks
                );

        SseEmitter emitter = new SseEmitter(120_000L);

        SecurityContext securityContext =
                SecurityContextHolder.getContext();

        CompletableFuture.runAsync(
                new DelegatingSecurityContextRunnable(
                        () -> {
                            long startTime = System.nanoTime();
            StringBuilder completeAnswer = new StringBuilder();

            try {
                String model = aiServiceClient.streamResponseAssistant(
                        request,
                        chunk -> {
                            completeAnswer.append(chunk);

                            try {
                                emitter.send(
                                        SseEmitter.event()
                                                .name("chunk")
                                                .data(chunk)
                                );
                            } catch (IOException exception) {
                                throw new IllegalStateException(
                                        "Could not send AI stream chunk",
                                        exception
                                );
                            }
                        }
                );

                long latencyMs =
                        (System.nanoTime() - startTime) / 1_000_000;

                AiGeneration generation = new AiGeneration(
                        ticketId,
                        AiGeneration.Kind.RESPONSE_SUGGESTION,
                        completeAnswer.toString(),
                        model,
                        latencyMs
                );

                aiGenerationRepository.save(generation);
                helpdeskMetrics.recordAiResponseGeneration(
                        Duration.ofMillis(latencyMs)
                );

                List<ResponseAssistantResponse.ResponseAssistantCitation>
                        citations =
                        relevantResults.stream()
                                .map(result ->
                                        new ResponseAssistantResponse
                                                .ResponseAssistantCitation(
                                                        result.documentId(),
                                                        result.documentTitle(),
                                                        result.id(),
                                                        result.chunkIndex()
                                                )
                                )
                                .toList();

                emitter.send(
                        SseEmitter.event()
                                .name("done")
                                .data(
                                        new StreamingResponseComplete(
                                                generation.getId(),
                                                model,
                                                citations
                                        )
                                )
                );

                emitter.complete();

            } catch (Exception exception) {

                try {
                    emitter.send(
                            SseEmitter.event()
                                    .name("error")
                                    .data(
                                            exception.getMessage() != null
                                                    ? exception.getMessage()
                                                    : "AI streaming failed"
                                    )
                    );
                } catch (Exception ignored) {
                    // Client may already have disconnected.
                }

                emitter.completeWithError(exception);
            }
                        })
        );

        return emitter;
    }

    public record StreamingResponseComplete(
            UUID generationId,
            String model,
            List<ResponseAssistantResponse.ResponseAssistantCitation>
                    citations
    ) {}


    @Transactional
    public TicketMessageResponse approveResponse(
            UUID ticketId,
            UUID generationId,
            String body
    ) {
        UUID organizationId = getCurrentOrganizationId();
        ensureAgentAccess();

        Ticket ticket = ticketRepository
                .findByIdAndOrganizationId(ticketId, organizationId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Ticket not found"));

        AiGeneration generation = aiGenerationRepository
                .findByIdAndTicketId(generationId, ticketId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "AI generation not found"
                        ));

        if (generation.getKind()
                != AiGeneration.Kind.RESPONSE_SUGGESTION) {
            throw new IllegalArgumentException(
                    "AI generation is not a response suggestion"
            );
        }

        if (generation.getApprovedBy() != null) {
            throw new IllegalArgumentException(
                    "AI generation has already been approved"
            );
        }

        UUID currentUserId = getCurrentUserId();

        generation.approve(currentUserId);
        aiGenerationRepository.save(generation);

        return ticketService.createMessage(
                ticket.getId(),
                new CreateTicketMessageRequest(
                        body,
                        false
                ),
                true
        );
    }

    private String buildSearchQuery(
            Ticket ticket,
            List<TicketMessage> messages
    ) {
        StringBuilder query = new StringBuilder();

        query.append(ticket.getSubject());

        if (ticket.getCategory() != null) {
            query.append(" ").append(ticket.getCategory());
        }

        for (int i = messages.size() - 1; i >= 0; i--) {
            TicketMessage message = messages.get(i);

            if (!message.isInternalNote()) {
                query.append(" ").append(message.getBody());
                break;
            }
        }

        return query.toString();
    }

    private void ensureAgentAccess() {
        OrganizationContext context =
                OrganizationContextHolder.get();

        if (context == null) {
            throw new IllegalStateException(
                    "Organization context not set"
            );
        }

        if (context.getRole() == MembershipRole.CUSTOMER) {
            throw new ForbiddenException(
                    "Customers cannot use the response assistant"
            );
        }
    }

    private UUID getCurrentOrganizationId() {
        OrganizationContext context =
                OrganizationContextHolder.get();

        if (context == null) {
            throw new IllegalStateException(
                    "Organization context not set"
            );
        }

        return context.getOrganizationId();
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
