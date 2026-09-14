package com.helpdesk.kb.repository;

import com.helpdesk.kb.entity.KnowledgeBaseDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KnowledgeBaseDocumentRepository
        extends JpaRepository<KnowledgeBaseDocument, UUID> {

    List<KnowledgeBaseDocument> findAllByOrganizationIdOrderByCreatedAtDesc(
            UUID organizationId
    );

    Optional<KnowledgeBaseDocument> findByIdAndOrganizationId(
            UUID id,
            UUID organizationId
    );
}
