package com.helpdesk.tickets.repository;

import com.helpdesk.tickets.entity.TicketMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TicketMessageRepository
        extends JpaRepository<TicketMessage, UUID> {

    List<TicketMessage> findByTicketIdAndInternalNoteFalseOrderByCreatedAtAscIdAsc(
            UUID ticketId
    );

    List<TicketMessage> findByTicketIdOrderByCreatedAtAscIdAsc(
            UUID ticketId
    );
}
