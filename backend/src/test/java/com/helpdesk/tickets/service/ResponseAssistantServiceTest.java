package com.helpdesk.tickets.service;

import com.helpdesk.exception.ForbiddenException;

import com.helpdesk.ai.client.AiServiceClient;
import com.helpdesk.ai.client.dto.ResponseAssistantRequest;
import com.helpdesk.ai.client.dto.ResponseAssistantResponse;
import com.helpdesk.ai.entity.AiGeneration;
import com.helpdesk.ai.repository.AiGenerationRepository;
import com.helpdesk.auth.MembershipRole;
import com.helpdesk.kb.repository.KbChunkSearchResult;
import com.helpdesk.kb.service.KnowledgeBaseSearchService;
import com.helpdesk.orgs.OrganizationContext;
import com.helpdesk.orgs.OrganizationContextHolder;
import com.helpdesk.tickets.entity.Ticket;
import com.helpdesk.tickets.entity.TicketMessage;
import com.helpdesk.tickets.entity.TicketPriority;
import com.helpdesk.tickets.dto.CreateTicketMessageRequest;
import com.helpdesk.tickets.dto.TicketMessageResponse;
import com.helpdesk.tickets.repository.TicketMessageRepository;
import com.helpdesk.tickets.repository.TicketRepository;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import com.helpdesk.auth.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResponseAssistantServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketMessageRepository ticketMessageRepository;

    @Mock
    private KnowledgeBaseSearchService knowledgeBaseSearchService;

    @Mock
    private AiServiceClient aiServiceClient;

    @Mock
    private AiGenerationRepository aiGenerationRepository;

    @Mock
    private TicketService ticketService;

    private ResponseAssistantService service;

    private UUID organizationId;
    private UUID ticketId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        ticketId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        service = new ResponseAssistantService(
                ticketRepository,
                ticketMessageRepository,
                knowledgeBaseSearchService,
                aiServiceClient,
                aiGenerationRepository,
                ticketService,
                0.75
        );

        OrganizationContextHolder.set(
                new OrganizationContext(
                        organizationId,
                        MembershipRole.AGENT
                )
        );
    }

    @AfterEach
    void tearDown() {
        OrganizationContextHolder.clear();
    }

    @Test
    void suggestResponse_shouldBuildRequestCallAiAndPersistGeneration() {
        Ticket ticket = new Ticket(
                organizationId,
                customerId,
                "Refund not received",
                TicketPriority.HIGH,
                "Billing"
        );

        TicketMessage message = new TicketMessage(
                ticketId,
                customerId,
                "I requested a refund five days ago but have not received it.",
                false
        );

        UUID chunkId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        KbChunkSearchResult relevantChunk =
                new KbChunkSearchResult(
                        chunkId,
                        documentId,
                        organizationId,
                        "Refund Policy",
                        "Refunds are processed within 5 business days.",
                        0,
                        10,
                        0.91
                );

        ResponseAssistantResponse aiResponse =
                new ResponseAssistantResponse(
                        "Your refund should be processed within 5 business days.",
                        "gemini-test",
                        List.of(
                                new ResponseAssistantResponse.ResponseAssistantCitation(
                                        documentId,
                                        "Refund Policy",
                                        chunkId,
                                        0
                                )
                        )
                );

        when(ticketRepository.findByIdAndOrganizationId(
                ticketId,
                organizationId
        )).thenReturn(Optional.of(ticket));

        when(ticketMessageRepository
                .findByTicketIdOrderByCreatedAtAscIdAsc(ticketId))
                .thenReturn(List.of(message));

        when(knowledgeBaseSearchService.search(
                eq(organizationId),
                any(String.class),
                eq(5)
        )).thenReturn(List.of(relevantChunk));

        when(aiServiceClient.generateResponseAssistant(
                any(ResponseAssistantRequest.class)
        )).thenReturn(aiResponse);

        ResponseAssistantResponse result =
                service.suggestResponse(ticketId);

        assertSame(aiResponse, result);

        ArgumentCaptor<ResponseAssistantRequest> requestCaptor =
                ArgumentCaptor.forClass(ResponseAssistantRequest.class);

        verify(aiServiceClient).generateResponseAssistant(
                requestCaptor.capture()
        );

        ResponseAssistantRequest request = requestCaptor.getValue();

        assertEquals("Refund not received", request.ticketSubject());
        assertEquals("HIGH", request.ticketPriority());
        assertEquals("Billing", request.ticketCategory());

        assertEquals(1, request.messages().size());
        assertEquals(
                "I requested a refund five days ago but have not received it.",
                request.messages().get(0).body()
        );
        assertFalse(request.messages().get(0).internalNote());

        assertEquals(1, request.chunks().size());
        assertEquals(
                "Refund Policy",
                request.chunks().get(0).documentTitle()
        );
        assertEquals(
                "Refunds are processed within 5 business days.",
                request.chunks().get(0).chunkText()
        );
        assertEquals(0.91, request.chunks().get(0).similarity());

        ArgumentCaptor<AiGeneration> generationCaptor =
                ArgumentCaptor.forClass(AiGeneration.class);

        verify(aiGenerationRepository).save(
                generationCaptor.capture()
        );

        AiGeneration generation = generationCaptor.getValue();

        assertEquals(
                AiGeneration.Kind.RESPONSE_SUGGESTION,
                generation.getKind()
        );
        assertEquals(
                "Your refund should be processed within 5 business days.",
                generation.getOutput()
        );
        assertEquals("gemini-test", generation.getModel());
    }

    @Test
    void suggestResponse_shouldExcludeChunksBelowSimilarityThreshold() {
        Ticket ticket = new Ticket(
                organizationId,
                customerId,
                "Refund not received",
                TicketPriority.HIGH,
                "Billing"
        );

        TicketMessage message = new TicketMessage(
                ticketId,
                customerId,
                "Where is my refund?",
                false
        );

        KbChunkSearchResult relevantChunk =
                new KbChunkSearchResult(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        organizationId,
                        "Refund Policy",
                        "Refunds are processed within 5 business days.",
                        0,
                        10,
                        0.90
                );

        KbChunkSearchResult irrelevantChunk =
                new KbChunkSearchResult(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        organizationId,
                        "Old Document",
                        "This content is unrelated.",
                        0,
                        5,
                        0.50
                );

        ResponseAssistantResponse aiResponse =
                new ResponseAssistantResponse(
                        "Your refund should be processed within 5 business days.",
                        "gemini-test",
                        List.of()
                );

        when(ticketRepository.findByIdAndOrganizationId(
                ticketId,
                organizationId
        )).thenReturn(Optional.of(ticket));

        when(ticketMessageRepository
                .findByTicketIdOrderByCreatedAtAscIdAsc(ticketId))
                .thenReturn(List.of(message));

        when(knowledgeBaseSearchService.search(
                eq(organizationId),
                any(String.class),
                eq(5)
        )).thenReturn(List.of(relevantChunk, irrelevantChunk));

        when(aiServiceClient.generateResponseAssistant(
                any(ResponseAssistantRequest.class)
        )).thenReturn(aiResponse);

        service.suggestResponse(ticketId);

        ArgumentCaptor<ResponseAssistantRequest> requestCaptor =
                ArgumentCaptor.forClass(ResponseAssistantRequest.class);

        verify(aiServiceClient).generateResponseAssistant(
                requestCaptor.capture()
        );

        ResponseAssistantRequest request = requestCaptor.getValue();

        assertEquals(1, request.chunks().size());
        assertEquals(
                relevantChunk.id().toString(),
                request.chunks().get(0).id()
        );
        assertEquals(
                0.90,
                request.chunks().get(0).similarity()
        );
    }


    @Test
    void suggestResponse_shouldFailWhenNoRelevantKbContentExists() {
        Ticket ticket = new Ticket(
                organizationId,
                customerId,
                "Refund not received",
                TicketPriority.HIGH,
                "Billing"
        );

        TicketMessage message = new TicketMessage(
                ticketId,
                customerId,
                "Where is my refund?",
                false
        );

        KbChunkSearchResult irrelevantChunk =
                new KbChunkSearchResult(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        organizationId,
                        "Old Document",
                        "This content is unrelated.",
                        0,
                        5,
                        0.50
                );

        when(ticketRepository.findByIdAndOrganizationId(
                ticketId,
                organizationId
        )).thenReturn(Optional.of(ticket));

        when(ticketMessageRepository
                .findByTicketIdOrderByCreatedAtAscIdAsc(ticketId))
                .thenReturn(List.of(message));

        when(knowledgeBaseSearchService.search(
                eq(organizationId),
                any(String.class),
                eq(5)
        )).thenReturn(List.of(irrelevantChunk));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.suggestResponse(ticketId)
                );

        assertEquals(
                "No relevant knowledge-base content found",
                exception.getMessage()
        );

        verify(aiServiceClient, never())
                .generateResponseAssistant(any(ResponseAssistantRequest.class));

        verify(aiGenerationRepository, never())
                .save(any(AiGeneration.class));
    }


    @Test
    void suggestResponse_shouldRejectCustomerAccess() {
        OrganizationContextHolder.set(
                new OrganizationContext(organizationId, MembershipRole.CUSTOMER)
        );

        ForbiddenException exception =
                assertThrows(
                        ForbiddenException.class,
                        () -> service.suggestResponse(ticketId)
                );

        assertEquals(
                "Customers cannot use the response assistant",
                exception.getMessage()
        );

        verifyNoInteractions(
                ticketRepository,
                ticketMessageRepository,
                knowledgeBaseSearchService,
                aiServiceClient,
                aiGenerationRepository
        );
    }


    @Test
    void suggestResponse_shouldFailWhenTicketHasNoMessages() {
        Ticket ticket = new Ticket(
                organizationId,
                customerId,
                "Refund not received",
                TicketPriority.HIGH,
                "Billing"
        );

        when(ticketRepository.findByIdAndOrganizationId(
                ticketId,
                organizationId
        )).thenReturn(Optional.of(ticket));

        when(ticketMessageRepository
                .findByTicketIdOrderByCreatedAtAscIdAsc(ticketId))
                .thenReturn(List.of());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.suggestResponse(ticketId)
                );

        assertEquals(
                "Cannot generate a response without ticket messages",
                exception.getMessage()
        );

        verifyNoInteractions(
                knowledgeBaseSearchService,
                aiServiceClient,
                aiGenerationRepository
        );
    }


    @Test
    void suggestResponse_shouldFailWhenTicketNotFoundInOrganization() {
        when(ticketRepository.findByIdAndOrganizationId(
                ticketId,
                organizationId
        )).thenReturn(Optional.empty());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.suggestResponse(ticketId)
                );

        assertEquals(
                "Ticket not found",
                exception.getMessage()
        );

        verifyNoInteractions(
                ticketMessageRepository,
                knowledgeBaseSearchService,
                aiServiceClient,
                aiGenerationRepository
        );
    }


    @Test
    void suggestResponse_shouldExcludeInternalMessagesFromSearchQuery() {
        Ticket ticket = new Ticket(
                organizationId,
                customerId,
                "Refund not received",
                TicketPriority.HIGH,
                "Billing"
        );

        TicketMessage publicMessage = new TicketMessage(
                ticketId,
                customerId,
                "I still have not received my refund.",
                false
        );

        TicketMessage internalMessage = new TicketMessage(
                ticketId,
                customerId,
                "Internal note: customer has been difficult.",
                true
        );

        KbChunkSearchResult relevantChunk =
                new KbChunkSearchResult(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        organizationId,
                        "Refund Policy",
                        "Refunds are processed within 5 business days.",
                        0,
                        10,
                        0.90
                );

        ResponseAssistantResponse aiResponse =
                new ResponseAssistantResponse(
                        "Your refund should be processed within 5 business days.",
                        "gemini-test",
                        List.of()
                );

        when(ticketRepository.findByIdAndOrganizationId(
                ticketId,
                organizationId
        )).thenReturn(Optional.of(ticket));

        when(ticketMessageRepository
                .findByTicketIdOrderByCreatedAtAscIdAsc(ticketId))
                .thenReturn(List.of(publicMessage, internalMessage));

        when(knowledgeBaseSearchService.search(
                eq(organizationId),
                any(String.class),
                eq(5)
        )).thenReturn(List.of(relevantChunk));

        when(aiServiceClient.generateResponseAssistant(
                any(ResponseAssistantRequest.class)
        )).thenReturn(aiResponse);

        service.suggestResponse(ticketId);

        ArgumentCaptor<String> queryCaptor =
                ArgumentCaptor.forClass(String.class);

        verify(knowledgeBaseSearchService).search(
                eq(organizationId),
                queryCaptor.capture(),
                eq(5)
        );

        String searchQuery = queryCaptor.getValue();

        assertEquals(
                "Refund not received Billing I still have not received my refund.",
                searchQuery
        );
    }


    @Test
    void suggestResponse_shouldUseLatestPublicMessageForSearchQuery() {
        Ticket ticket = new Ticket(
                organizationId,
                customerId,
                "Refund not received",
                TicketPriority.HIGH,
                "Billing"
        );

        TicketMessage olderPublicMessage = new TicketMessage(
                ticketId,
                customerId,
                "I requested a refund.",
                false
        );

        TicketMessage latestPublicMessage = new TicketMessage(
                ticketId,
                customerId,
                "The refund still has not arrived.",
                false
        );

        KbChunkSearchResult relevantChunk =
                new KbChunkSearchResult(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        organizationId,
                        "Refund Policy",
                        "Refunds are processed within 5 business days.",
                        0,
                        10,
                        0.90
                );

        ResponseAssistantResponse aiResponse =
                new ResponseAssistantResponse(
                        "Your refund should be processed within 5 business days.",
                        "gemini-test",
                        List.of()
                );

        when(ticketRepository.findByIdAndOrganizationId(
                ticketId,
                organizationId
        )).thenReturn(Optional.of(ticket));

        when(ticketMessageRepository
                .findByTicketIdOrderByCreatedAtAscIdAsc(ticketId))
                .thenReturn(List.of(
                        olderPublicMessage,
                        latestPublicMessage
                ));

        when(knowledgeBaseSearchService.search(
                eq(organizationId),
                any(String.class),
                eq(5)
        )).thenReturn(List.of(relevantChunk));

        when(aiServiceClient.generateResponseAssistant(
                any(ResponseAssistantRequest.class)
        )).thenReturn(aiResponse);

        service.suggestResponse(ticketId);

        ArgumentCaptor<String> queryCaptor =
                ArgumentCaptor.forClass(String.class);

        verify(knowledgeBaseSearchService).search(
                eq(organizationId),
                queryCaptor.capture(),
                eq(5)
        );

        String searchQuery = queryCaptor.getValue();

        assertEquals(
                "Refund not received Billing The refund still has not arrived.",
                searchQuery
        );
    }


    @Test
    void approveResponse_shouldApproveGenerationAndCreatePublicReply() {
        UUID currentUserId = UUID.randomUUID();

        User user = mock(User.class);
        when(user.getId()).thenReturn(currentUserId);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        user,
                        null
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        Ticket ticket = mock(Ticket.class);
        when(ticket.getId()).thenReturn(ticketId);

        UUID generationId = UUID.randomUUID();

        AiGeneration generation =
                new AiGeneration(
                        ticketId,
                        AiGeneration.Kind.RESPONSE_SUGGESTION,
                        "Your refund should arrive within 5 business days.",
                        "gemini-test",
                        120L
                );

        TicketMessageResponse messageResponse = mock(TicketMessageResponse.class);

        when(ticketRepository.findByIdAndOrganizationId(
                ticketId,
                organizationId
        )).thenReturn(Optional.of(ticket));

        when(aiGenerationRepository.findByIdAndTicketId(
                generationId,
                ticketId
        )).thenReturn(Optional.of(generation));

        when(ticketService.createMessage(
                eq(ticketId),
                any(CreateTicketMessageRequest.class),
                eq(true)
        )).thenReturn(messageResponse);

        TicketMessageResponse result =
                service.approveResponse(
                        ticketId,
                        generationId,
                        "Your refund should arrive within 5 business days."
                );

        assertSame(messageResponse, result);
        assertEquals(currentUserId, generation.getApprovedBy());

        verify(aiGenerationRepository).save(generation);

        ArgumentCaptor<CreateTicketMessageRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateTicketMessageRequest.class);

        verify(ticketService).createMessage(
                eq(ticketId),
                requestCaptor.capture(),
                eq(true)
        );

        CreateTicketMessageRequest request =
                requestCaptor.getValue();

        assertEquals(
                "Your refund should arrive within 5 business days.",
                request.body()
        );
        assertFalse(request.internalNote());
    }


    @Test
    void approveResponse_shouldRejectNonResponseSuggestionGeneration() {
        User user = mock(User.class);

        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                user,
                                null
                        )
                );

        Ticket ticket = new Ticket(
                organizationId,
                customerId,
                "Refund not received",
                TicketPriority.HIGH,
                "Billing"
        );

        UUID generationId = UUID.randomUUID();

        AiGeneration generation =
                new AiGeneration(
                        ticketId,
                        AiGeneration.Kind.SUMMARY,
                        "Summary",
                        "gemini-test",
                        100L
                );

        when(ticketRepository.findByIdAndOrganizationId(
                ticketId,
                organizationId
        )).thenReturn(Optional.of(ticket));

        when(aiGenerationRepository.findByIdAndTicketId(
                generationId,
                ticketId
        )).thenReturn(Optional.of(generation));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.approveResponse(
                                ticketId,
                                generationId,
                                "Approved reply"
                        )
                );

        assertEquals(
                "AI generation is not a response suggestion",
                exception.getMessage()
        );

        verify(aiGenerationRepository, never())
                .save(any(AiGeneration.class));

        verifyNoInteractions(ticketService);
    }


    @Test
    void approveResponse_shouldRejectAlreadyApprovedGeneration() {
        UUID originalApproverId = UUID.randomUUID();

        User user = mock(User.class);

        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                user,
                                null
                        )
                );

        Ticket ticket = new Ticket(
                organizationId,
                customerId,
                "Refund not received",
                TicketPriority.HIGH,
                "Billing"
        );

        UUID generationId = UUID.randomUUID();

        AiGeneration generation =
                new AiGeneration(
                        ticketId,
                        AiGeneration.Kind.RESPONSE_SUGGESTION,
                        "Suggested reply",
                        "gemini-test",
                        100L
                );

        generation.approve(originalApproverId);

        when(ticketRepository.findByIdAndOrganizationId(
                ticketId,
                organizationId
        )).thenReturn(Optional.of(ticket));

        when(aiGenerationRepository.findByIdAndTicketId(
                generationId,
                ticketId
        )).thenReturn(Optional.of(generation));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.approveResponse(
                                ticketId,
                                generationId,
                                "Another reply"
                        )
                );

        assertEquals(
                "AI generation has already been approved",
                exception.getMessage()
        );

        assertEquals(
                originalApproverId,
                generation.getApprovedBy()
        );

        verify(aiGenerationRepository, never())
                .save(any(AiGeneration.class));

        verifyNoInteractions(ticketService);
    }


    @Test
    void approveResponse_shouldRejectCustomerAccess() {
        OrganizationContextHolder.set(
                new OrganizationContext(
                        organizationId,
                        MembershipRole.CUSTOMER
                )
        );

        ForbiddenException exception =
                assertThrows(
                        ForbiddenException.class,
                        () -> service.approveResponse(
                                ticketId,
                                UUID.randomUUID(),
                                "Approved reply"
                        )
                );

        assertEquals(
                "Customers cannot use the response assistant",
                exception.getMessage()
        );

        verifyNoInteractions(
                ticketRepository,
                aiGenerationRepository,
                ticketService
        );
    }



    @Test
    void approveResponse_shouldFailWhenTicketNotFound() {
        UUID generationId = UUID.randomUUID();

        when(ticketRepository.findByIdAndOrganizationId(
                ticketId,
                organizationId
        )).thenReturn(Optional.empty());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.approveResponse(
                                ticketId,
                                generationId,
                                "Approved reply"
                        )
                );

        assertEquals(
                "Ticket not found",
                exception.getMessage()
        );

        verifyNoInteractions(
                aiGenerationRepository,
                ticketService
        );
    }


    @Test
    void approveResponse_shouldFailWhenGenerationNotFound() {
        Ticket ticket = mock(Ticket.class);
        UUID generationId = UUID.randomUUID();

        when(ticketRepository.findByIdAndOrganizationId(
                ticketId,
                organizationId
        )).thenReturn(Optional.of(ticket));

        when(aiGenerationRepository.findByIdAndTicketId(
                generationId,
                ticketId
        )).thenReturn(Optional.empty());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.approveResponse(
                                ticketId,
                                generationId,
                                "Approved reply"
                        )
                );

        assertEquals(
                "AI generation not found",
                exception.getMessage()
        );

        verify(aiGenerationRepository, never())
                .save(any(AiGeneration.class));

        verifyNoInteractions(ticketService);
    }


    @Test
    void streamResponse_shouldStreamChunksAndPersistGeneration() {
        Ticket ticket = mock(Ticket.class);

        when(ticket.getSubject())
                .thenReturn("Refund not received");

        when(ticket.getPriority())
                .thenReturn(TicketPriority.HIGH);

        when(ticket.getCategory())
                .thenReturn("Billing");

        TicketMessage message = mock(TicketMessage.class);

        when(message.getBody())
                .thenReturn("The refund still has not arrived.");

        when(message.isInternalNote())
                .thenReturn(false);

        UUID chunkId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        KbChunkSearchResult chunk =
                new KbChunkSearchResult(
                        chunkId,
                        documentId,
                        organizationId,
                        "Refund Policy",
                        "Refunds are processed within 5 business days.",
                        0,
                        20,
                        0.90
                );

        when(ticketRepository.findByIdAndOrganizationId(
                ticketId,
                organizationId
        )).thenReturn(Optional.of(ticket));

        when(ticketMessageRepository
                .findByTicketIdOrderByCreatedAtAscIdAsc(ticketId))
                .thenReturn(List.of(message));

        when(knowledgeBaseSearchService.search(
                eq(organizationId),
                anyString(),
                eq(5)
        )).thenReturn(List.of(chunk));

        doAnswer(invocation -> {
            Consumer<String> consumer = invocation.getArgument(1);

            consumer.accept("Your refund ");
            consumer.accept("is being processed.");

            return "gemini-test";
        }).when(aiServiceClient).streamResponseAssistant(
                any(ResponseAssistantRequest.class),
                any()
        );

        SseEmitter emitter = service.streamResponse(ticketId);

        assertNotNull(emitter);

        verify(aiServiceClient, timeout(2000))
                .streamResponseAssistant(
                        any(ResponseAssistantRequest.class),
                        any()
                );

        verify(aiGenerationRepository, timeout(2000))
                .save(argThat(generation ->
                        generation.getTicketId().equals(ticketId)
                                && generation.getKind()
                                == AiGeneration.Kind.RESPONSE_SUGGESTION
                                && generation.getOutput().equals(
                                        "Your refund is being processed."
                                )
                                && generation.getModel().equals(
                                        "gemini-test"
                                )
                ));
    }


    @Test
    void streamResponse_shouldNotPersistGenerationWhenAiStreamingFails() {
        Ticket ticket = mock(Ticket.class);

        when(ticket.getSubject())
                .thenReturn("Refund not received");

        when(ticket.getPriority())
                .thenReturn(TicketPriority.HIGH);

        when(ticket.getCategory())
                .thenReturn("Billing");

        TicketMessage message = mock(TicketMessage.class);

        when(message.getBody())
                .thenReturn("The refund still has not arrived.");

        when(message.isInternalNote())
                .thenReturn(false);

        UUID chunkId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        KbChunkSearchResult chunk =
                new KbChunkSearchResult(
                        chunkId,
                        documentId,
                        organizationId,
                        "Refund Policy",
                        "Refunds are processed within 5 business days.",
                        0,
                        20,
                        0.90
                );

        when(ticketRepository.findByIdAndOrganizationId(
                ticketId,
                organizationId
        )).thenReturn(Optional.of(ticket));

        when(ticketMessageRepository
                .findByTicketIdOrderByCreatedAtAscIdAsc(ticketId))
                .thenReturn(List.of(message));

        when(knowledgeBaseSearchService.search(
                eq(organizationId),
                anyString(),
                eq(5)
        )).thenReturn(List.of(chunk));

        doAnswer(invocation -> {
            Consumer<String> consumer = invocation.getArgument(1);

            consumer.accept("Starting response...");

            throw new RuntimeException("AI service unavailable");
        }).when(aiServiceClient).streamResponseAssistant(
                any(ResponseAssistantRequest.class),
                any()
        );

        SseEmitter emitter = service.streamResponse(ticketId);

        assertNotNull(emitter);

        verify(aiServiceClient, timeout(2000))
                .streamResponseAssistant(
                        any(ResponseAssistantRequest.class),
                        any()
                );

        verify(aiGenerationRepository, after(500).never())
                .save(any(AiGeneration.class));
    }

}
