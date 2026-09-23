package com.helpdesk.kb.service;

import com.helpdesk.ai.client.AiServiceClient;
import com.helpdesk.ai.client.dto.DocumentChunkResponse;
import com.helpdesk.ai.client.dto.DocumentProcessResponse;
import com.helpdesk.ai.client.dto.EmbeddingResponse;
import com.helpdesk.kb.entity.DocumentStatus;
import com.helpdesk.kb.entity.KnowledgeBaseDocument;
import com.helpdesk.kb.repository.KbChunkVectorRepository;
import com.helpdesk.kb.repository.KnowledgeBaseDocumentRepository;
import com.helpdesk.storage.DocumentStorage;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.UUID;

@Service
public class KnowledgeBaseDocumentProcessor {

    private final KnowledgeBaseDocumentRepository documentRepository;
    private final KbChunkVectorRepository chunkVectorRepository;
    private final DocumentStorage documentStorage;
    private final AiServiceClient aiServiceClient;

    public KnowledgeBaseDocumentProcessor(
            KnowledgeBaseDocumentRepository documentRepository,
            KbChunkVectorRepository chunkVectorRepository,
            DocumentStorage documentStorage,
            AiServiceClient aiServiceClient
    ) {
        this.documentRepository = documentRepository;
        this.chunkVectorRepository = chunkVectorRepository;
        this.documentStorage = documentStorage;
        this.aiServiceClient = aiServiceClient;
    }

    public void process(
            UUID organizationId,
            UUID documentId,
            String filename
    ) {
        KnowledgeBaseDocument document = documentRepository
                .findByIdAndOrganizationId(documentId, organizationId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Document not found"));

        try {
            document.setStatus(DocumentStatus.PROCESSING);
            documentRepository.save(document);

            DocumentProcessResponse processResponse;

            try (InputStream inputStream = documentStorage.open(
                    organizationId,
                    documentId,
                    filename
            )) {
                processResponse = aiServiceClient.processDocument(
                        inputStream,
                        filename
                );
            }

            for (DocumentChunkResponse chunk : processResponse.chunks()) {

                EmbeddingResponse embeddingResponse =
                        aiServiceClient.createEmbedding(chunk.text());

                float[] embedding = new float[
                        embeddingResponse.embedding().size()
                ];

                for (int i = 0; i < embedding.length; i++) {
                    embedding[i] = embeddingResponse.embedding().get(i);
                }

                chunkVectorRepository.insertChunk(
                        UUID.randomUUID(),
                        documentId,
                        organizationId,
                        chunk.text(),
                        embedding,
                        chunk.chunkIndex(),
                        chunk.tokenCount()
                );
            }

            document.setStatus(DocumentStatus.READY);
            documentRepository.save(document);

        } catch (Exception exception) {

            document.setStatus(DocumentStatus.FAILED);
            documentRepository.save(document);

            throw new IllegalStateException(
                    "Failed to process knowledge base document",
                    exception
            );
        }
    }
}
