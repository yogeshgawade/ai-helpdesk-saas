package com.helpdesk.ai.client;

import com.helpdesk.ai.client.dto.DocumentProcessResponse;
import com.helpdesk.ai.client.dto.EmbeddingResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
public class AiServiceTestController {

    private final AiServiceClient aiServiceClient;

    public AiServiceTestController(AiServiceClient aiServiceClient) {
        this.aiServiceClient = aiServiceClient;
    }

    @GetMapping("/internal/test-ai-embedding")
    public EmbeddingResponse testEmbedding(
            @RequestParam String text
    ) {
        return aiServiceClient.createEmbedding(text);
    }

    @GetMapping("/internal/test-ai-document")
    public DocumentProcessResponse testDocument(
            @RequestParam String path
    ) {
        Path filePath = Path.of(path);

        try (InputStream inputStream = Files.newInputStream(filePath)) {
            return aiServiceClient.processDocument(
                    inputStream,
                    filePath.getFileName().toString()
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not read test document",
                    exception
            );
        }
    }
}
