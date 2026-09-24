package com.helpdesk.kb.service;

import com.helpdesk.auth.MembershipRole;
import com.helpdesk.exception.ForbiddenException;
import com.helpdesk.kb.dto.KnowledgeBaseDocumentResponse;
import com.helpdesk.kb.entity.KnowledgeBaseDocument;
import com.helpdesk.kb.repository.KnowledgeBaseDocumentRepository;
import com.helpdesk.orgs.OrganizationContext;
import com.helpdesk.orgs.OrganizationContextHolder;
import com.helpdesk.storage.DocumentStorage;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class KnowledgeBaseDocumentService {

    private final KnowledgeBaseDocumentRepository documentRepository;
    private final DocumentStorage documentStorage;
    private final KnowledgeBaseDocumentProcessor documentProcessor;

    public KnowledgeBaseDocumentService(
            KnowledgeBaseDocumentRepository documentRepository,
            DocumentStorage documentStorage,
            KnowledgeBaseDocumentProcessor documentProcessor
    ) {
        this.documentRepository = documentRepository;
        this.documentStorage = documentStorage;
        this.documentProcessor = documentProcessor;
    }

    @Transactional
    public List<KnowledgeBaseDocumentResponse> getDocuments() {
        UUID organizationId = getCurrentOrganizationId();

        if (getCurrentRole() == MembershipRole.CUSTOMER) {
            throw new ForbiddenException(
                    "Customers cannot access the knowledge base"
            );
        }

        return documentRepository
                .findAllByOrganizationIdOrderByCreatedAtDesc(
                        organizationId
                )
                .stream()
                .map(KnowledgeBaseDocumentResponse::from)
                .toList();
    }

    @Transactional
    public KnowledgeBaseDocumentResponse getDocument(UUID documentId) {
        UUID organizationId = getCurrentOrganizationId();

        if (getCurrentRole() == MembershipRole.CUSTOMER) {
            throw new ForbiddenException(
                    "Customers cannot access the knowledge base"
            );
        }

        KnowledgeBaseDocument document = documentRepository
                .findByIdAndOrganizationId(documentId, organizationId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Document not found")
                );

        return KnowledgeBaseDocumentResponse.from(document);
    }

    @Transactional
    public KnowledgeBaseDocumentResponse createDocument(
            String title,
            String sourceType,
            MultipartFile file
    ) {
        UUID organizationId = getCurrentOrganizationId();
        MembershipRole role = getCurrentRole();

        if (role == MembershipRole.CUSTOMER
                || role == MembershipRole.AGENT) {
            throw new ForbiddenException(
                    "Only owners and admins can create knowledge base documents"
            );
        }

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Document file cannot be empty");
        }

        final long maxFileSizeBytes = 20L * 1024 * 1024;

        if (file.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException(
                    "Document file exceeds the maximum allowed size of 20 MB"
            );
        }

        String filename = file.getOriginalFilename();

        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException(
                    "Document filename is required"
            );
        }

        KnowledgeBaseDocument document = new KnowledgeBaseDocument();

        document.setOrganizationId(organizationId);
        document.setTitle(title);
        document.setSourceType(sourceType);

        KnowledgeBaseDocument savedDocument =
                documentRepository.saveAndFlush(document);

        try {
            documentStorage.store(
                    organizationId,
                    savedDocument.getId(),
                    filename,
                    file.getInputStream()
            );
        } catch (IOException e) {
            documentRepository.delete(savedDocument);

            throw new IllegalStateException(
                    "Failed to store knowledge base document",
                    e
            );
        }

        documentProcessor.process(
                organizationId,
                savedDocument.getId(),
                filename
        );

        return KnowledgeBaseDocumentResponse.from(savedDocument);
    }

    @Transactional
    public void deleteDocument(UUID documentId) {
        UUID organizationId = getCurrentOrganizationId();
        MembershipRole role = getCurrentRole();

        if (role == MembershipRole.CUSTOMER
                || role == MembershipRole.AGENT) {
            throw new ForbiddenException(
                    "Only owners and admins can delete knowledge base documents"
            );
        }

        KnowledgeBaseDocument document = documentRepository
                .findByIdAndOrganizationId(documentId, organizationId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Document not found")
                );

        documentRepository.delete(document);
    }

    private UUID getCurrentOrganizationId() {
        OrganizationContext context = OrganizationContextHolder.get();

        if (context == null) {
            throw new IllegalStateException(
                    "Organization context is not available"
            );
        }

        return context.getOrganizationId();
    }

    private MembershipRole getCurrentRole() {
        OrganizationContext context = OrganizationContextHolder.get();

        if (context == null) {
            throw new IllegalStateException(
                    "Organization context is not available"
            );
        }

        return context.getRole();
    }
}
