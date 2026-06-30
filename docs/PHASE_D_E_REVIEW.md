# Phase D/E Metrics and Integration Review

Date: 2026-06-30

## Scope

This document covers the Phase D/E handoff block:

- Phase D: live evaluator and reinforcement metrics
- Phase E: docs/archive/command surface cleanup and final integration review

No runtime code is deleted by this phase. Legacy systems remain protected until
there is build proof, runtime duel proof, and a rollback plan.

## Metrics Status

| Metric | Current source | Report status |
|---|---|---|
| `damageDealt` | `CombatTrainingSnapshot.damageDealt` from `Bot` damage accounting | Collected in training snapshots; null in report-only `/ai evaluate` exports |
| `damageTaken` | `CombatTrainingSnapshot.damageTaken` from `Bot` damage accounting | Collected in training snapshots; null in report-only `/ai evaluate` exports |
| `damageDelta` | `CombatTrainingSnapshot.damageDelta()` | Derived in training snapshots; null in report-only exports |
| `damageRatio` | `CombatTrainingSnapshot.damageRatio()` | Derived in training snapshots; null in report-only exports |
| per-weapon damage buckets | `CombatTrainingSnapshot` sword/axe/mace/trident/spear/projectile/explosive fields | Collected in training snapshots |
| `fakeActionCount` | `PlayerLikeActionController.directShortcutCount()` | Collected in bot snapshot; null in report-only exports |
| `instantConsumeCount` | `PlayerLikeActionController.instantConsumeShortcutCount()` | Collected in bot snapshot; null in report-only exports |
| `illegalSameTickActionCount` | `PlayerLikeActionController.sameTickActionViolations()` | Collected in bot snapshot; null in report-only exports |
| `actionInterruptionCount` | `PlayerLikeActionController.interruptionCount()` | Collected in bot snapshot; null in report-only exports |
| `fallbackRate` | `MovementEvaluationHarness` route probes | Collected in report-only exports |
| `routeThrashRate` | `MovementEvaluationHarness` active-family switches per sample | Collected in report-only exports |
| live arena metrics | arena runner not implemented | Explicitly null in report-only exports |

Report-only `/ai evaluate` exports now include a `metricCatalog` explaining
which metrics are genuinely collected by the report and which require a live
duel arena. Null live metrics mean "not collected", not zero.

## Command Surface Classification

| Command surface | Bucket | Notes |
|---|---|---|
| `/bot create`, `/bot loadout`, `/bot inventory`, `/bot preset`, `/bot weapons` | CURRENT_RUNTIME | Main duel setup and inspection commands |
| `/bot combatdebug` | DEBUG_ONLY | Enables combat/movement/action telemetry |
| `/bot reset`, `/bot gather`, `/bot settings`, `/bot give`, `/bot armor`, `/bot loadoutmix` | ADMIN_COMMAND | Useful, but broad or mutating; not proof of current 1v1 strategy |
| `/ai movement`, `/ai reinforcement`, `/ai brain`, `/ai evaluate`, `/ai stop`, `/ai info` | TRAINING_ONLY | Movement brain training, persistence, and report export |
| `/ai random` | LEGACY_COMPATIBILITY | Old random-network workflow; preserve until explicitly removed |
| `/botenvironment ...` | LEGACY_COMPATIBILITY / ADMIN_COMMAND | Mutates legacy material and mob taxonomy |
| `/terminatorplus debuginfo` | DEBUG_ONLY | Support/reporting utility |

## Source Classification

| Area | Bucket | Review note |
|---|---|---|
| `CombatDirector` | CURRENT_RUNTIME | Owns combat planning/execution; keep authority here |
| `MovementInput`, `MovementOutput`, `MovementOutputApplier`, `MovementBaselinePolicy`, `MovementOutputMixer` | CURRENT_RUNTIME | Movement-only path; must not execute combat or items |
| `PlayerLikeActionController`, `BotActionState` | MODERN_DUEL_CORE | Central action-state foundation; still partial |
| `CombatTrainingSnapshot`, `MovementTrainingSnapshot`, `MovementRewardProfile` | TRAINING_ONLY | Snapshot/reward boundary for reinforcement |
| `MovementEvaluationHarness` | TRAINING_ONLY | Report-only export; no live arena execution yet |
| `LegacyAgent`, `LegacyBlockCheck`, `LegacyMats` | LEGACY_COMPATIBILITY / LEGACY_FALLBACK | Still active/protected; do not delete |
| `Bot`, `BotInventory`, `MockConnection`, NMS helpers | HIGH_RISK_NMS / DO_NOT_TOUCH_WITHOUT_TESTS | Fake-player runtime boundary |
| `OpportunityScanner`, crystals, anchors, wind, pearls, utility placements | CURRENT_RUNTIME with ARCHIVE_CANDIDATE advanced-tool risk | Keep as controlled advanced tools; do not use as proof fundamentals are solved |

## Boundary Review

| Area | Finding | Status |
|---|---|---|
| Combat authority | `CombatDirector` still owns action choice; movement changes do not call combat behavior internals | Safe |
| Movement-only contract | Baseline and mixer emit `MovementOutput` only | Safe |
| Action controller | Central state exists and blocks timed consume/drink actions from stacking with attacks | Partial but aligned |
| Consumables | Apples and drinkable potions are timed; splash self-heal/offense now reserve a short throw action and no longer apply duplicate instant self-heal | Improved in 6.1.0; needs runtime test |
| Mining/block breaking | Legacy mining crack progress now uses a small block/tool speed model for common obstacle blocks | Improved in 6.1.0; needs runtime test |
| Fall/clutch/mace damage | Mace-in-hotbar fall immunity is not present; environment fall blocking now records fall-clutch telemetry | Improved in 6.1.0; needs runtime tests |
| Block/projectile/explosive actions | Cobweb, pearl, splash, and trident now reserve action-controller phases before direct release; wind/crystal/anchor remain shortcut telemetry | Partial |
| Evaluation | Report-only exports are honest about null live metrics; runtime fights now have `LiveDuelMetricsRecorder` payloads | Improved |
| Docs/commands | Current vs training/debug/admin/legacy surfaces are classified here | Improved |

## Remaining Direct Shortcut Table

| Path | Current state | Next migration |
|---|---|---|
| Splash self-heal | Short action phase, projectile release, no duplicate immediate heal | Runtime-test impact timing |
| Splash offense | Short action phase, projectile release | Runtime-test aim and consume-once behavior |
| Cobweb placement | Action-controller placement window before `setType` release | Runtime-test scanner and utility paths |
| Ender pearl | Selected item plus short release action, then direct projectile spawn telemetry | Runtime-test teleport and weapon restore |
| Trident | Charge is action-state-bound; direct spawn counted on release | Runtime-test charge/release and hit accounting |
| Wind charge | Windup exists for boost, spawn remains direct with telemetry | Convert to projectile use action |
| Crystal | Direct host block, crystal spawn, explosion | Split into host place, crystal place, detonate sequence |
| Anchor | Direct anchor set, charge consume, explosion | Split into place, charge, use/detonate sequence |
| Legacy clutch/block helpers | Direct block mutation remains in legacy path | Convert or compatibility-gate after runtime proof |

## Recommended Fix List

Must fix before merge:

- Runtime-test timed apple/drink use in a duel and confirm weapon restore happens only after completion.
- Confirm action-controller same-tick blocking does not suppress committed mace/trident phases incorrectly.

Should fix soon:

- Runtime-test splash self-heal/offense to confirm release timing and no duplicate healing.
- Runtime-test live collection for `damageDealt`, `damageTaken`, desired-range ticks, fallback rate, fake action count, and same-tick action count.
- Continue migrating wind/crystal/anchor after cobweb/pearl/trident runtime proof.

Safe follow-up:

- Add command help text that labels `/ai evaluate` as report-only unless an arena runner is selected.
- Extend `MovementOutputMixer` counters into live metrics.
- Add docs links from wiki training pages to this metric catalog.

Do not touch without runtime tests:

- `LegacyAgent` deletion or broad rewrite.
- `Bot`, `BotInventory`, `MockConnection`, NMS fallback chains.
- Fall damage physics, clutch behavior, mace damage immunity.
- Crystal/anchor full rewrite.

## Validation Commands

Source/build changes in this phase require:

```bash
./gradlew build -q
```

Runtime validation still needed:

```text
/bot create DuelBot
/bot loadout sword DuelBot
/bot combatdebug DuelBot on
/ai movement 1 DuelBot
```

Then verify:

- timed gapple/drink starts, waits, completes, and restores weapon;
- no consume plus attack in the same tick for converted paths;
- `/ai evaluate branch_family_latched all 1337` exports JSON with `metricCatalog`;
- live metrics are null in report-only output, not zero.
