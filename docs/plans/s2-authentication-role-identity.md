# S2 — Authentication & Role Identity — Implementation Plan

## Status

Implemented and verified locally. All acceptance criteria below have been
checked. Not yet committed — awaiting your go-ahead.

## Slice recap (from `docs/implementation-plan.md`)

Let a caller authenticate and be recognized as `REQUESTER`, `AGENT`, or
`ADMIN`, since almost every later slice needs to know who is acting.

**Acceptance criteria (from plan):** a seeded user of each role can log in;
an authenticated request carries a verifiable identity and role; an
unauthenticated request to a protected endpoint is rejected.

**Risk called out:** the authentication mechanism and identity lifecycle are
unresolved and must be selected in this slice.

## Current repository state

Confirmed by inspection:

- `ticketing-service` is a Spring Boot 4.1.1 MVC application with Flyway,
  PostgreSQL, H2-backed tests, Actuator, and no domain or security code yet.
- The `ticketing` schema currently contains no domain tables; its only
  migration creates the schema.
- The Angular application is a standalone, routing-free shell with Vitest
  tests and no HTTP or authentication services.
- `notification-service` is independently owned and does not participate in
  user authentication.
- Docker Compose supplies local configuration to `ticketing-service` through
  environment variables.

## Relevant architecture decisions and constraints

- `ticketing-service` owns users and roles and is the authoritative
  authorization boundary.
- The frontend may use role information for presentation, but backend checks
  cannot trust role data supplied by the frontend.
- The domain has exactly one role per user: `REQUESTER`, `AGENT`, or `ADMIN`.
- User email is unique.
- The frontend communicates with `ticketing-service` over REST and does not
  access the database or `notification-service`.
- Schema changes use Flyway SQL migrations scoped to the `ticketing` schema.
- Authentication mechanism, token/session handling, provisioning, and role
  management must be decided here.
- User CRUD and role assignment remain assigned to S13, so S2 must not add
  general user-management endpoints.

## Decisions proposed for this slice

| Decision | Choice | Rationale |
|---|---|---|
| Authentication mechanism | Email/password login issuing a signed JWT bearer access token | Simple REST boundary for the Angular SPA, stateless across service replicas, and no external identity provider or session store is required. |
| Token signing | HMAC-SHA-256 with a base64-encoded 256-bit-or-stronger secret supplied by `JWT_SECRET` | Appropriate for one token issuer/resource server inside `ticketing-service`; avoids committed secrets and unnecessary key-management infrastructure. |
| Token lifetime | 60 minutes, configurable through `JWT_EXPIRATION` | Limits exposure of a leaked token while keeping the MVP login flow usable. |
| Refresh/logout behavior | No refresh token or server-side revocation in S2; logout removes the token from the SPA, and an expired token requires login again | Keeps the first identity slice small. Refresh-token storage and revocation would add an additional security-sensitive lifecycle not required by the acceptance criteria. |
| Password storage | BCrypt hashes only; plaintext passwords are never persisted or logged | Uses Spring Security's standard adaptive password hashing and satisfies the project's security requirements. |
| Identity lifecycle | No self-registration. Local seeded accounts satisfy S2; administrator provisioning and role changes remain in S13 | Preserves the implementation-plan boundary and avoids introducing a temporary public registration path. |
| Local seed data | A local-profile bootstrap component creates one user per role from environment-supplied passwords when explicitly enabled | Meets the seeded-user acceptance criterion without putting reusable plaintext credentials or production seed accounts in a Flyway migration. |
| Frontend token storage | `sessionStorage` | Survives navigation but is cleared when the browser session ends. It is still accessible to JavaScript, so the SPA must avoid unsafe HTML rendering; an HttpOnly-cookie design would require CSRF handling and a server-side logout/revocation model beyond this slice. |
| Authorization model | Spring Security authorities mapped from the persisted role, with method security enabled | Gives later slices a single backend pattern such as `hasRole('AGENT')` instead of duplicating manual role checks. |
| Error format | Spring `ProblemDetail` responses for validation and authentication failures, without revealing whether an email exists | Establishes only the error contract needed by this slice and avoids account enumeration. |

Record the accepted choices in
`docs/decisions/0003-authentication-and-identity-lifecycle.md` during
implementation.

## API contract

### `POST /api/auth/login`

Public endpoint.

Request:

```json
{
  "email": "requester@example.test",
  "password": "environment-supplied password"
}
```

Successful response (`200 OK`):

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresAt": "2026-09-06T20:00:00Z",
  "user": {
    "id": "00000000-0000-0000-0000-000000000000",
    "fullName": "Local Requester",
    "email": "requester@example.test",
    "role": "REQUESTER"
  }
}
```

Invalid credentials return `401 Unauthorized` with the same response shape
whether the email is unknown or the password is incorrect. Invalid request
data returns `400 Bad Request`.

### `GET /api/auth/me`

Protected endpoint. Requires
`Authorization: Bearer <accessToken>`.

Returns the authenticated user's `id`, `fullName`, `email`, and `role`.
Missing, expired, malformed, or incorrectly signed tokens return
`401 Unauthorized`.

No production-only role-probe endpoints will be added. Role enforcement will
be proven through security integration tests using test-only secured
controllers, leaving later slices to apply the same method-security pattern
to real domain endpoints.

## Persistence model

Add a Flyway migration for `ticketing.app_user`:

| Column | Type/constraint |
|---|---|
| `id` | UUID primary key |
| `full_name` | non-null varchar |
| `email` | non-null unique varchar, stored normalized to lowercase |
| `password_hash` | non-null varchar |
| `role` | non-null varchar constrained to `REQUESTER`, `AGENT`, or `ADMIN` |
| `created_at` | non-null timestamp with time zone |

The Java domain model remains `User`; the explicit `app_user` table name
avoids ambiguity with database/system user terminology. Authentication
queries use normalized email equality and never expose `password_hash`
through API DTOs.

## Local seeded identities

The `local` Spring profile will support an explicitly enabled bootstrap
component that creates missing users without changing existing rows:

| Role | Default email |
|---|---|
| `REQUESTER` | `requester@example.test` |
| `AGENT` | `agent@example.test` |
| `ADMIN` | `admin@example.test` |

Passwords come from `SEED_REQUESTER_PASSWORD`, `SEED_AGENT_PASSWORD`, and
`SEED_ADMIN_PASSWORD`. The bootstrap is enabled only when
`SEED_USERS_ENABLED=true`; it fails startup with a clear configuration error
if enabled without all required passwords. Docker Compose and the README will
document these variables but will not commit password values.

Automated tests will create their own users with test-only credentials and
will not depend on the local bootstrap.

## Files/components to create or update

```text
ticketing-service/
  pom.xml
    add Spring Security, OAuth2 resource-server/JWT, Data JPA, and Validation
  src/main/java/com/servicedesk/ticketing/
    auth/
      AuthController.java
      AuthService.java
      JwtService.java
      SecurityConfig.java
      AuthenticationEntryPoint.java
      dto/LoginRequest.java
      dto/LoginResponse.java
      dto/AuthenticatedUserResponse.java
    user/
      User.java
      UserRole.java
      UserRepository.java
      DatabaseUserDetailsService.java
    config/
      LocalUserSeeder.java
  src/main/resources/
    application.yml
    application-local.yml
    db/migration/V2__create_app_user.sql
  src/test/java/com/servicedesk/ticketing/
    auth/...
    user/...
  src/test/resources/application.yml

frontend/
  src/app/
    app.ts
    app.html
    app.css
    app.config.ts
    app.spec.ts
    auth/
      auth.service.ts
      auth.service.spec.ts
      auth.interceptor.ts
      auth.interceptor.spec.ts
      auth.models.ts

infra/
  docker-compose.yml
    enable the local profile/seeder and pass JWT/seed configuration from the
    environment

docs/decisions/
  0003-authentication-and-identity-lifecycle.md

README.md
  document required local auth environment variables and the three seed
  identities
```

Exact package grouping may be adjusted during implementation if Spring Boot
4.1 APIs make a smaller arrangement clearer, but the auth and user concerns
will remain separate from future ticket domain code.

## Implementation sequence

1. **Record the architecture decision:** add ADR 0003 covering JWT bearer
   authentication, BCrypt, local-only seeded identities, token lifetime, and
   deferred identity lifecycle features.
2. **Add persistence:** introduce Spring Data JPA and the Flyway migration for
   `app_user`; add the `User`, `UserRole`, and repository types. Normalize
   emails at the application boundary before persistence or lookup.
3. **Add authentication services:** load users from the repository, verify
   BCrypt passwords, issue signed JWTs with subject/user ID and role claims,
   and map verified claims into Spring Security authorities.
4. **Configure HTTP security:** keep `/actuator/health` and
   `/api/auth/login` public; require authentication for `/api/**`; configure
   stateless request handling, bearer-token validation, method security,
   configurable CORS for the Angular origin, and explicit `401`/`403`
   responses. Disable CSRF because credentials are sent in an authorization
   header rather than an automatically attached cookie.
5. **Expose the auth API:** implement login and current-user endpoints with
   validated DTOs and `ProblemDetail` failures. Do not return password data
   or distinguish unknown-email from wrong-password failures.
6. **Add local seed support:** create the conditional local-profile seeder,
   hash supplied passwords with BCrypt, and insert only missing users.
7. **Wire local configuration:** update Compose to activate the local profile
   and forward required JWT/seed environment variables. Update the README
   with setup and login examples.
8. **Add the Angular login flow:** configure `HttpClient`, add a login form,
   call the login endpoint, retain the returned token and identity in
   `sessionStorage`, attach the bearer token through an interceptor, restore
   identity through `/api/auth/me`, display the authenticated user's name and
   role, and provide client-side logout.
9. **Verify the vertical slice:** start the local stack and SPA; log in as
   each seeded role; confirm `/api/auth/me` reports the correct identity;
   confirm missing, altered, and expired tokens are rejected.

## Automated tests

### `ticketing-service`

- Flyway/JPA test proving a user can be persisted and found by normalized
  email and that duplicate email is rejected.
- Authentication service tests for successful login, unknown email, wrong
  password, and indistinguishable failures for unknown-email and
  wrong-password attempts.
- JWT tests for valid claims, role mapping, expiration, and signature
  rejection.
- MockMvc integration tests proving:
  - each role can log in and receives its own identity/role;
  - `/api/auth/me` rejects an unauthenticated request;
  - a valid bearer token reaches the protected endpoint;
  - malformed, altered, and expired tokens are rejected;
  - test-only role-protected endpoints allow the required role and return
    `403 Forbidden` to a different authenticated role;
  - `/actuator/health` remains public.
- Local seeder tests proving it is disabled by default, requires all three
  passwords when enabled, creates one user per role, hashes passwords, and is
  idempotent without overwriting existing users.

### Frontend

- Login form/component tests for required inputs, successful identity
  display, generic invalid-credentials feedback, and logout.
- Auth service tests for login, `/api/auth/me` restoration, token persistence,
  and clearing invalid/expired sessions after a `401`.
- Interceptor tests proving the bearer header is attached only when a token
  exists.

No new test framework or external identity service is introduced.

## Acceptance criteria as verifiable checks

- [x] With local seeding explicitly enabled, one user exists for each of
      `REQUESTER`, `AGENT`, and `ADMIN`, using environment-supplied passwords.
- [x] Each seeded user can log in through the SPA and directly through
      `POST /api/auth/login`.
- [x] A successful login returns a signed, expiring bearer token and the
      authenticated user's ID, name, normalized email, and role.
- [x] `GET /api/auth/me` returns the identity represented by a valid token
      rather than trusting identity or role values sent by the frontend.
- [x] An unauthenticated request to `/api/auth/me` returns `401`.
- [x] An authenticated user without a required role receives `403` from a
      role-protected method, as proven by integration tests.
- [x] Missing, malformed, altered, and expired JWTs are rejected.
- [x] Stored passwords are BCrypt hashes; plaintext passwords and JWT signing
      secrets are absent from the repository, API responses, and logs.
- [x] The SPA can restore the current identity during the browser session and
      removes local auth state on logout or an authentication `401`.
- [x] Existing health checks remain publicly accessible.
- [x] `mvnw test` passes for `ticketing-service`, and `npm test` passes for
      the frontend.

## Explicit non-goals (deferred)

- Self-registration, password reset/change, email verification, multi-factor
  authentication, account lockout, or external OAuth/OIDC providers.
- Refresh tokens, persistent login across browser sessions, token revocation,
  and cross-device logout.
- General user CRUD, account deactivation, or role assignment (S13).
- Ticket-, category-, comment-, assignment-, or status-specific permissions;
  those are applied when the corresponding domain endpoints are introduced.
- Authentication between `ticketing-service` and `notification-service`.
- Angular route guards or role-specific navigation; there are no protected
  feature routes yet.

## Risks and implementation notes

- JWT contents are signed, not encrypted. Include only the minimum identity
  claims required for authorization and UI display.
- Role changes do not affect an already issued token until it expires. The
  proposed 60-minute lifetime bounds this stale-authorization window; active
  revocation remains outside S2.
- `sessionStorage` reduces persistence but does not protect against XSS.
  Angular templates must continue to avoid unsafe HTML injection, and token
  values must never be logged.
- The HMAC signing secret must be stable across all `ticketing-service`
  replicas in an environment and supplied through deployment configuration.
- Local seeding is configuration-gated and additive. It must never silently
  reset passwords or roles on an existing account.
