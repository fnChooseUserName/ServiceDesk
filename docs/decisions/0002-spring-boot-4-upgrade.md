# 0002 — Update Spring Boot to 4.1.x (from tech-brief's 3.x)

## Status

Accepted (S1 — Project scaffolding & local dev stack).

## Context

`tech-brief.md` originally pinned the backend framework to "Spring Boot
3.x". While scaffolding `ticketing-service` and `notification-service` in
this slice, Spring Initializr (start.spring.io) no longer offered any 3.x
release line — only 4.0.8 and 4.1.1 were available. This is an external
tooling change, not a project decision made in advance, so it's recorded
here rather than silently absorbed.

## Decision

Use **Spring Boot 4.1.x** (starting from 4.1.1) for both services. Java
stays at 21 (Temurin), which remains a supported baseline for Spring Boot 4.

`tech-brief.md` and `docs/architecture.md` have been updated to say
"Spring Boot 4.1.x" with a note pointing back to this decision.

## Consequences

- Some Spring Boot 4 module/package names differ from the 3.x line (for
  example, test-support artifacts split into narrower
  `spring-boot-starter-<feature>-test` modules, and
  `AutoConfigureMockMvc` moved to
  `org.springframework.boot.webmvc.test.autoconfigure`). This only affects
  import paths and dependency declarations already handled during
  scaffolding; no functional impact for this slice.
- Any future guidance, samples, or Stack Overflow answers referencing
  Spring Boot 3.x APIs should be checked against 4.x before reuse.
