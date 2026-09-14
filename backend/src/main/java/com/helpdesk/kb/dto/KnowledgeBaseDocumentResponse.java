package com.helpdesk.kb.dto;

import com.helpdesk.kb.entity.KnowledgeBaseDocument;

import java.time.OffsetDateTime;
import java.util.UUID;

public record KnowledgeBaseDocumentResponse(
        UUID id,
        UUID organizationId,
        String title,
        String sourceType,
        String s3Key,
        String contentHash,
        String status,
        OffsetDateTime createdAt
) {

    public static KnowledgeBaseDocumentResponse from(
            KnowledgeBaseDocument document
    ) {
        return new KnowledgeBaseDocumentResponse(
                document.getId(),
                document.getOrganizationId(),
                document.getTitle(),
                document.getSourceType(),
                document.getS3Key(),
                document.getContentHash(),
                document.getStatus().name(),
                document.getCreatedAt()
        );
    }
}
