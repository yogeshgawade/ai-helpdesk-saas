package com.helpdesk.kb.service;

import com.helpdesk.ai.client.AiServiceClient;
import com.helpdesk.ai.client.dto.RagContextChunk;
import com.helpdesk.ai.client.dto.RagRequest;
import com.helpdesk.ai.client.dto.RagResponse;
import com.helpdesk.kb.repository.KbChunkSearchResult;
import com.helpdesk.orgs.OrganizationContext;
import com.helpdesk.orgs.OrganizationContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class KnowledgeBaseRagService {

    private final KnowledgeBaseSearchService searchService;
    private final AiServiceClient aiServiceClient;
    private final double similarityThreshold;

    public KnowledgeBaseRagService(
            KnowledgeBaseSearchService searchService,
            AiServiceClient aiServiceClient,
            @Value("${app.kb.rag.similarity-threshold}") double similarityThreshold
    ) {
        this.searchService = searchService;
        this.aiServiceClient = aiServiceClient;
        this.similarityThreshold = similarityThreshold;
    }

    public RagResponse generate(
            UUID organizationId,
            String query,
            int limit
    ) {
        UUID currentOrganizationId = getCurrentOrganizationId();

        if (!currentOrganizationId.equals(organizationId)) {
            throw new IllegalStateException(
                    "Organization context does not match requested organization"
            );
        }

        List<KbChunkSearchResult> searchResults =
                searchService.search(
                        currentOrganizationId,
                        query,
                        limit
                );

        List<KbChunkSearchResult> relevantResults =
                searchResults.stream()
                        .filter(result -> result.similarity() >= similarityThreshold)
                        .toList();

        if (relevantResults.isEmpty()) {
            throw new IllegalArgumentException(
                    "No relevant knowledge-base content found"
            );
        }

        List<RagContextChunk> chunks = relevantResults.stream()
                .map(this::toRagContextChunk)
                .toList();

        RagRequest request = new RagRequest(
                query,
                chunks
        );

        return aiServiceClient.generateRagResponse(request);
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

    private RagContextChunk toRagContextChunk(
            KbChunkSearchResult result
    ) {
        return new RagContextChunk(
                result.id().toString(),
                result.documentId(),
                result.documentTitle(),
                result.chunkText(),
                result.chunkIndex(),
                result.similarity()
        );
    }
}
