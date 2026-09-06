/**
 * Shapes returned by ticketing-service's authentication API
 * (see docs/plans/s2-authentication-role-identity.md).
 */
export type UserRole = 'REQUESTER' | 'AGENT' | 'ADMIN';

export interface AuthenticatedUser {
  id: string;
  fullName: string;
  email: string;
  role: UserRole;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
  user: AuthenticatedUser;
}
