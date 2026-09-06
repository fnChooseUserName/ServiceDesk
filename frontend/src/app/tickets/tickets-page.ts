import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { Category, Ticket, TicketPriority, TicketSummary } from './ticket.models';
import { TicketService } from './ticket.service';

@Component({
  selector: 'app-tickets-page',
  imports: [FormsModule],
  templateUrl: './tickets-page.html',
})
export class TicketsPage implements OnInit {
  readonly priorities: TicketPriority[] = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];
  readonly categories = signal<Category[]>([]);
  readonly tickets = signal<TicketSummary[]>([]);
  readonly selectedTicket = signal<Ticket | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly submitting = signal(false);

  title = '';
  description = '';
  categoryId = '';
  priority: TicketPriority = 'MEDIUM';

  constructor(
    readonly auth: AuthService,
    private readonly ticketService: TicketService,
    private readonly router: Router,
  ) {}

  async ngOnInit(): Promise<void> {
    try {
      const [categories, tickets] = await Promise.all([
        this.ticketService.getCategories(),
        this.ticketService.getTickets(),
      ]);
      this.categories.set(categories);
      this.tickets.set(tickets);
      this.categoryId = categories[0]?.id ?? '';
    } catch {
      this.errorMessage.set('Unable to load ticket data.');
    }
  }

  async submit(): Promise<void> {
    this.errorMessage.set(null);
    this.submitting.set(true);
    try {
      await this.ticketService.createTicket({
        title: this.title,
        description: this.description,
        categoryId: this.categoryId,
        priority: this.priority,
      });
      this.title = '';
      this.description = '';
      await this.loadTickets();
    } catch {
      this.errorMessage.set('Unable to submit ticket.');
    } finally {
      this.submitting.set(false);
    }
  }

  async viewTicket(id: string): Promise<void> {
    try {
      this.selectedTicket.set(await this.ticketService.getTicket(id));
    } catch {
      this.errorMessage.set('Unable to load ticket.');
    }
  }

  logout(): void {
    this.auth.logout();
    void this.router.navigate(['/login']);
  }

  private async loadTickets(): Promise<void> {
    this.tickets.set(await this.ticketService.getTickets());
  }
}
