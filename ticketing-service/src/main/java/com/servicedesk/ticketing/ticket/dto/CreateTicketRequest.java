package com.servicedesk.ticketing.ticket.dto;

import com.servicedesk.ticketing.ticket.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateTicketRequest(
        @NotBlank
        @Size(max = 200)
        String title,
        @NotBlank
        @Size(max = 5000)
        String description,
        @NotNull
        UUID categoryId,
        @NotNull
        TicketPriority priority
) {
}
