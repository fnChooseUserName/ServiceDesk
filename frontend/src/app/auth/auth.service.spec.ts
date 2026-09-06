import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';
import { AuthenticatedUser, LoginResponse } from './auth.models';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  const user: AuthenticatedUser = {
    id: '11111111-1111-1111-1111-111111111111',
    fullName: 'Ada Requester',
    email: 'requester@example.com',
    role: 'REQUESTER',
  };

  const loginResponse: LoginResponse = {
    accessToken: 'token-abc',
    tokenType: 'Bearer',
    expiresAt: '2099-01-01T00:00:00Z',
    user,
  };

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
  });

  it('has no current user by default', () => {
    expect(service.currentUser()).toBeNull();
    expect(service.accessToken).toBeNull();
  });

  it('stores the token and user on successful login', async () => {
    const loginPromise = service.login(user.email, 'secret');
    const req = httpMock.expectOne('http://localhost:8081/api/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: user.email, password: 'secret' });
    req.flush(loginResponse);

    await loginPromise;

    expect(service.accessToken).toBe('token-abc');
    expect(service.currentUser()).toEqual(user);
    expect(sessionStorage.getItem('servicedesk.accessToken')).toBe('token-abc');
  });

  it('restores the session from /api/auth/me when a token is stored', async () => {
    sessionStorage.setItem('servicedesk.accessToken', 'token-abc');

    const restorePromise = service.restoreSession();
    const req = httpMock.expectOne('http://localhost:8081/api/auth/me');
    expect(req.request.method).toBe('GET');
    req.flush(user);

    await restorePromise;

    expect(service.currentUser()).toEqual(user);
  });

  it('clears the session when restoring with an invalid/expired token', async () => {
    sessionStorage.setItem('servicedesk.accessToken', 'token-abc');
    sessionStorage.setItem('servicedesk.user', JSON.stringify(user));

    const restorePromise = service.restoreSession();
    const req = httpMock.expectOne('http://localhost:8081/api/auth/me');
    req.flush({ message: 'unauthorized' }, { status: 401, statusText: 'Unauthorized' });

    await restorePromise;

    expect(service.currentUser()).toBeNull();
    expect(service.accessToken).toBeNull();
  });

  it('does not call /api/auth/me when no token is stored', async () => {
    await service.restoreSession();
    httpMock.expectNone('http://localhost:8081/api/auth/me');
  });

  it('clears local state on logout', async () => {
    const loginPromise = service.login(user.email, 'secret');
    httpMock.expectOne('http://localhost:8081/api/auth/login').flush(loginResponse);
    await loginPromise;

    service.logout();

    expect(service.currentUser()).toBeNull();
    expect(service.accessToken).toBeNull();
  });
});
