package com.helpdesk.sla.repository;

import com.helpdesk.sla.entity.SlaPolicy;
import com.helpdesk.tickets.entity.TicketPriority;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SlaPolicyRepository
        extends JpaRepository<SlaPolicy, UUID> {

    List<SlaPolicy> findByOrganizationIdOrderByPriorityAsc(
            UUID organizationId
    );

    Optional<SlaPolicy> findByIdAndOrganizationId(
            UUID id,
            UUID organizationId
    );

    Optional<SlaPolicy> findByOrganizationIdAndPriority(
            UUID organizationId,
            TicketPriority priority
    );
}
