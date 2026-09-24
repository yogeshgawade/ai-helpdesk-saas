package com.helpdesk.kb.repository;

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
    private TenantTransactionExecutor tenantTransactionExecutor;

    @Test
    void shouldInsertAndSearchVector() {
        UUID organizationId =
                UUID.fromString("8a568ac9-b1b2-41cc-a19a-86de79f83d4e");

        UUID documentId =
                UUID.fromString("08a4388b-e990-4425-82e7-9f2c4384a71a");

        UUID chunkId = UUID.randomUUID();

        float[] embedding = new float[384];
        embedding[0] = 1.0f;

        tenantTransactionExecutor.execute(
                organizationId,
                () -> {
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
                }
        );
    }
}