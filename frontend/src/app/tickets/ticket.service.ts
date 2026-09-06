import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import {
  Category,
  CreateTicketRequest,
  Ticket,
  TicketSummary,
} from './ticket.models';

const API_BASE_URL = 'http://localhost:8081';

@Injectable({ providedIn: 'root' })
export class TicketService {
  constructor(private readonly http: HttpClient) {}

  getCategories(): Promise<Category[]> {
    return firstValueFrom(this.http.get<Category[]>(`${API_BASE_URL}/api/categories`));
  }

  createTicket(request: CreateTicketRequest): Promise<Ticket> {
    return firstValueFrom(
      this.http.post<Ticket>(`${API_BASE_URL}/api/tickets`, request),
    );
  }

  getTickets(): Promise<TicketSummary[]> {
    return firstValueFrom(this.http.get<TicketSummary[]>(`${API_BASE_URL}/api/tickets`));
  }

  getTicket(id: string): Promise<Ticket> {
    return firstValueFrom(this.http.get<Ticket>(`${API_BASE_URL}/api/tickets/${id}`));
  }
}
