package com.helpdesk.kb.repository;

import com.helpdesk.kb.entity.KbChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface KbChunkRepository extends JpaRepository<KbChunk, UUID> {

    List<KbChunk> findAllByDocumentIdAndOrganizationIdOrderByChunkIndexAsc(
            UUID documentId,
            UUID organizationId
    );

    void deleteAllByDocumentIdAndOrganizationId(
            UUID documentId,
            UUID organizationId
    );
}
