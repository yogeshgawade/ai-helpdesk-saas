package com.helpdesk.kb.service;

import com.helpdesk.ai.client.AiServiceClient;
import com.helpdesk.ai.client.dto.EmbeddingResponse;
import com.helpdesk.kb.repository.KbChunkSearchResult;
import com.helpdesk.kb.repository.KbChunkVectorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class KnowledgeBaseSearchService {

    private final AiServiceClient aiServiceClient;
    private final KbChunkVectorRepository chunkVectorRepository;

    public KnowledgeBaseSearchService(
            AiServiceClient aiServiceClient,
            KbChunkVectorRepository chunkVectorRepository
    ) {
        this.aiServiceClient = aiServiceClient;
        this.chunkVectorRepository = chunkVectorRepository;
    }

    @Transactional
    public List<KbChunkSearchResult> search(
            UUID organizationId,
            String query,
            int limit
    ) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query cannot be empty");
        }

        if (limit < 1 || limit > 50) {
            throw new IllegalArgumentException(
                    "Search limit must be between 1 and 50"
            );
        }

        EmbeddingResponse embeddingResponse =
                aiServiceClient.createEmbedding(query);

        float[] embedding = new float[
                embeddingResponse.embedding().size()
        ];

        for (int i = 0; i < embedding.length; i++) {
            embedding[i] = embeddingResponse.embedding().get(i);
        }

        return chunkVectorRepository.searchSimilar(
                organizationId,
                embedding,
                limit
        );
    }
}
