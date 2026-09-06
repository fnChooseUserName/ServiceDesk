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
