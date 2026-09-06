# Service Desk

An internal IT service desk / ticketing platform, modeled on tools like
Jira Service Management or Zendesk. See `tech-brief.md` for the full scope
and technology choices, and `docs/architecture.md` for the intended MVP
architecture.

## Repository layout

- `ticketing-service/` — Spring Boot 4.1.x service, core ticketing domain.
- `notification-service/` — Spring Boot 4.1.x service, email notifications.
- `frontend/` — Angular SPA, presentation layer only.
- `infra/` — local Docker Compose stack and Postgres init scripts.
- `docs/` — architecture, implementation plan, decisions, and plans.

## Prerequisites

- Java 21 (Eclipse Temurin)
- Node.js 20+ and npm
- Docker Desktop (with Compose v2)

## Local development

### Backend services + database (Docker Compose)

From `infra/`:

```powershell
cd infra
docker compose up --build
```

This starts:

- PostgreSQL on `localhost:5432` (schemas `ticketing` and `notification`,
  user/password `servicedesk`/`servicedesk` — local dev only, not for
  production use)
- `ticketing-service` on `http://localhost:8081` — health check at
  `http://localhost:8081/actuator/health`
- `notification-service` on `http://localhost:8082` — health check at
  `http://localhost:8082/actuator/health`

Stop the stack with `docker compose down` (add `-v` to also drop the
Postgres data volume).

### Database migrations and seed data

Each backend service owns its database changes through Flyway migrations
under `src/main/resources/db/migration/`. Flyway applies pending migrations
automatically when that service starts and records them in the service
schema's `flyway_schema_history` table. Add schema changes as new,
forward-only migration files; do not edit an applied migration or run
application schema scripts manually from `infra/`.

The scripts under `infra/postgres/init/` only create the initial schemas when
PostgreSQL starts with a new data volume. They are not the application
migration mechanism and do not rerun for an existing volume.

Required reference data may be added through a Flyway migration. Local/demo
accounts and other environment-specific data should use an explicitly
enabled local seeder instead. For the authentication slice, you choose the
plaintext test passwords and supply them at startup through
`SEED_REQUESTER_PASSWORD`, `SEED_AGENT_PASSWORD`, and
`SEED_ADMIN_PASSWORD`. The application BCrypt-hashes those values before
storing them; you log in with the same plaintext values you selected. Do not
commit those passwords to the repository.

### Frontend (run independently for now)

The frontend is not yet part of the Compose stack (containerizing it is
deferred to a later slice). Run it directly:

```powershell
cd frontend
npm install
npm start
```

This serves the Angular shell at `http://localhost:4200`.

## Running tests

```powershell
cd ticketing-service
./mvnw test

cd notification-service
./mvnw test

cd frontend
npm test
```
