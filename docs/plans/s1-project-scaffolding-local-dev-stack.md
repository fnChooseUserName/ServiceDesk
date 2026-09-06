# S1 — Project Scaffolding & Local Dev Stack — Implementation Plan

## Status

Implemented and verified locally. All acceptance criteria below have been
checked. Not yet committed — awaiting your go-ahead.

## Slice recap (from `docs/implementation-plan.md`)

Establish two independently buildable Spring Boot services and an Angular
shell, wired together by a local Docker Compose stack with PostgreSQL, so
every later slice has somewhere to land.

**Acceptance criteria (from plan):** `docker-compose up` starts Postgres,
both services, and the SPA; each service responds to a health check; the SPA
loads a blank page; nothing is deployed to real functionality yet.

**Risk called out:** migration tooling for schema versioning was unselected.
**Resolved:** Flyway (per your selection above).

## Current repository state

Confirmed by inspection: the repository contains only `AGENTS.md`,
`README.md`, `tech-brief.md`, `docs/` (with `architecture.md`,
`implementation-plan.md`, and empty `decisions/` and `plans/`
subdirectories), and `.ai-tooling/copilot-instructions.md`. No service code,
build files, or Docker configuration exist yet. This slice starts from a
clean slate.

## Decisions made for this slice

| Decision | Choice | Rationale |
|---|---|---|
| Migration tool | Flyway | Your selection; plain SQL migrations, minimal setup, standard Spring Boot integration (`flyway-core` + `spring-boot-starter`). |
| Repo layout | Monorepo, one subfolder per deployable: `ticketing-service/`, `notification-service/`, `frontend/`, `infra/` (compose + DB init) | Matches the existing single-repo setup; keeps the local dev stack wiring in one place. |
| Health check | Spring Boot Actuator `/actuator/health` on each service | Simplest way to satisfy "responds to a health check" without hand-rolling an endpoint; no custom logic needed yet. |
| DB per service | One Postgres instance, two schemas (`ticketing`, `notification`), one Flyway migration history table per schema (`flyway_schema_history` scoped via each service's own `spring.flyway.schemas`) | Matches architecture doc's schema-ownership boundary; avoids a shared migration history across services. |

## Assumptions requiring your confirmation

These are reasonable defaults but affect generated files — flag any you want
changed before I implement:

1. **Versions:** Java 21 (Temurin), Spring Boot 3.3.x (latest stable 3.x),
   Maven wrapper (`mvnw`), Angular 18 (latest LTS at plan time) with the
   Angular CLI's default standalone-component setup, Node 20 LTS.
2. **Ports:** `ticketing-service` → 8081, `notification-service` → 8082,
   `frontend` (ng serve / dev container) → 4200, PostgreSQL → 5432.
3. **Local credentials:** Postgres superuser `postgres`/`postgres` for local
   dev only, via Compose environment variables (not committed secrets, per
   `.ai-tooling` security guidance). Real secrets management is out of scope
   until deployment slices (S14+).
4. **Group/artifact IDs:** `com.servicedesk.ticketing` / `ticketing-service`
   and `com.servicedesk.notification` / `notification-service`.
5. **Frontend package manager:** npm (Angular CLI default).
6. **No frontend routing/state library yet** — this slice only needs a
   blank shell page to load, per acceptance criteria.

## Files/components to create

```
ticketing-service/
  pom.xml
  mvnw, mvnw.cmd, .mvn/wrapper/...
  src/main/java/com/servicedesk/ticketing/TicketingServiceApplication.java
  src/main/resources/application.yml
  src/main/resources/db/migration/V1__init_ticketing_schema.sql   (placeholder: create schema only)
  src/test/java/com/servicedesk/ticketing/TicketingServiceApplicationTests.java

notification-service/
  pom.xml
  mvnw, mvnw.cmd, .mvn/wrapper/...
  src/main/java/com/servicedesk/notification/NotificationServiceApplication.java
  src/main/resources/application.yml
  src/main/resources/db/migration/V1__init_notification_schema.sql (placeholder: create schema only)
  src/test/java/com/servicedesk/notification/NotificationServiceApplicationTests.java

frontend/
  (generated via `ng new frontend`) — standalone Angular app, default routing disabled or a single blank root route
  src/app/app.component.* (default/blank shell)

infra/
  docker-compose.yml
  postgres/init/01-create-schemas.sql   (creates `ticketing` and `notification` schemas + per-service DB roles if needed)

docs/decisions/
  0001-migration-tool-flyway.md   (short ADR capturing the Flyway decision and why, per the empty docs/decisions/ folder already present)

README.md
  updated with "Local development" section: prerequisites, how to run `docker-compose up`, how to hit each health check, how to reach the frontend.
```

No existing files are modified other than `README.md` (additive section only).

## Implementation sequence

1. **ADR:** Add `docs/decisions/0001-migration-tool-flyway.md` recording the
   Flyway decision (short — context, decision, consequences).
2. **`ticketing-service` scaffold:** Generate via Spring Initializr
   equivalent (Maven, Java 21, dependencies: Web, Actuator, Flyway,
   PostgreSQL driver, Test). Commit `mvnw`. Add `application.yml` with
   Postgres connection (env-var driven), `spring.flyway.schemas: ticketing`.
   Add a single placeholder Flyway migration that only ensures the schema
   exists (idempotent `CREATE SCHEMA IF NOT EXISTS`) — no domain tables yet
   (those belong to later slices). Verify `mvnw spring-boot:run` boots and
   `/actuator/health` returns `UP` against a locally running Postgres.
3. **`notification-service` scaffold:** Same steps as step 2, scoped to the
   `notification` schema.
4. **Frontend scaffold:** `ng new frontend` (standalone app), strip the
   default Angular CLI starter content down to a blank/minimal shell page
   (per acceptance criteria: "SPA loads a blank page"). Verify `ng serve`
   loads.
5. **Postgres init scripts:** Add `infra/postgres/init/01-create-schemas.sql`
   so the schemas exist even before Flyway runs (Flyway migration also
   guards with `CREATE SCHEMA IF NOT EXISTS` so it works either way — this
   keeps the two independent should either run first).
6. **Docker Compose:** Add `infra/docker-compose.yml` defining `postgres`,
   `ticketing-service`, `notification-service` (built from per-service
   Dockerfiles), with correct inter-service environment variables (DB
   host/port/credentials). `frontend` is **not** part of Compose in this
   slice — run separately via `ng serve`.
7. **Per-service Dockerfiles:** Minimal multi-stage Dockerfile per Spring
   Boot service (Maven build stage + Temurin JRE runtime stage) so Compose
   can build images locally.
8. **README update:** Document prerequisites, the `docker-compose up`
   workflow for Postgres + backend services, which URLs/ports to check, and
   the separate `ng serve` step for the frontend. Note that frontend
   containerization is deferred to a later slice (S14).
9. **Full stack verification:** Run `docker-compose up`, confirm Postgres
   healthy and both services' `/actuator/health` return `UP`; separately run
   `ng serve` and confirm the frontend serves a blank page at its port.

## Tests

Per `AGENTS.md`/architecture testing strategy, this slice is infrastructure
with minimal domain logic, so tests are correspondingly minimal but not
skipped:

- **`ticketing-service`:** Default Spring Boot context-load test
  (`TicketingServiceApplicationTests`, generated by Initializr) confirming
  the application context starts. Optionally a `WebTestClient`/`MockMvc`
  test hitting `/actuator/health` and asserting `200`/`UP`.
- **`notification-service`:** Same as above, scoped to its own application
  class.
- **Frontend:** Default Angular CLI-generated `app.component.spec.ts` smoke
  test (component creates successfully), run via `ng test`.
- **Manual/integration verification (not automated in this slice):**
  `docker-compose up` full-stack smoke check listed in step 9 above. No
  automated end-to-end test harness is introduced here — that's addressed
  later (S14 containerized validation, S16 CI).

## Acceptance criteria (restated as verifiable checks)

- [x] `docker-compose up` (from `infra/`) starts three containers:
      `postgres`, `ticketing-service`, `notification-service`, with no crash
      loops.
- [x] `GET http://localhost:8081/actuator/health` returns `200` with
      `{"status":"UP"}`.
- [x] `GET http://localhost:8082/actuator/health` returns `200` with
      `{"status":"UP"}`.
- [x] `ng serve` (run independently, outside Compose) serves a
      blank/minimal Angular shell page at `http://localhost:4200` with no
      console errors.
- [x] Postgres contains both `ticketing` and `notification` schemas after
      startup (verifiable via `psql`/pgAdmin).
- [x] `mvnw test` passes for both services; `ng test` passes for the
      frontend (each run standalone, outside Compose).
- [x] No domain tables, business endpoints, or authentication exist yet —
      confirming scope stayed within S1.

## Explicit non-goals (deferred to later slices)

- Any `User`/`Ticket`/`Category`/etc. tables or entities (S3+).
- Authentication (S2).
- Kubernetes/`kind` manifests (S15).
- CI pipeline (S16).
- Production-hardened Dockerfiles or image publishing (S14/S17).

## Resolved decisions

1. **Frontend containerization:** Deferred. For S1, `frontend` is run
   independently via `ng serve` on the host, not inside Docker Compose.
   Compose covers only `postgres`, `ticketing-service`, and
   `notification-service`. Containerizing the frontend is noted as a
   follow-up (naturally fits S14, "Containerized full-stack validation",
   which already covers building/validating Docker images for the stack).
   Acceptance criteria updated accordingly (see below).
2. **ADR format:** Freeform markdown (context/decision/consequences)
   confirmed.
3. **Versions/ports/naming:** Confirmed as proposed — Java 21/Temurin,
   Spring Boot 3.3.x, Angular 18, Node 20, ports 8081/8082/4200/5432, group
   id `com.servicedesk.*`.

## Next step

Plan approved and implemented. See root `README.md` for how to run the
stack, and `docs/decisions/0001-migration-tool-flyway.md` and
`docs/decisions/0002-spring-boot-4-upgrade.md` for decisions made during
implementation. Awaiting your review before committing.
