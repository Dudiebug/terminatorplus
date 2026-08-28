# TerminatorPlus 6.2.0 - Paper 26.2

TerminatorPlus 6.2.0 strengthens the bot runtime with isolated per-bot state,
centralized scheduling, live inspection, opt-in respawning, safer UUID
handling, and a validated Movement V2 foundation for Paper 26.2.

## Highlights

- Per-bot runtime and agent state are isolated while a centralized scheduler
  coordinates bot work.
- `/bot info` provides paginated list and detail inspection for loaded bots,
  with safe handling when a bot disappears while a view is open.
- `/bot respawn` supports opt-in respawn behavior that preserves bot identity,
  state, equipment, and training metadata, with reset and disable cancellation.
- Generated bot UUIDs use the version-2 identity path and coexist cleanly with
  Vault and LuckPerms on Paper.
- Movement V2 adds collision-aware planning, player-like traversal actions,
  bridge and pillar placement, mining escalation, openable interaction, and
  water-bucket clutch planning while preserving the movement-only contract.
- The Movement V2 gate remains opt-in until the remaining live traversal,
  mining, pillar-recovery, and planner-budget cases are resolved.

## Validation

- Official Paper 26.2 build 119 under Java 25.0.4.
- Clean Gradle build and focused traversal regression test passed.
- Live acceptance covered pursuit, parkour, bridge, pillar, placement
  cancellation, mining-focused runs, openables, five clutch heights, respawn,
  GUI inspection, 25-bot performance, and Vault/LuckPerms compatibility.
- Full measurements and known limits are documented in
  `docs/ISSUES_17_21_IMPLEMENTATION_STATUS.md` and `docs/MOVEMENT_V2.md`.

## Artifact

`TerminatorPlus-6.2.0-BETA-mc26.2.jar`

Use this build with Paper 26.2 and Java 25.
