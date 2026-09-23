package com.helpdesk.kb.controller;

import com.helpdesk.kb.repository.KbChunkSearchResult;
import com.helpdesk.kb.service.KnowledgeBaseSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orgs/{organizationId}/kb/search")
public class KnowledgeBaseSearchController {

    private final KnowledgeBaseSearchService searchService;

    public KnowledgeBaseSearchController(
            KnowledgeBaseSearchService searchService
    ) {
        this.searchService = searchService;
    }

    @GetMapping
    public List<KbChunkSearchResult> search(
            @PathVariable UUID organizationId,
            @RequestParam String q,
            @RequestParam(defaultValue = "5") int limit
    ) {
        return searchService.search(
                organizationId,
                q,
                limit
        );
    }
}
