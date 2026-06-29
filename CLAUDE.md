# TerminatorPlus - Claude Compatibility Notice

This repository now uses `CODEX.md` as the canonical AI-agent playbook.

Read `CODEX.md` before making changes.

Current strategy target: `mc-26.1.2`.

Do not use this file as separate branch/process guidance. If `CODEX.md` and
older docs disagree, follow `CODEX.md` and current source code, then update
stale docs in a scoped docs task.

Useful guardrails:

- Use `./gradlew build -q` if a build is required.
- Do not run `shadowJar`.
- Do not run `reobfJar`.
- Do not run `gradlew clean`.
- Do not treat `master` as the development source of truth.
- Do not treat `mc-1.21.11` as primary unless a task explicitly scopes
  compatibility work.
