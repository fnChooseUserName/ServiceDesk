package com.servicedesk.ticketing.ticket;

import com.servicedesk.ticketing.category.Category;
import com.servicedesk.ticketing.category.CategoryRepository;
import com.servicedesk.ticketing.ticket.dto.CreateTicketRequest;
import com.servicedesk.ticketing.ticket.dto.TicketResponse;
import com.servicedesk.ticketing.ticket.dto.TicketSummaryResponse;
import com.servicedesk.ticketing.user.User;
import com.servicedesk.ticketing.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public TicketService(
            TicketRepository ticketRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository
    ) {
        this.ticketRepository = ticketRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TicketResponse create(CreateTicketRequest request, UUID requesterId) {
        String title = request.title().trim();
        String description = request.description().trim();
        if (title.isEmpty() || description.isEmpty()) {
            throw new InvalidTicketRequestException("Title and description must not be blank");
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new InvalidTicketRequestException("Unknown category"));
        User requester = userRepository.findById(requesterId)
                .orElseThrow(TicketNotFoundException::new);
        Instant now = Instant.now();

        Ticket ticket = ticketRepository.save(new Ticket(
                UUID.randomUUID(),
                title,
                description,
                request.priority(),
                TicketStatus.NEW,
                category,
                requester,
                now,
                now
        ));
        return TicketResponse.from(ticket);
    }

    @Transactional(readOnly = true)
    public List<TicketSummaryResponse> listForRequester(UUID requesterId) {
        return ticketRepository.findAllByRequesterIdOrderByCreatedAtDesc(requesterId).stream()
                .map(TicketSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse getForRequester(UUID ticketId, UUID requesterId) {
        Ticket ticket = ticketRepository.findByIdAndRequesterId(ticketId, requesterId)
                .orElseThrow(TicketNotFoundException::new);
        return TicketResponse.from(ticket);
    }
}
