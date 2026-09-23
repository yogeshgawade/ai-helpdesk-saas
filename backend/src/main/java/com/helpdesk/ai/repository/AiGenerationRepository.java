package com.helpdesk.ai.repository;

import com.helpdesk.ai.entity.AiGeneration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiGenerationRepository
        extends JpaRepository<AiGeneration, UUID> {

    List<AiGeneration> findByTicketIdOrderByCreatedAtDesc(UUID ticketId);

    Optional<AiGeneration> findByIdAndTicketId(
            UUID id,
            UUID ticketId
    );
}
