import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TicketService } from './ticket.service';

describe('TicketService', () => {
  let service: TicketService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TicketService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('loads categories', async () => {
    const promise = service.getCategories();
    const request = httpMock.expectOne('http://localhost:8081/api/categories');
    expect(request.request.method).toBe('GET');
    request.flush([{ id: 'category-1', name: 'Hardware', description: 'Equipment', defaultSlaHours: 24 }]);

    await expect(promise).resolves.toHaveLength(1);
  });

  it('creates a ticket', async () => {
    const body = {
      title: 'Laptop issue',
      description: 'It will not start.',
      categoryId: 'category-1',
      priority: 'HIGH' as const,
    };
    const promise = service.createTicket(body);
    const request = httpMock.expectOne('http://localhost:8081/api/tickets');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(body);
    request.flush({ id: 'ticket-1', ...body, categoryName: 'Hardware', status: 'NEW', createdAt: '', updatedAt: '' });

    await expect(promise).resolves.toMatchObject({ id: 'ticket-1' });
  });

  it('loads the requester ticket list and detail', async () => {
    const listPromise = service.getTickets();
    const listRequest = httpMock.expectOne('http://localhost:8081/api/tickets');
    listRequest.flush([]);
    await expect(listPromise).resolves.toEqual([]);

    const detailPromise = service.getTicket('ticket-1');
    const detailRequest = httpMock.expectOne('http://localhost:8081/api/tickets/ticket-1');
    detailRequest.flush({ id: 'ticket-1' });
    await expect(detailPromise).resolves.toMatchObject({ id: 'ticket-1' });
  });
});
