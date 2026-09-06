# S3 — Ticket Submission — Implementation Plan

## Status

Proposed. Awaiting approval before implementation.

## Slice recap (from `docs/implementation-plan.md`)

Let an authenticated requester submit a ticket and see their own submitted
tickets — the first slice that produces real domain value.

**Acceptance criteria (from plan):** a logged-in requester creates a ticket
with title, description, category, and priority; it is persisted with status
`NEW`; the requester can retrieve/view it; a requester cannot view another
requester's tickets.

**Risk called out:** `slaDueAt` and the initial `TicketStatusHistory` row are
deferred to later slices. S3 will persist no SLA value and will not create a
history row.

## Current repository state

Confirmed by targeted inspection:

- `ticketing-service` owns the `ticketing` PostgreSQL schema and applies
  forward-only Flyway migrations at startup.
- S2 provides the `User` entity, normalized email, UUID identity, JWT
  authentication, and method-security support. The verified JWT subject is the
  authoritative requester identity.
- No `Category` or `Ticket` domain types, migrations, repositories, or REST
  endpoints exist yet.
- The Angular app is standalone and currently routing-free. S2 provides
  `AuthService`, bearer-token interception, and the authenticated identity
  display; S3 will introduce the first minimal routed workflow rather than
  keeping login and ticket submission in one shell component.
- `notification-service` is not involved in S3. Ticket-created notification
  delivery remains S9.

## Relevant architecture decisions and constraints

- `ticketing-service` is the system of record for tickets, categories, and
  requester ownership.
- The frontend communicates only with `ticketing-service`; it never accesses
  PostgreSQL or `notification-service`.
- Authorization and ownership filtering must be enforced by the backend. The
  frontend may hide controls but cannot be trusted to select a requester or
  filter another user's tickets.
- `User` has exactly one of `REQUESTER`, `AGENT`, or `ADMIN`. S3 implements
  requester submission and requester-owned reads only; agent queue access is
  S5 and administrative category management is S4.
- Schema changes belong in new Flyway migrations under
  `ticketing-service/src/main/resources/db/migration/`. Fixed reference data
  may be inserted by migration; environment-specific accounts continue to use
  the local seeder.
- The domain model defines `NEW` as the initial ticket status and priority
  values `CRITICAL`, `HIGH`, `MEDIUM`, and `LOW`.
- The architecture requires ticket writes to be transactional. S3 has one
  ticket write and does not yet write status history or emit notifications.
- REST details are still an unresolved architecture item. This slice must
  establish the initial frontend-to-ticketing contract without introducing
  versioning or abstractions not needed by the MVP.

## Decisions proposed for this slice

| Decision | Choice | Rationale |
|---|---|---|
| Category provisioning | Seed a small fixed category set in a Flyway migration | Categories are reference data in S3 and are not admin-managed until S4; migration makes the data reproducible in every environment without adding another runtime seeder contract. |
| Initial categories | `Hardware`, `Software`, and `Access` with short descriptions and default SLA values | Provides enough variety for the form and future category management while keeping the seed set intentionally small. `defaultSlaHours` is stored but not used to calculate S3 tickets. |
| Ticket priority | Enum/string constrained to `CRITICAL`, `HIGH`, `MEDIUM`, `LOW` | Matches the domain model and avoids accepting arbitrary priority values from the SPA. |
| Ticket status | Persist `NEW` on creation | Matches the documented state machine; later slices own transitions away from `NEW`. |
| Requester identity | Set `requester_id` from the verified JWT subject | Prevents a caller from submitting on behalf of another user by posting an arbitrary user ID or email. |
| Own-ticket reads | `GET /api/tickets` returns only the authenticated requester’s tickets; `GET /api/tickets/{id}` returns only that requester’s ticket | Makes the acceptance criterion explicit and keeps ownership enforcement in one backend service layer. An inaccessible/unknown ID returns `404` rather than revealing whether another requester owns it. |
| S3 role boundary | Ticket submission and requester reads require `ROLE_REQUESTER` | Agent queue access and administrative operations are deferred to S5/S13; S3 should not create partially authorized parallel behavior. |
| SLA fields | Persist `sla_due_at` as nullable only if included in the initial ticket table; do not calculate or expose it in S3 responses | S3 explicitly defers SLA calculation to S8. The implementation should avoid presenting a misleading due date. |
| Status history | No history table or initial history row in S3 | The implementation plan explicitly defers the first history row to S5, when agent acknowledgement introduces the first meaningful transition. |
| Error handling | Use the existing Spring `ProblemDetail`/security error approach and validation failures for bad commands | Reuses S2 behavior rather than creating a ticket-specific error envelope. |
| Frontend navigation | Add minimal Angular routes for `/login` and `/tickets`, redirect `/` to the appropriate route, and use a client-side auth guard for presentation flow | Makes the new workflow navigable and keeps login separate from ticket submission. The guard improves UX only; every API remains protected by `ticketing-service`. |

## API contract

### `GET /api/categories`

Protected endpoint. Requires an authenticated caller; the initial SPA uses it
to populate the requester form.

Response (`200 OK`):

```json
[
  {
    "id": "00000000-0000-0000-0000-000000000000",
    "name": "Hardware",
    "description": "Laptops, monitors, peripherals, and other equipment",
    "defaultSlaHours": 24
  }
]
```

The endpoint returns active seeded categories in stable name order. S4 will add
category management and active/inactive behavior; S3 need not expose mutation
endpoints.

### `POST /api/tickets`

Protected endpoint. Requires `ROLE_REQUESTER`.

Request:

```json
{
  "title": "Laptop will not start",
  "description": "The power light turns on but the laptop does not boot.",
  "categoryId": "00000000-0000-0000-0000-000000000000",
  "priority": "HIGH"
}
```

Validation:

- `title`: required, trimmed, 1–200 characters;
- `description`: required, trimmed, 1–5000 characters;
- `categoryId`: required UUID referencing an existing seeded category;
- `priority`: required and one of `CRITICAL`, `HIGH`, `MEDIUM`, `LOW`.

The service sets `id`, `requesterId` from the JWT subject, `status` to `NEW`,
and `createdAt`/`updatedAt` to the current time. The request cannot supply a
requester, status, timestamps, assignee, SLA, or history data.

Successful response (`201 Created`):

```json
{
  "id": "00000000-0000-0000-0000-000000000000",
  "title": "Laptop will not start",
  "description": "The power light turns on but the laptop does not boot.",
  "category": {
    "id": "00000000-0000-0000-0000-000000000000",
    "name": "Hardware"
  },
  "priority": "HIGH",
  "status": "NEW",
  "createdAt": "2026-09-06T21:00:00Z",
  "updatedAt": "2026-09-06T21:00:00Z"
}
```

Unknown category IDs return `400 Bad Request` (or the repository's established
validation/problem-detail equivalent), without creating a ticket.

### `GET /api/tickets`

Protected endpoint. Requires `ROLE_REQUESTER`.

Returns the authenticated requester’s tickets only, ordered by `createdAt`
descending. The list response may omit the full description to keep the
minimal list view compact, but must include enough information to identify and
open each ticket: `id`, `title`, category name, priority, status, and
`createdAt`.

### `GET /api/tickets/{ticketId}`

Protected endpoint. Requires `ROLE_REQUESTER`.

Returns the full ticket representation only when `ticketId` belongs to the
authenticated requester. A ticket owned by another requester is treated as
not found and returns `404`.

## Persistence model

### Category

Add a `Category` entity mapped to `ticketing.category`:

| Column | Type/constraint |
|---|---|
| `id` | UUID primary key |
| `name` | non-null varchar, unique |
| `description` | non-null varchar |
| `default_sla_hours` | non-null integer, positive |

The migration seeds the initial fixed categories. S4 may later add
deactivation; S3 does not need to add an `active` column or expose mutation
operations unless implementation inspection shows the existing brief requires
it now.

### Ticket

Add a `Ticket` entity mapped to `ticketing.ticket`:

| Column | Type/constraint |
|---|---|
| `id` | UUID primary key |
| `title` | non-null varchar(200) |
| `description` | non-null text |
| `priority` | non-null varchar constrained to the four domain values |
| `status` | non-null varchar constrained to `NEW` initially |
| `category_id` | non-null foreign key to `ticketing.category` |
| `requester_id` | non-null foreign key to `ticketing.app_user` |
| `created_at` | non-null timestamp with time zone |
| `updated_at` | non-null timestamp with time zone |

The S3 migration should not add an assignee, history table, or derived SLA
calculation. Those concerns belong to S5/S6/S8 and can be introduced through
later forward-only migrations. If an implementation choice includes nullable
future columns for a simpler entity mapping, it must not expose or populate
them in S3.

Repository queries must encode ownership explicitly, for example:

- find all tickets by requester ID ordered by creation time descending;
- find one ticket by ticket ID and requester ID.

Do not load all tickets and filter in Java or rely on frontend filtering.

## Files/components to create or update

```text
ticketing-service/
  src/main/java/com/servicedesk/ticketing/
    category/
      Category.java
      CategoryRepository.java
      CategoryController.java
      dto/CategoryResponse.java
    ticket/
      Ticket.java
      TicketPriority.java
      TicketStatus.java
      TicketRepository.java
      TicketService.java
      TicketController.java
      dto/CreateTicketRequest.java
      dto/TicketResponse.java
      dto/TicketSummaryResponse.java
  src/main/resources/
    db/migration/V3__create_categories_and_tickets.sql
  src/test/java/com/servicedesk/ticketing/
    category/...
    ticket/...

frontend/
  src/app/
    app.routes.ts
    auth/
      auth.guard.ts
    tickets/
      ticket.models.ts
      ticket.service.ts
      tickets-page.ts
      tickets-page.html
      tickets-page.spec.ts
      ticket.service.spec.ts
```

Exact package grouping may be adjusted to match implementation details, but
category and ticket domain/application concerns must remain separate from the
existing `auth` and `user` packages.

## Implementation sequence

1. **Define persistence:** add the V3 Flyway migration for `category` and
   `ticket`, including constraints, foreign keys, and fixed category seed rows.
2. **Add domain types:** implement `Category`, `Ticket`, `TicketPriority`, and
   `TicketStatus` using the existing explicit JPA/schema conventions.
3. **Add repositories:** provide category lookup and requester-scoped ticket
   queries so ownership is enforced at the data-access boundary.
4. **Implement ticket application logic:** validate category existence, read
   requester UUID from the verified `Jwt`/authentication principal, create
   tickets with status `NEW`, and return requester-owned list/detail results.
5. **Expose REST endpoints:** add category read, ticket create, own-list, and
   own-detail endpoints with request validation, `201 Created` creation
   responses, and existing ProblemDetail/security behavior.
6. **Add Angular navigation and ticket flow:** configure standalone Angular
   routes for `/login` and `/tickets`, redirect `/` based on authentication,
   protect `/tickets` with a client-side guard, and keep login separate from
   the ticket page. Add ticket models/service, load categories for
   authenticated requesters, add the submission form, reload the own-ticket
   list after a successful creation, and provide a minimal detail display.
7. **Verify the vertical slice:** run backend/frontend tests and a local
   Compose smoke test proving a requester can create/read their ticket while a
   second requester cannot retrieve it.

## Automated tests

### `ticketing-service`

- Flyway/JPA persistence tests for category seed data, ticket persistence,
  enum/constraint values, and required foreign keys.
- Service tests for successful creation, `NEW` status assignment, unknown
  category rejection, and use of the authenticated requester identity rather
  than request-supplied identity.
- MockMvc integration tests proving:
  - an authenticated requester can list categories;
  - a requester can create a ticket with the expected persisted fields;
  - the create response does not accept or expose caller-controlled
    requester/status values;
  - a requester sees only their own tickets;
  - a requester receives `404` for another requester's ticket;
  - unauthenticated and non-requester access is rejected according to the
    S2 security contract;
  - existing health and authentication endpoints remain functional.

### Frontend

- Ticket service tests for category loading, ticket creation, own-list
  loading, and detail loading with the existing bearer interceptor.
- App/component tests for required form fields, category/priority selection,
  successful submission, visible validation/API errors, and rendering the
  requester’s submitted tickets.

No new test framework, database, or service dependency is introduced.

## Acceptance criteria as verifiable checks

- [ ] The V3 migration creates `Category` and `Ticket` tables with the
      required constraints and seeds the fixed categories.
- [ ] A logged-in requester can submit title, description, category, and
      priority through the SPA.
- [ ] The SPA provides navigable login and ticket pages, redirects the root
      path appropriately, and does not rely on the client guard as the
      backend authorization mechanism.
- [ ] A successful submission persists a ticket with the authenticated
      requester, selected category, selected priority, and status `NEW`.
- [ ] `GET /api/tickets` and `GET /api/tickets/{id}` return the requester’s
      own tickets through the backend API.
- [ ] A requester cannot retrieve another requester’s ticket, including by
      guessing its ID.
- [ ] Invalid input and unknown category IDs are rejected without creating a
      ticket.
- [ ] `slaDueAt` is not calculated or presented as an S3 result, and no
      initial `TicketStatusHistory` row is created.
- [ ] Existing S2 authentication behavior and public health checks remain
      functional.
- [ ] Backend and frontend automated tests pass, and the local Compose smoke
      test verifies the cross-requester ownership boundary.

## Explicit non-goals (deferred)

- SLA calculation or display (`slaDueAt`) — S8.
- Initial or subsequent `TicketStatusHistory` rows — S5/S6.
- Agent queue access, acknowledgement, assignment, or status transitions —
  S5/S6.
- Category creation, editing, deactivation, or admin UI — S4.
- Ticket comments and internal-note visibility — S7.
- Ticket-created notification calls or notification reliability — S9.
- Attachments, requester self-registration, pagination, search, reporting,
  and bulk ticket operations.

## Risks and implementation notes

- The initial category seed values are product-facing reference data. If the
  user wants different names or SLA defaults, that should be decided before
  implementation because changing an applied seed migration requires a new
  forward migration.
- Returning `404` for another requester’s ticket avoids exposing whether a
  guessed UUID exists. This behavior must be tested at the service/API layer,
  not implemented only in the Angular UI.
- A requester can create a ticket only for themselves. The backend must use
  the verified JWT subject and must not accept `requesterId` in the create
  request.
- S3 intentionally leaves SLA and history fields out of the write path. Later
  slices must add them through explicitly planned forward-only migrations
  (S6 for status history and S8 for SLA fields) without editing V3 after it
  has been applied.
