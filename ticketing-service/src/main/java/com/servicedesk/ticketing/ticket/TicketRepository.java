package com.servicedesk.ticketing.ticket;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    List<Ticket> findAllByRequesterIdOrderByCreatedAtDesc(UUID requesterId);

    Optional<Ticket> findByIdAndRequesterId(UUID ticketId, UUID requesterId);
}
