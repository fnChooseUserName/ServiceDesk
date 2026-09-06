# Project instructions

## Project
This is a small greenfield Zendesk-like ticketing application.

## Engineering principles
- Prefer simple solutions over abstractions.
- Do not introduce infrastructure unless required by the tech brief.
- Keep domain logic independent from UI concerns.
- Favor explicit code over clever patterns.
- Do not add dependencies without justification.
- Every feature should have automated tests appropriate to its layer.

## Development workflow
- Read tech-brief.md before making architectural decisions.
- Preserve existing architectural decisions unless there is a compelling reason
  to change them.
- Before implementing a feature, inspect the relevant existing code.
- Keep changes narrowly scoped.
- Run the relevant tests after implementation.
- Do not refactor unrelated code while implementing a feature.

## Git
- Keep commits small and coherent.
- Do not modify unrelated files.

## Blockers and Issues
- When faced with an issue in implementation, do not attempt workarounds or refactoring - escalate immediately for clarification
- If a bug surfaces during testing or review, do not attempt to auto-fix - escalate immediately for clarification