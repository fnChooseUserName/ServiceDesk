export type TicketPriority = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
export type TicketStatus = 'NEW';

export interface Category {
  id: string;
  name: string;
  description: string;
  defaultSlaHours: number;
}

export interface CreateTicketRequest {
  title: string;
  description: string;
  categoryId: string;
  priority: TicketPriority;
}

export interface TicketSummary {
  id: string;
  title: string;
  categoryName: string;
  priority: TicketPriority;
  status: TicketStatus;
  createdAt: string;
}

export interface Ticket extends TicketSummary {
  description: string;
  categoryId: string;
  updatedAt: string;
}
