# Sidebar-001 — Basic Local Logging — Implementation Plan

## Status

Implemented. Compose configuration validated; startup/file-output verification
is pending. Not yet committed — awaiting review.

## Objective

Make local service failures easier to investigate by writing rolling log files
for `ticketing-service` and `notification-service` while preserving the
existing Docker console output.

## Scope

- Configure Spring Boot logging for each service to write a local rolling file.
- Bind each container's `/logs` directory to a separate host directory under
  `infra/logs/`.
- Keep console logging enabled so `docker compose logs` continues to work.
- Ignore generated logs in Git.
- Document where local log files are written and how to inspect them.
- Verify that both services start and produce log output in the mounted files.

## Proposed decisions

| Decision | Choice | Rationale |
|---|---|---|
| Log destination | `/logs/ticketing-service.log` and `/logs/notification-service.log` inside containers | Gives each service an explicit, stable path suitable for a Compose bind mount. |
| Rotation | Spring Boot/Logback rolling policy with a bounded file size and retained history | Prevents local development logs from growing without limit. |
| Console output | Preserve the default console appender | Keeps Docker-native diagnostics and existing workflows unchanged. |
| Host directories | `infra/logs/ticketing-service/` and `infra/logs/notification-service/` | Separates service output and makes the files easy to find from the repository root. |
| Secrets | Do not log passwords, JWTs, signing secrets, or authorization headers | Logging must not create a second credential-leak path. |
| Environment scope | Local Compose configuration only initially | This is a developer troubleshooting aid, not a production logging platform or centralized sink. |

## Files/components expected

```text
ticketing-service/
  src/main/resources/
    application-local.yml

notification-service/
  src/main/resources/
    application-local.yml

infra/
  docker-compose.yml
  logs/
    .gitkeep
    ticketing-service/
      .gitkeep
    notification-service/
      .gitkeep

.gitignore
README.md
```

The exact configuration file placement may follow the existing service
configuration layout if either service does not yet have a local profile file.
Generated `.log` files must remain ignored.

## Implementation sequence

1. Add bounded rolling file logging configuration for both services while
   retaining console output.
2. Add Compose bind mounts from the two host log directories to each
   container's `/logs` directory.
3. Add Git ignore rules and placeholder files/directories as needed so the
   expected paths are discoverable without committing generated logs.
4. Add a short README note explaining `docker compose logs` versus the
   per-service files under `infra/logs/`.
5. Start the Compose stack and verify each service writes startup/error
   entries to its own mounted file.

## Verification

- Existing service tests continue to pass.
- `docker compose up` starts both services with the logging mounts.
- Each service writes a readable log file under `infra/logs/`.
- `docker compose logs` still displays console output.
- Generated log content is not reported by `git status`.

## Explicit non-goals

- Centralized log aggregation, ELK/Loki, OpenTelemetry, or cloud log sinks.
- Production retention, alerting, or compliance policy.
- Structured JSON logging or correlation IDs.
- Changing log levels globally beyond what is required to enable file output.
- Logging request bodies, credentials, JWTs, or other sensitive values.
