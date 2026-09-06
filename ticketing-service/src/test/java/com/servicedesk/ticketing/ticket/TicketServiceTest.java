package com.servicedesk.ticketing.ticket;

import com.servicedesk.ticketing.category.Category;
import com.servicedesk.ticketing.category.CategoryRepository;
import com.servicedesk.ticketing.ticket.dto.CreateTicketRequest;
import com.servicedesk.ticketing.ticket.dto.TicketResponse;
import com.servicedesk.ticketing.user.User;
import com.servicedesk.ticketing.user.UserRepository;
import com.servicedesk.ticketing.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TicketService ticketService;

    @Test
    void createsNewTicketForAuthenticatedRequester() {
        UUID requesterId = UUID.randomUUID();
        Category category = new Category(UUID.randomUUID(), "Hardware", "Equipment", 24);
        User requester = new User(
                requesterId,
                "Requester",
                "requester@example.test",
                "hash",
                UserRole.REQUESTER,
                Instant.now()
        );
        CreateTicketRequest request = new CreateTicketRequest(
                "  Laptop issue  ",
                "  It will not start.  ",
                category.getId(),
                TicketPriority.HIGH
        );

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(userRepository.findById(requesterId)).thenReturn(Optional.of(requester));
        when(ticketRepository.save(org.mockito.ArgumentMatchers.any(Ticket.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TicketResponse response = ticketService.create(request, requesterId);

        assertThat(response.title()).isEqualTo("Laptop issue");
        assertThat(response.description()).isEqualTo("It will not start.");
        assertThat(response.categoryName()).isEqualTo("Hardware");
        assertThat(response.status()).isEqualTo(TicketStatus.NEW);
        assertThat(response.priority()).isEqualTo(TicketPriority.HIGH);
        verify(ticketRepository).save(org.mockito.ArgumentMatchers.any(Ticket.class));
    }

    @Test
    void rejectsUnknownCategory() {
        UUID categoryId = UUID.randomUUID();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.create(
                new CreateTicketRequest("Title", "Description", categoryId, TicketPriority.LOW),
                UUID.randomUUID()
        ))
                .isInstanceOf(InvalidTicketRequestException.class)
                .hasMessage("Unknown category");
    }
}
