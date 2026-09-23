package com.helpdesk.tickets.repository;

import com.helpdesk.tickets.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID>, JpaSpecificationExecutor<Ticket> {

    Optional<Ticket> findByIdAndOrganizationId(
            UUID ticketId,
            UUID organizationId
    );

    List<Ticket> findByOrganizationId(
            UUID organizationId
    );

    List<Ticket> findByOrganizationIdAndCustomerId(
            UUID organizationId,
            UUID customerId
    );


    @Query("""
            SELECT t
            FROM Ticket t
            WHERE t.firstResponseDueAt IS NOT NULL
              AND t.firstResponseDueAt <= :now
              AND t.slaFirstResponseBreached = FALSE
              AND (
                    t.firstRespondedAt IS NULL
                    OR t.firstRespondedAt > t.firstResponseDueAt
              )
            """)
    List<Ticket> findTicketsWithBreachedFirstResponseSla(
            @Param("now") Instant now
    );

    @Query("""
            SELECT t
            FROM Ticket t
            WHERE t.resolutionDueAt IS NOT NULL
              AND t.resolutionDueAt <= :now
              AND t.slaResolutionBreached = FALSE
            """)
    List<Ticket> findTicketsWithBreachedResolutionSla(
            @Param("now") Instant now
    );

    @Modifying
    @Transactional
    @Query("""
            UPDATE Ticket t
            SET t.aiCategory = :category,
                t.aiPriority = :priority,
                t.aiConfidence = :confidence,
                t.aiReason = :reason,
                t.aiClassifiedAt = :classifiedAt
            WHERE t.id = :ticketId
              AND t.organizationId = :organizationId
            """)
    int updateAiClassification(
            @Param("ticketId") UUID ticketId,
            @Param("organizationId") UUID organizationId,
            @Param("category") String category,
            @Param("priority") String priority,
            @Param("confidence") Double confidence,
            @Param("reason") String reason,
            @Param("classifiedAt") Instant classifiedAt
    );

    @Modifying
    @Transactional
    @Query("""
            UPDATE Ticket t
            SET t.aiSummary = :summary,
                t.aiSummarizedAt = :summarizedAt
            WHERE t.id = :ticketId
              AND t.organizationId = :organizationId
            """)
    int updateAiSummary(
            @Param("ticketId") UUID ticketId,
            @Param("organizationId") UUID organizationId,
            @Param("summary") String summary,
            @Param("summarizedAt") Instant summarizedAt
    );
}
