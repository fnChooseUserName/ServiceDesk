# 0001 — Use Flyway for database schema migrations

## Status

Accepted (S1 — Project scaffolding & local dev stack).

## Context

`docs/architecture.md` ("Persistence architecture") notes that both
`ticketing-service` and `notification-service` need versioned, reproducible
schema migrations for their respective PostgreSQL schemas (`ticketing` and
`notification`), but the brief did not select a migration tool. This had to
be resolved before any schema (even the empty placeholder schema needed for
S1) could be committed.

The two realistic options were:

- **Flyway** — plain versioned SQL migration files, minimal configuration,
  first-class Spring Boot support via `spring-boot-starter-flyway`.
- **Liquibase** — changelog-based (XML/YAML/JSON/SQL), more powerful
  diffing/rollback tooling, more setup overhead.

## Decision

Use **Flyway**, with plain `.sql` migrations under
`src/main/resources/db/migration` in each service, each scoped to its own
schema via `spring.flyway.schemas`/`spring.flyway.default-schema`.

## Consequences

- Migrations are plain SQL — easy to read and review, consistent with the
  project's "favor explicit code over clever patterns" principle.
- No changelog abstraction layer; rollback requires a new forward migration
  rather than Liquibase's built-in rollback tags. Acceptable for this
  project's size.
- Each service owns and runs only its own schema's migrations, preserving
  the schema-ownership boundary from `docs/architecture.md`.
