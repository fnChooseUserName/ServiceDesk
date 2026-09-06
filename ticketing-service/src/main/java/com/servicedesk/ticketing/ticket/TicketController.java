package com.servicedesk.ticketing.ticket;

import com.servicedesk.ticketing.auth.JwtService;
import com.servicedesk.ticketing.ticket.dto.CreateTicketRequest;
import com.servicedesk.ticketing.ticket.dto.TicketResponse;
import com.servicedesk.ticketing.ticket.dto.TicketSummaryResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final JwtService jwtService;

    public TicketController(TicketService ticketService, JwtService jwtService) {
        this.ticketService = ticketService;
        this.jwtService = jwtService;
    }

    @PostMapping
    @PreAuthorize("hasRole('REQUESTER')")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse create(
            @Valid @RequestBody CreateTicketRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ticketService.create(request, jwtService.userId(jwt));
    }

    @GetMapping
    @PreAuthorize("hasRole('REQUESTER')")
    public List<TicketSummaryResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return ticketService.listForRequester(jwtService.userId(jwt));
    }

    @GetMapping("/{ticketId}")
    @PreAuthorize("hasRole('REQUESTER')")
    public TicketResponse get(
            @PathVariable UUID ticketId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ticketService.getForRequester(ticketId, jwtService.userId(jwt));
    }

    @ExceptionHandler(TicketNotFoundException.class)
    public ProblemDetail handleNotFound() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Ticket not found");
    }

    @ExceptionHandler(InvalidTicketRequestException.class)
    public ProblemDetail handleInvalidRequest(InvalidTicketRequestException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}
