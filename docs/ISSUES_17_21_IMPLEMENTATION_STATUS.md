# Issues #17–#21 implementation status

Branch: `codex/issues-17-21`

This branch implements issues #17 through #21 except for the explicitly
excluded issues #24 and #25. It was built in a separate worktree so unrelated
changes in the original workspace were not modified.

## Fixed scope and decisions

- #17 Movement V2 becomes the default only after the Paper arena matrix
  passes. Until then, the existing movement path remains the default.
- #18 keeps one plugin-wide scheduler while giving every bot one owned runtime
  state object and an immutable inspection snapshot.
- #19 is read-only, resolves bots by UUID, shows 45 bots per page, and refreshes
  only when requested by the viewer.
- #20 `/bot respawn` is runtime-only, defaults to `false`, waits 20 ticks, and
  preserves the bot's identity and loadout. Training generations opt out.
- #21 fixes NPC identity at UUID creation rather than suppressing Vault or
  LuckPerms warnings or moving unsafe Bukkit work off-thread.
- #24 and #25 are out of scope and have not been changed on this branch.

## Completed

### #21 — main-thread lookup warning

Commit: `ca7f0dc Fix NPC UUID classification for Vault lookups`

- `BotUtils.randomSteveUUID()` now emits UUID version 2, which identifies the
  generated profile as an NPC to LuckPerms/Vault integrations.
- The existing Steve skin parity behavior remains intact.
- A regression test checks UUID version, variant, parity, and uniqueness across
  512 generated IDs.

### #17 — Movement V2 implementation candidate

Merge commit: `0026e94 Merge Movement V2 candidate for issue 17`

- Integrated the existing Movement V2 planner, controller, traversal action
  executor, Bukkit navigation context, tests, documentation, configuration,
  and status command from `codex/movement-v2-player-actions`.
- Added planner elapsed time to `/botenvironment movementV2Status` output.
- The feature gate intentionally remains `false`; default-on is still blocked
  on live arena evidence.

### #18 — per-bot runtime ownership

Commit: `3b19000 Own mutable agent state per bot`

- Added one `BotRuntime` per registered bot under the existing centralized
  scheduler.
- Moved target, stuck, center/tower, and clutch cooldown state out of shared
  bot-keyed maps.
- Added immutable `BotRuntimeSnapshot` telemetry through the agent API.
- Bot registration, removal, and reset now create and clean runtime ownership
  explicitly.
- Added lifecycle tests for reuse, tick counting, removal, and reset.

### #20 — opt-in bot respawning

Commit: `6d9cdcd Restore opt-in bot respawning`

- Restored `/bot respawn [true|false]`; querying without a value reports the
  current runtime-only state and pending count.
- The default is disabled and disabling it cancels pending respawns.
- Death snapshots preserve original spawn, UUID, name, skin, inventory, armor,
  offhand, selected slot, loadout lock, default item, neural network, target,
  kills, and player-list mode.
- Drops and experience are suppressed when a respawn is pending.
- The old entity is fully cleaned before its UUID is reused after 20 ticks.
- Reinforcement/training candidates opt out; ordinary `/ai random` bots retain
  the global behavior.
- Added command parsing and inventory snapshot-copy tests.

### #19 — loaded-bot inspection GUI

Commit: `2b3bf82 Add read-only bot inspection GUI`

- `/bot info` opens a paginated loaded-bot list for players and provides a
  compact console listing.
- `/bot info <name>` opens a detail view for players and prints details to the
  console.
- List selection uses UUIDs, safely handles removed bots, and supports more
  than 45 active bots.
- Detail panels show identity, health, target, combat intent/state, movement,
  inventory/loadout, per-bot runtime telemetry, and Movement V2 status.
- All menu interactions are read-only and refresh only on explicit clicks.
- Existing editable `BotInventoryGUI` behavior remains separate.
- Added boundary tests for pagination and page clamping.

## Verification completed

All checks below ran in the Java 25 development container used by this
worktree:

- Focused `BotUtilsTest`: passed.
- Focused `BotRuntimeOrchestratorTest`: passed.
- Focused respawn parser and snapshot-copy tests: passed.
- Focused `BotInspectionPaginationTest`: passed.
- Full `:TerminatorPlus-Plugin:test`: passed after runtime integration.
- Full `./gradlew clean build`: passed after all implementations; this includes
  API tests, plugin tests, and the movement-only contract check.
- Official Paper download metadata was checked and Paper 26.2 build 119 was the
  latest stable build at the time of this handoff.

## Remaining work

These items are deliberately not represented as complete:

1. Run the live Paper 26.2 arena matrix documented in `docs/MOVEMENT_V2.md`:
   ordinary pursuit, parkour, bridge, pillar, door/gate/trapdoor interaction,
   mining, clutch heights, inventory restoration, action cancellation, and
   bounded planner timing.
2. Compare legacy and Movement V2 runs for route completion, fallbacks, action
   failures, same-tick action violations, inventory changes, damage, and server
   tick impact.
3. If that matrix passes, change both configuration and code fallbacks for
   `ai.movement.v2.enabled` to `true`, update the Movement V2 documentation,
   and rerun the complete build and Paper smoke test.
4. On Paper, smoke-test `/bot respawn true` through death and verify same UUID,
   original location, loadout, target/network metadata, no drops, and reset or
   disable cancellation.
5. On Paper with a player, smoke-test `/bot info` list/detail navigation,
   pagination, manual refresh, removed-bot handling, and unchanged editable
   inventory GUI behavior.
6. With Vault/LuckPerms installed, confirm generated version-2 bot UUIDs no
   longer trigger the main-thread lookup warning.
7. Post the resulting evidence to issues #17–#21 and close only the issues whose
   acceptance criteria have been demonstrated. No issue has been closed by
   this branch yet.

## Handoff commands

```bash
git fetch origin
git switch codex/issues-17-21
./gradlew clean build
```

Use Java 25. Do not enable Movement V2 by default until the remaining live
matrix passes.
