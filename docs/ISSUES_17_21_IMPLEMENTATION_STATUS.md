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

All checks below ran under Java 25.0.4. The live checks used official stable
Paper 26.2 build 119 (`bb09b43`, published 2026-08-25); the downloaded server
jar matched SHA-256
`a8c9140c3075bd7c04973e9cdc491b21bfe6bad472b674ef932a4ae0fec19629`.

- Focused `BotUtilsTest`: passed.
- Focused `BotRuntimeOrchestratorTest`: passed.
- Focused respawn parser and snapshot-copy tests: passed.
- Focused `BotInspectionPaginationTest`: passed.
- Full `:TerminatorPlus-Plugin:test`: passed after runtime integration.
- Full `./gradlew clean build`: passed after all implementations; this includes
  API tests, plugin tests, and the movement-only contract check.

### Live Movement V2 matrix

The final run used real Paper bot entities, the centralized agent scheduler,
Bukkit events, survival inventories, and the production planner/action
executor. Movement V2 remains default-off because the complete matrix did not
pass.

| Scenario | Legacy | Movement V2 |
| --- | --- | --- |
| Ordinary pursuit | completed in 45 ticks | completed in 64 ticks; 0 action failures; 0 same-tick violations |
| Parkour | completed in 26 ticks | completed in 26 ticks; 0 action failures; 0 same-tick violations |
| Repeated bridge | bypassed without placing | 5 placements in 64 ticks; cobblestone 12→7; selected slot restored to 2 |
| Repeated pillar | no placements | 4 placements and reached Y=105.260 in 114 ticks, but recovered from 2 action failures |
| Door/gate/trapdoor corridor | did not complete | did not complete; one openable crossed, 0 action failures, repeated replanning |
| Two-block mining wall | no breaks | nondeterministic: focused runs broke both blocks and crossed with the pickaxe/selected slot restored, but the final run found no route and made 0 breaks |
| Cancelled placement | n/a | cancellation honored; 0 placements; 12 blocks retained; expected failure recorded |
| Clutch 4/8/16/32/48 | n/a | all five completed with 0 damage, real bucket empty/fill events, bucket and selected slot restored, and 0 same-tick violations |
| 25-bot performance | p95 3.418 ms, max 174.191 ms | p95 3.254 ms, max 171.102 ms; 112 plans |

Planner maxima occasionally exceeded the configured 2,000 µs phase target
(2,320.1 µs in pursuit and 2,170.5 µs in openables). The server-tick maxima
include scenario setup and JVM pauses; p95 is the useful comparison.

The live investigation also found a false occlusion when a ray aimed at one
half of a door hit its linked half. The executor now aims inside the target's
collision box and accepts the other half of the same door. A regression test
covers thin collision-shape targeting. Post-fix openable runs had zero action
failures, but route completion is still unreliable, so the feature gate was
not enabled.

### #18–#21 live acceptance

- #18: one centralized scheduler remained active with 46 concurrent GUI-test
  bots while each exposed independent runtime snapshots; lifecycle/unit tests
  also passed.
- #19: `/bot info` opened from a player-like bot, showed 45 entries on page 1
  and one on page 2, navigated list/detail views, refreshed manually, handled a
  removed bot with a safe placeholder, cancelled clicks/drags, printed console
  output, and left the editable `BotInventoryGUI` behavior intact.
- #20: actual health-zero death produced a pending respawn and restored the
  same UUID, creation location, skin, inventory/armor/offhand, selected slot,
  neural network object, target, kills, loadout lock, and player-list mode with
  zero drops/experience. Disabling respawn and `/bot reset` cancelled pending
  respawns; a training candidate did not respawn.
- #21: all 46 generated bot UUIDs were version 2. A Paper smoke run with
  LuckPerms 5.5.81 and Vault 1.7.3-b131 loaded successfully and produced no
  main-thread/offline-player lookup warning while bots were created and used.

## Remaining work

Issue #17 remains open. Before enabling Movement V2 by default, fix and rerun:

1. reliable physical traversal through opened doors, gates, and trapdoors;
2. deterministic escalation to mining from live approach positions;
3. pillar action failures; and
4. strict planner-budget overruns.

Issues #18–#21 have unit/build evidence and live acceptance evidence. Issues
#24 and #25 remain explicitly untouched.

## Handoff commands

```bash
git fetch origin
git switch codex/issues-17-21
./gradlew clean build
```

Use Java 25. Do not enable Movement V2 by default until the remaining live
matrix passes without action failures or planner-budget overruns.
