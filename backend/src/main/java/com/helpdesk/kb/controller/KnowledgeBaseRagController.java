package com.helpdesk.kb.controller;

import com.helpdesk.ai.client.dto.RagResponse;
import com.helpdesk.kb.service.KnowledgeBaseRagService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/orgs/{organizationId}/kb/rag")
public class KnowledgeBaseRagController {

    private final KnowledgeBaseRagService ragService;

    public KnowledgeBaseRagController(
            KnowledgeBaseRagService ragService
    ) {
        this.ragService = ragService;
    }

    @PostMapping
    public RagResponse generate(
            @PathVariable UUID organizationId,
            @RequestBody RagQueryRequest request
    ) {
        return ragService.generate(
                organizationId,
                request.query(),
                request.limit() == null ? 5 : request.limit()
        );
    }

    public record RagQueryRequest(
            String query,
            Integer limit
    ) {
    }
}
