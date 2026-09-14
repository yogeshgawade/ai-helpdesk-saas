package com.helpdesk.tickets.repository;

import com.helpdesk.tickets.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

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
}
