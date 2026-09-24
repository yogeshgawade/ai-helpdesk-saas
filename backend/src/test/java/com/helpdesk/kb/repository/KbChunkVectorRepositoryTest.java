package com.helpdesk.kb.repository;

import com.helpdesk.kb.entity.KnowledgeBaseDocument;
import com.helpdesk.orgs.Organization;
import com.helpdesk.orgs.OrganizationRepository;
import com.helpdesk.orgs.TenantTransactionExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class KbChunkVectorRepositoryTest {

    @Autowired
    private KbChunkVectorRepository vectorRepository;

    @Autowired
    private KbChunkRepository chunkRepository;

    @Autowired
    private KnowledgeBaseDocumentRepository documentRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private TenantTransactionExecutor tenantTransactionExecutor;

    @Test
    void shouldInsertAndSearchVector() {
        Organization organization = organizationRepository.saveAndFlush(
                new Organization(
                        "pgvector integration test",
                        "pgvector-integration-" + UUID.randomUUID()
                )
        );

        UUID organizationId = organization.getId();

        float[] embedding = new float[384];
        embedding[0] = 1.0f;

        try {
            tenantTransactionExecutor.execute(
                    organizationId,
                    () -> {
                        KnowledgeBaseDocument document =
                                new KnowledgeBaseDocument();

                        document.setOrganizationId(organizationId);
                        document.setTitle("pgvector integration test");
                        document.setSourceType("TEST");

                        document = documentRepository.saveAndFlush(document);

                        UUID documentId = document.getId();
                        UUID chunkId = UUID.randomUUID();

                        vectorRepository.insertChunk(
                                chunkId,
                                documentId,
                                organizationId,
                                "Java pgvector integration test.",
                                embedding,
                                100,
                                6
                        );

                        var results = vectorRepository.searchSimilar(
                                organizationId,
                                embedding,
                                5
                        );

                        assertTrue(
                                results.stream()
                                        .anyMatch(result -> result.id().equals(chunkId))
                        );

                        var result = results.stream()
                                .filter(r -> r.id().equals(chunkId))
                                .findFirst()
                                .orElseThrow();

                        assertEquals(
                                "Java pgvector integration test.",
                                result.chunkText()
                        );

                        assertEquals(1.0, result.similarity(), 0.000001);

                        chunkRepository.deleteById(chunkId);
                        documentRepository.deleteById(documentId);
                    }
            );
        } finally {
            organizationRepository.deleteById(organizationId);
        }
    }
}
