# TerminatorPlus 6.1.4 - Paper 26.2 Prerelease

This prerelease combines the audited/debloated codebase with the Paper 26.2
compatibility port.

## Highlights

- Targets Paper/Minecraft `26.2` with Java 25.
- Removes audited dead code, stale documentation, and unused release material.
- Keeps the current combat, movement, inventory, and training behavior from the
  6.1.x line.
- Publishes `TerminatorPlus-6.1.4-BETA-mc26.2.jar` for prerelease testing.

## Validation

- Build command: `./gradlew build`
- Runtime duel validation: needs runtime test

This is a prerelease. Report compatibility or gameplay regressions with the
Paper build number and a server log.
