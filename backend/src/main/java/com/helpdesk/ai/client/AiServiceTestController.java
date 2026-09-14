package com.helpdesk.ai.client;

import com.helpdesk.ai.client.dto.EmbeddingResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
