# TerminatorPlus 6.2.2 - Paper 26.2 Prerelease

TerminatorPlus 6.2.2 reorganizes the command surface into predictable,
discoverable groups while preserving the existing flat command paths for
compatibility.

## Highlights

- Adds canonical grouped `/bot` paths for spawning, inspection, movement,
  settings, equipment, presets, debugging, administration, and environment
  management.
- Adds grouped `/ai spawn`, `/ai train`, and `/ai inspect` paths while keeping
  the existing AI commands available.
- Updates tab completion and the bot management UI to follow the organized
  command surface.
- Normalizes target-goal input across case, hyphen, and underscore spelling.
- Documents the canonical command layout in `wiki/Commands.md`.

## Compatibility and scope

- Existing flat command paths remain registered for compatibility.
- This is a prerelease for Paper 26.2 and Java 25.
- Gameplay behavior is unchanged by the command organization itself; live
  Paper acceptance testing is still required before a production rollout.

## Artifact

`TerminatorPlus-6.2.2-BETA-mc26.2.jar`
