# Project Guidelines for Copilot

> Place this file at `.github/copilot-instructions.md` in each repo.
> It's loaded automatically for every Copilot Chat, agent-mode, and CLI
> request in that repo — no per-prompt setup needed.
> Edit the bracketed sections to fit each project.

## Project context
- [Project name / one-line purpose]
- Stack: [e.g. TypeScript, Node.js, React, SQLite]
- This is a small, local/personal application — not an enterprise system.
  Prioritize simplicity and working code over elaborate abstraction,
  microservices, or premature scalability patterns.

## Coding standards
- Follow existing naming/formatting conventions already present in the repo;
  don't introduce a new style mid-project.
- Prefer readable, explicit code over clever one-liners.
- Keep functions small and single-purpose.
- Add comments only where intent isn't obvious from the code itself —
  don't narrate every line.
- No dead code, no commented-out blocks left behind.

## Security
- Treat security as a real priority, not an afterthought, even for small apps:
  - Never hardcode secrets, API keys, or credentials — use environment
    variables / a `.env` file (and make sure it's gitignored).
  - Validate and sanitize all external input (user input, file uploads,
    API responses).
  - Use parameterized queries — never string-concatenated SQL.
  - Default to the least-privilege option for any file, network, or
    process permission the app requests.
- Flag anything that touches auth, payments, or personal data explicitly
  rather than silently implementing it.

## Testing
- Write or update tests for new logic where the project has a test setup.
- Don't invent a testing framework if one isn't already configured — ask first.

## Working style (agent mode / CLI)
- Before a multi-file change, briefly state the plan and the files you intend
  to touch.
- Prefer the smallest change that solves the problem. Don't refactor
  unrelated code while completing a task.
- If a task looks like it needs more than ~15–20 tool calls or touches more
  than a handful of files, pause and confirm scope with me before continuing —
  don't keep going indefinitely.
- When unsure between two reasonable approaches, ask rather than guessing.

## Explanations (chat mode)
- When I ask you to explain a concept rather than write code, assume I want
  a clear breakdown of *why*, not just a definition — use examples where useful.
