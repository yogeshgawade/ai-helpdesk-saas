package com.helpdesk.analytics.repository;

import com.helpdesk.tickets.entity.Ticket;
import com.helpdesk.tickets.entity.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AnalyticsRepository
        extends JpaRepository<Ticket, UUID> {

    @Query("""
            SELECT COUNT(t)
            FROM Ticket t
            WHERE t.organizationId = :organizationId
              AND t.createdAt >= :from
              AND t.createdAt < :to
            """)
    long countTickets(
            @Param("organizationId") UUID organizationId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
            SELECT COUNT(t)
            FROM Ticket t
            WHERE t.organizationId = :organizationId
              AND t.status = :status
              AND t.createdAt >= :from
              AND t.createdAt < :to
            """)
    long countTicketsByStatus(
            @Param("organizationId") UUID organizationId,
            @Param("status") TicketStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
            SELECT t.priority, COUNT(t)
            FROM Ticket t
            WHERE t.organizationId = :organizationId
              AND t.createdAt >= :from
              AND t.createdAt < :to
            GROUP BY t.priority
            ORDER BY t.priority
            """)
    List<Object[]> countTicketsByPriority(
            @Param("organizationId") UUID organizationId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
            SELECT t.status, COUNT(t)
            FROM Ticket t
            WHERE t.organizationId = :organizationId
              AND t.createdAt >= :from
              AND t.createdAt < :to
            GROUP BY t.status
            ORDER BY t.status
            """)
    List<Object[]> countTicketsByStatus(
            @Param("organizationId") UUID organizationId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
            SELECT COALESCE(t.category, 'Uncategorized'), COUNT(t)
            FROM Ticket t
            WHERE t.organizationId = :organizationId
              AND t.createdAt >= :from
              AND t.createdAt < :to
            GROUP BY t.category
            ORDER BY COUNT(t) DESC
            """)
    List<Object[]> countTicketsByCategory(
            @Param("organizationId") UUID organizationId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query(value = """
            SELECT AVG(
                EXTRACT(
                    EPOCH FROM (first_responded_at - created_at)
                )
            ) / 60.0
            FROM tickets
            WHERE organization_id = :organizationId
              AND created_at >= :from
              AND created_at < :to
              AND first_responded_at IS NOT NULL
            """,
            nativeQuery = true)
    Double averageFirstResponseMinutes(
            @Param("organizationId") UUID organizationId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query(value = """
            SELECT AVG(
                EXTRACT(
                    EPOCH FROM (resolved_at - created_at)
                )
            ) / 60.0
            FROM tickets
            WHERE organization_id = :organizationId
              AND created_at >= :from
              AND created_at < :to
              AND resolved_at IS NOT NULL
            """,
            nativeQuery = true)
    Double averageResolutionMinutes(
            @Param("organizationId") UUID organizationId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
            SELECT COUNT(t)
            FROM Ticket t
            WHERE t.organizationId = :organizationId
              AND t.createdAt >= :from
              AND t.createdAt < :to
              AND t.firstResponseDueAt IS NOT NULL
            """)
    long countTicketsWithFirstResponseSla(
            @Param("organizationId") UUID organizationId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
            SELECT COUNT(t)
            FROM Ticket t
            WHERE t.organizationId = :organizationId
              AND t.createdAt >= :from
              AND t.createdAt < :to
              AND t.firstResponseDueAt IS NOT NULL
              AND t.slaFirstResponseBreached = TRUE
            """)
    long countFirstResponseSlaBreaches(
            @Param("organizationId") UUID organizationId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
            SELECT COUNT(t)
            FROM Ticket t
            WHERE t.organizationId = :organizationId
              AND t.createdAt >= :from
              AND t.createdAt < :to
              AND t.resolutionDueAt IS NOT NULL
            """)
    long countTicketsWithResolutionSla(
            @Param("organizationId") UUID organizationId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
            SELECT COUNT(t)
            FROM Ticket t
            WHERE t.organizationId = :organizationId
              AND t.createdAt >= :from
              AND t.createdAt < :to
              AND t.resolutionDueAt IS NOT NULL
              AND t.slaResolutionBreached = TRUE
            """)
    long countResolutionSlaBreaches(
            @Param("organizationId") UUID organizationId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
            SELECT t.assignedAgentId,
                   SUM(
                       CASE
                           WHEN t.status <> 'RESOLVED'
                           THEN 1
                           ELSE 0
                       END
                   ),
                   SUM(
                       CASE
                           WHEN t.status = 'RESOLVED'
                           THEN 1
                           ELSE 0
                       END
                   ),
                   COUNT(t)
            FROM Ticket t
            WHERE t.organizationId = :organizationId
              AND t.createdAt >= :from
              AND t.createdAt < :to
              AND t.assignedAgentId IS NOT NULL
            GROUP BY t.assignedAgentId
            ORDER BY COUNT(t) DESC
            """)
    List<Object[]> getAgentWorkload(
            @Param("organizationId") UUID organizationId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query(value = """
            SELECT
                DATE(t.created_at) AS ticket_date,
                COUNT(*) AS ticket_count
            FROM tickets t
            WHERE t.organization_id = :organizationId
              AND t.created_at >= :from
              AND t.created_at < :to
            GROUP BY DATE(t.created_at)
            ORDER BY DATE(t.created_at)
            """,
            nativeQuery = true)
    List<Object[]> getTicketVolume(
            @Param("organizationId") UUID organizationId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );
}
