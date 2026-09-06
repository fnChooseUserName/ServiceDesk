package com.servicedesk.ticketing.ticket.dto;

import com.servicedesk.ticketing.ticket.Ticket;
import com.servicedesk.ticketing.ticket.TicketPriority;
import com.servicedesk.ticketing.ticket.TicketStatus;

import java.time.Instant;
import java.util.UUID;

public record TicketSummaryResponse(
        UUID id,
        String title,
        String categoryName,
        TicketPriority priority,
        TicketStatus status,
        Instant createdAt
) {

    public static TicketSummaryResponse from(Ticket ticket) {
        return new TicketSummaryResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getCategory().getName(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getCreatedAt()
        );
    }
}
