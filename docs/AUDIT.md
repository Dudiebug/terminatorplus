# TerminatorPlus Audit

## Executive summary

The repo already contains pieces of the new architecture: `CombatDirector`, `CombatIntent`, `MovementState`, movement-controller routing, movement-only contract checks, named loadouts, presets, debug logging, and legacy fallback behavior.

The main problem is strategic drift. The codebase and wiki still expose the project as a broad bot/plugin feature set. The new direction needs to make 1v1 PvP quality the central organizing principle.

## Repo structure

Expected high-level structure:

```text
CLAUDE.md
README.md
settings.gradle.kts
build.gradle.kts
TerminatorPlus-Plugin/
TerminatorPlus-API/
src/main/resources/plugin.yml
wiki/
```

Important runtime areas:

```text
TerminatorPlus-Plugin/src/main/java/net/nuggetmc/tplus/
TerminatorPlus-Plugin/src/main/java/net/nuggetmc/tplus/bot/
TerminatorPlus-Plugin/src/main/java/net/nuggetmc/tplus/bot/combat/
TerminatorPlus-Plugin/src/main/java/net/nuggetmc/tplus/bot/loadout/
TerminatorPlus-Plugin/src/main/java/net/nuggetmc/tplus/bot/preset/
TerminatorPlus-API/src/main/java/net/nuggetmc/tplus/api/agent/legacyagent/
```

## Build system summary

Use only:

```bash
./gradlew build -q
```

Do not use:

```bash
./gradlew shadowJar
./gradlew reobfJar
./gradlew clean
```

If Gradle/buildSrc cache corruption appears, delete only:

```text
buildSrc/build
```

Then retry:

```bash
./gradlew build -q
```

## Branch/release summary

Current strategy target:

```text
mc-26.1.2 = primary active target
mc-1.21.11 = compatibility/older branch
master = display branch
```

Any docs saying `mc-1.21.11` is primary need to be reconciled with the new strategy.

## Runtime flow

### Startup

`TerminatorPlus` initializes plugin singleton state, validates server version expectations, creates bot manager/combat systems, registers commands/events/API bridge, and cleans up static or singleton state on disable.

### Bot creation

Bots are spawned through the bot manager. Creation involves server/world integration, mock connection behavior, skin lookup, player list visibility, and manager registration.

### Tick loop

The tick loop coordinates target choice, movement, combat planning, movement execution, combat execution, fall handling, inventory/loadout guarantees, and debug state.

### Target selection

`LegacyAgent` still contains much of the target acquisition and validation logic. It should be treated as high-risk and preserved unless a narrow runtime-tested change is needed.

### Movement

There are two important movement modes:

- legacy movement
- movement-controller network movement

The new direction should prefer movement-controller work, but legacy fallback must not be broken.

### Combat decision

`CombatDirector` should remain the owner of combat planning, item selection, timing, committed phases, and execution.

### Weapon behavior

Weapon behaviors should be tuned, not rewritten first. Mace, trident, crystal, anchor, cobweb, elytra, wind charge, pearl, healing, shield, sword, and axe behavior should be subordinated to 1v1 fundamentals.

### Inventory/loadouts

`BotInventory` and loadout application logic are high-risk because Paper/NMS internals and MockConnection behavior can cause rollback or packet sync issues.

### Presets

Preset serialization is useful and should not be rewritten during combat tuning.

### Debug logging

Combat debug is useful for runtime truth. Keep logs focused on timing, state, target HP, charge, branch decisions, and success/failure outcomes.

## Current combat architecture

Current architecture is moving toward:

```text
CombatDirector.plan()
  -> CombatIntent
  -> MovementBrainRouter / MovementNetwork
  -> MovementOutputApplier
  -> MovementState
  -> CombatDirector.execute()
```

This is the correct direction.

## Current strengths

- Existing combat behavior classes.
- Existing movement-controller architecture.
- Existing movement-only contract check.
- Existing loadouts and presets.
- Existing debug hooks.
- Existing branch-family concept.
- Existing support for advanced mechanics.
- Existing legacy fallback path.

## Current weaknesses

- Strategy is spread across old wiki pages.
- User-facing docs still emphasize broad feature coverage.
- Advanced tools can distract from fundamentals.
- Runtime testing plan is not central enough.
- Tuning constants are scattered.
- `LegacyAgent` remains large and risky.
- Some docs likely lag source code.
- It is not obvious which files are safe for agents to edit.
- AI agents may follow old wiki strategy unless redirected.

## Likely reason the bot still feels weak against humans

Needs runtime test, but likely causes are:

1. Weak spacing discipline.
2. Poor close-range pressure.
3. Overcommitted special moves.
4. Attack timing that is technically valid but tactically poor.
5. Bot not punishing human recovery/eating/bowing windows.
6. Movement not keeping enough range control under pressure.
7. Advanced tools firing without strong setup.
8. Too much logic optimized around feature demonstration instead of duel conversion.

## Top 10 blockers to strong 1v1 PvP

1. Inconsistent melee range control.
2. Face-hugging or overshooting.
3. Freezing while waiting for crit windows.
4. Weak shield pressure.
5. Weak defensive reset behavior.
6. Low-HP player not chased hard enough.
7. Eating/bowing not punished reliably.
8. Advanced tools not gated by tactical setup.
9. Debug data not tied to duel pass/fail metrics.
10. Old docs pulling agents back toward broad-feature work.

## Top 10 risky files/areas

1. `LegacyAgent.java`
2. `Bot.java`
3. `BotInventory.java`
4. `CombatDirector.java`
5. `BotCombatTiming.java`
6. `OpportunityScanner.java`
7. Paper/NMS access paths
8. MockConnection behavior
9. Brain persistence/training files
10. Loadout/preset serialization

## What already works and should not be rewritten

- Bot lifecycle setup/cleanup.
- NMS inventory path.
- Presets.
- Basic loadout application.
- Movement-controller routing concept.
- Movement-only contract.
- Legacy mode fallback.
- Existing debug commands.

## What needs tuning instead of rewriting

- Melee ideal range.
- Retreat distance.
- Strafe pressure.
- Attack-charge thresholds.
- Shield/axe rules.
- Healing thresholds.
- Pearl-away thresholds.
- Special-move cooldowns.
- Trident range bands.
- Mace engage conditions.

## Recommended first 5 coding slices

1. Add/centralize `DuelTuning` constants without behavior changes.
2. Add duel debug metrics for spacing, charge, and punish windows.
3. Tune melee spacing and too-close retreat.
4. Tune sword/axe/shield fundamentals.
5. Tune defensive recovery and re-entry.

## Safe-to-tune first

- constants
- debug labels
- docs
- duel test plan
- small behavior thresholds
- narrow helper methods

## Needs runtime test

- whether bot can maintain melee range
- whether bot actually lands charged hits
- whether shield break logic works against a human
- whether healing creates openings or saves fights
- whether trident/mace commits are tactically useful
- whether movement brain routing matches expected branch families
