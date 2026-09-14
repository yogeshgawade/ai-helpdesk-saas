package com.helpdesk.kb.controller;

import com.helpdesk.kb.dto.KnowledgeBaseDocumentResponse;
import com.helpdesk.kb.service.KnowledgeBaseDocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orgs/{organizationId}/kb/documents")
public class KnowledgeBaseDocumentController {

    private final KnowledgeBaseDocumentService documentService;

    public KnowledgeBaseDocumentController(
            KnowledgeBaseDocumentService documentService
    ) {
        this.documentService = documentService;
    }

    @GetMapping
    public List<KnowledgeBaseDocumentResponse> getDocuments(
            @PathVariable UUID organizationId
    ) {
        return documentService.getDocuments();
    }

    @GetMapping("/{documentId}")
    public KnowledgeBaseDocumentResponse getDocument(
            @PathVariable UUID organizationId,
            @PathVariable UUID documentId
    ) {
        return documentService.getDocument(documentId);
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<KnowledgeBaseDocumentResponse> createDocument(
            @PathVariable UUID organizationId,
            @RequestParam String title,
            @RequestParam String sourceType,
            @RequestPart MultipartFile file
    ) {
        KnowledgeBaseDocumentResponse document =
                documentService.createDocument(
                        title,
                        sourceType,
                        file
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(document);
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable UUID organizationId,
            @PathVariable UUID documentId
    ) {
        documentService.deleteDocument(documentId);

        return ResponseEntity.noContent().build();
    }
}
