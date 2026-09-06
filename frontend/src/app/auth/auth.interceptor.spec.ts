import {
  HttpClient,
  HttpErrorResponse,
  provideHttpClient,
  withInterceptors,
} from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authService: AuthService;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    authService = TestBed.inject(AuthService);
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
  });

  it('does not set an Authorization header when there is no session', () => {
    http.get('/api/tickets').subscribe();
    const req = httpMock.expectOne('/api/tickets');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('attaches the bearer token when a session exists', () => {
    sessionStorage.setItem('servicedesk.accessToken', 'token-abc');

    http.get('/api/tickets').subscribe();
    const req = httpMock.expectOne('/api/tickets');
    expect(req.request.headers.get('Authorization')).toBe('Bearer token-abc');
    req.flush({});
  });

  it('clears the session on a 401 response', () => {
    sessionStorage.setItem('servicedesk.accessToken', 'token-abc');
    sessionStorage.setItem(
      'servicedesk.user',
      JSON.stringify({ id: '1', fullName: 'A', email: 'a@example.com', role: 'REQUESTER' }),
    );

    http.get('/api/tickets').subscribe({
      error: (error: unknown) => expect(error).toBeInstanceOf(HttpErrorResponse),
    });
    const req = httpMock.expectOne('/api/tickets');
    req.flush({ message: 'unauthorized' }, { status: 401, statusText: 'Unauthorized' });

    expect(authService.accessToken).toBeNull();
    expect(authService.currentUser()).toBeNull();
  });
});
