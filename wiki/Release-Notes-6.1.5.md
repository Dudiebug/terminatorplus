# TerminatorPlus 6.1.5 - Paper 26.2

This release fixes bot skin selection and placement for Paper/Minecraft `26.2`.

## Highlights

- Explicit skin usernames now keep their entered casing during Paper profile lookup.
- Bot names and fallback skin behavior remain independent of the requested skin username.
- `/bot place` works with the corrected command completion handling.
- Publishes `TerminatorPlus-6.1.5-BETA-mc26.2.jar`.

## Validation

- Build command: `./gradlew build -q`
- Runtime skin and placement validation: needs runtime test
