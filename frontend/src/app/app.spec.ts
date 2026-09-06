import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { App } from './app';

describe('App', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    sessionStorage.clear();
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render the heading and a login form when signed out', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('Service Desk');
    expect(compiled.querySelector('form')).toBeTruthy();
  });

  it('shows the signed-in identity and a logout button after a successful login', async () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    await fixture.whenStable();

    app.email = 'agent@example.com';
    app.password = 'secret';
    const loginPromise = app.login();

    const req = httpMock.expectOne('http://localhost:8081/api/auth/login');
    req.flush({
      accessToken: 'token-abc',
      tokenType: 'Bearer',
      expiresAt: '2099-01-01T00:00:00Z',
      user: {
        id: '1',
        fullName: 'Alex Agent',
        email: 'agent@example.com',
        role: 'AGENT',
      },
    });
    await loginPromise;
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Alex Agent');
    expect(compiled.querySelector('button')?.textContent).toContain('Log out');
  });
});
