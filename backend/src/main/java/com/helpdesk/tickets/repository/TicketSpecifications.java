package com.helpdesk.tickets.repository;

import com.helpdesk.tickets.entity.Ticket;
import com.helpdesk.tickets.entity.TicketPriority;
import com.helpdesk.tickets.entity.TicketStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class TicketSpecifications {

    private TicketSpecifications() {
    }

    public static Specification<Ticket> belongsToOrganization(
            UUID organizationId
    ) {
        return (root, query, cb) ->
                cb.equal(root.get("organizationId"), organizationId);
    }

    public static Specification<Ticket> belongsToCustomer(
            UUID customerId
    ) {
        return (root, query, cb) ->
                cb.equal(root.get("customerId"), customerId);
    }

    public static Specification<Ticket> subjectContains(
            String search
    ) {
        return (root, query, cb) ->
                cb.like(
                        cb.lower(root.get("subject")),
                        "%" + search.toLowerCase() + "%"
                );
    }

    public static Specification<Ticket> hasStatus(
            TicketStatus status
    ) {
        return (root, query, cb) ->
                cb.equal(root.get("status"), status);
    }

    public static Specification<Ticket> hasPriority(
            TicketPriority priority
    ) {
        return (root, query, cb) ->
                cb.equal(root.get("priority"), priority);
    }

    public static Specification<Ticket> hasCategory(
            String category
    ) {
        return (root, query, cb) ->
                cb.equal(root.get("category"), category);
    }
}
