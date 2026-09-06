# 0003 — Authentication mechanism and identity lifecycle

## Status

Accepted (S2 — Authentication & role identity).

## Context

`docs/architecture.md` ("Authentication and authorization", "Unresolved
decisions" #3) reserves authentication/authorization as a required
cross-cutting concern without prescribing a mechanism. `ticketing-service`
needed to let a caller authenticate and be recognized as `REQUESTER`,
`AGENT`, or `ADMIN` before any later slice could enforce per-role behavior.

The realistic options considered:

- **Server-side sessions** — simplest cookie-based model, but adds sticky
  session or shared session-store infrastructure once `ticketing-service`
  scales to multiple replicas (a stated future goal in the brief).
- **Stateless JWT bearer tokens issued by `ticketing-service` itself** — no
  session store, works uniformly across replicas, easy to verify in a
  filter/resource-server chain, and doesn't require an external identity
  provider that this project's brief never asked for.
- **External identity provider (OAuth2/OIDC)** — unnecessary infrastructure
  for a small internal MVP with three fixed roles and no third-party login
  requirement.

## Decision

- Authenticate with **email + password**, verified with **BCrypt** password
  hashes stored in `ticketing.app_user`.
- On success, issue a **signed JWT bearer access token** (HMAC-SHA-256,
  `JWT_SECRET`-supplied symmetric key), valid for a configurable lifetime
  (default 60 minutes, `JWT_EXPIRATION`).
- The token carries the user's ID, normalized email, full name, and role as
  claims; `ticketing-service` verifies the signature and expiry on every
  request and derives the caller's Spring Security authority
  (`ROLE_<ROLE>`) from the token — never from client-supplied data.
- No refresh tokens, session storage, or server-side revocation in this
  slice; an expired token requires logging in again.
- No self-registration. Local development/testing identities are created by
  an explicitly enabled, profile-gated local seeder
  (`SEED_USERS_ENABLED=true` + `SEED_REQUESTER_PASSWORD` /
  `SEED_AGENT_PASSWORD` / `SEED_ADMIN_PASSWORD`), not by a committed
  migration or a public endpoint. General user provisioning and role
  management remain scoped to S13 ("Administrative operations").

## Consequences

- `ticketing-service` remains the sole authority for identity and role
  checks; the frontend only reflects the role claim for UI purposes.
- Because verification is stateless, no additional shared session
  infrastructure is required as `ticketing-service` scales to multiple
  replicas.
- A role change made in a later slice does not affect an already-issued
  token until it expires (bounded by the 60-minute default lifetime);
  active revocation is out of scope until a future slice needs it.
- The signing secret must be identical across every `ticketing-service`
  replica in a given environment and is supplied only through deployment
  configuration/secrets, never committed to source control.
- Local/demo credentials are chosen by whoever starts the stack (via
  environment variables) and are hashed before storage; they are never
  present in the repository in plaintext.
