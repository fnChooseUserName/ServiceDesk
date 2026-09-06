import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AuthenticatedUser, LoginResponse } from './auth.models';

/**
 * Base URL of ticketing-service's REST API. Hardcoded for now since S2 is
 * the first slice to call the backend at all; revisit if/when a build-time
 * environment configuration mechanism is introduced for later slices.
 */
const API_BASE_URL = 'http://localhost:8081';

const TOKEN_STORAGE_KEY = 'servicedesk.accessToken';
const USER_STORAGE_KEY = 'servicedesk.user';

/**
 * Holds the current authentication session (bearer token + identity) in
 * sessionStorage and exposes it to the rest of the SPA. See
 * docs/decisions/0003-authentication-and-identity-lifecycle.md for why
 * sessionStorage was chosen over a persistent store.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly currentUserSignal = signal<AuthenticatedUser | null>(this.readStoredUser());

  /** The currently authenticated user, or null if logged out. */
  readonly currentUser = this.currentUserSignal.asReadonly();

  constructor(private readonly http: HttpClient) {}

  get accessToken(): string | null {
    return sessionStorage.getItem(TOKEN_STORAGE_KEY);
  }

  async login(email: string, password: string): Promise<void> {
    const response = await firstValueFrom(
      this.http.post<LoginResponse>(`${API_BASE_URL}/api/auth/login`, { email, password }),
    );
    sessionStorage.setItem(TOKEN_STORAGE_KEY, response.accessToken);
    sessionStorage.setItem(USER_STORAGE_KEY, JSON.stringify(response.user));
    this.currentUserSignal.set(response.user);
  }

  /**
   * Restores the current identity from a previously stored token by calling
   * /api/auth/me, so a page reload doesn't lose the session within the
   * browser session. Clears the session if the token is missing/expired.
   */
  async restoreSession(): Promise<void> {
    if (!this.accessToken) {
      return;
    }
    try {
      const user = await firstValueFrom(
        this.http.get<AuthenticatedUser>(`${API_BASE_URL}/api/auth/me`),
      );
      sessionStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user));
      this.currentUserSignal.set(user);
    } catch {
      this.clearSession();
    }
  }

  logout(): void {
    this.clearSession();
  }

  /** Clears local auth state without calling the backend (no revocation in S2). */
  clearSession(): void {
    sessionStorage.removeItem(TOKEN_STORAGE_KEY);
    sessionStorage.removeItem(USER_STORAGE_KEY);
    this.currentUserSignal.set(null);
  }

  private readStoredUser(): AuthenticatedUser | null {
    const raw = sessionStorage.getItem(USER_STORAGE_KEY);
    return raw ? (JSON.parse(raw) as AuthenticatedUser) : null;
  }
}
