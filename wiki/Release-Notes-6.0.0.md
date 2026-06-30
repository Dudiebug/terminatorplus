# TerminatorPlus 6.0.0 - Major PvP Bot Platform Update

TerminatorPlus 6.0.0 is a major update, not a small balance patch. It brings together the combat director rewrite, movement brain-bank work, player-like action timing, training metrics, loadout polish, safety cleanup, and documentation needed to make TerminatorPlus a serious 1v1 PvP bot platform on modern Paper.

The headline: bots now have a much stronger internal structure for deciding what they want, how they move, which action they are taking, and how that behavior is measured after the fight. This release lays the foundation for the next generation of TerminatorPlus training and evaluation.

## Compatibility

- Built for Paper 26.1.2.
- Requires Java 25.
- Spigot and CraftBukkit are not supported.
- Release jar: `TerminatorPlus-6.0.0-BETA-mc26.1.2.jar`.

## Huge Systems Added

- Added a focused 1v1 PvP project strategy for TerminatorPlus, replacing the old broad "general bot" framing with a duel-quality target.
- Added the authority split that moves combat, targeting, survival, movement, actions, training, and evaluation toward clear ownership boundaries.
- Added `CombatIntent` as the central combat-to-movement contract.
- Added movement objectives so bots can explicitly request pressure, retreat, orbiting, kiting, re-entry, recovery, or other tactical movement goals.
- Added combat action categories so the runtime can reason about melee, shield, consumable, projectile, explosive, utility, block, and recovery actions.
- Added movement branch families for routing brains by tactical style instead of treating every bot as one generic movement problem.
- Added a movement brain bank with per-family specialist brains.
- Added movement brain manifest loading, schema checks, fallback handling, and quarantine state.
- Added evaluation exports for fixed-seed movement-bank comparisons.
- Added a live evaluator metric schema with a metric catalog for duel, movement, safety, reward, and action-legality fields.
- Added a player-like action controller foundation so actions can take time, block conflicting combat, and be interrupted.
- Added action-legality counters for fake/direct shortcuts, instant consumes, same-tick primary-action violations, and interrupted actions.

## Combat and Duel Logic

- Reworked combat execution around planned actions instead of scattered direct behavior calls.
- Added richer `CombatIntent` state, including range, health, target state, hazards, preferred movement family, and action category.
- Added combat/movement coupling so footwork can be driven by what the bot is actually trying to do in the duel.
- Added planned-action telemetry to make combat behavior easier to inspect.
- Added expanded combat snapshots for training and debugging.
- Added stronger sword and axe timing around vanilla attack charge.
- Added shield-use tracking as a primary action.
- Added melee attack primary-action recording.
- Added combat gating while timed player-like actions are active.
- Added support for holding combat on `action-hold` instead of forcing an impossible same-tick action.
- Improved mace decision scaffolding around fall, timing, and target state.
- Improved trident throw instrumentation.
- Improved wind charge action tracking.
- Improved crystal PvP action tracking.
- Improved anchor-bomb action tracking.
- Improved ender pearl action tracking.
- Improved cobweb utility action tracking.
- Added direct-shortcut telemetry for advanced behaviors that still need full vanilla-style legality replacement.

## Player-Like Actions and Consumables

- Added `PlayerLikeActionController`.
- Added bot action states for idle, timed use, block use, mining, projectile, explosive, and interrupted action flows.
- Added per-tick action budgeting so a bot can only claim one primary player-like action per tick.
- Added timed consumable use for regular golden apples.
- Added timed consumable use for enchanted golden apples.
- Added timed drink-potion use for healing.
- Added timed drink-potion use for fire resistance.
- Added timed drink-potion use for strength.
- Added completion callbacks so timed actions apply their effects only after the use duration completes.
- Added interruption tracking for timed actions.
- Added action blocking so active consumable use prevents conflicting combat actions.
- Kept splash-potion and advanced utility actions working while marking them through direct-shortcut telemetry.

## Movement Brain Bank

- Added per-family movement brain routing.
- Added `general_fallback` movement brain behavior.
- Added support for importing legacy `ai/brain.json` into the movement bank.
- Added manifest-backed movement-brain persistence under `ai/movement/`.
- Added family-specific brain files for melee, mace, trident/ranged, spear/melee, mobility, explosive survival, projectile ranged, and fallback movement.
- Added movement-bank route metrics.
- Added fallback metrics so reports show when a specialist brain was missing or quarantined.
- Added movement-bank schema versioning.
- Added quarantine metrics for bad or incompatible movement brains.
- Added training autosave behavior for movement brains.
- Added save-only-improved safeguards so weaker brains do not overwrite stronger specialist brains.
- Added mixed-family training support that can update eligible specialist brains from one mixed population.
- Added projectile-ranged curriculum support.
- Added movement-balanced reward profile support.

## Movement and Footwork

- Added baseline movement policy extraction from combat intent.
- Added safer baseline retreat behavior when health is low.
- Added safer minimum/maximum range handling for movement decisions.
- Added movement objective fields to the movement input contract.
- Added movement branch-family fields to the movement input contract.
- Added richer movement telemetry for desired range, too-close time, too-far time, orbit stability, stuck ticks, baseline-only ticks, neural-network ticks, and safety overrides.
- Added route-family telemetry for training and evaluation.
- Improved movement fallback behavior when a specialist brain is unavailable.
- Preserved vertical velocity while steering, fixing mace-dive slow-fall behavior from earlier releases.

## Training and Evaluation

- Added report schema version 2 for movement evaluation exports.
- Added a metric catalog so every report explains what each live metric means.
- Added explicit `null` live metrics for report-only evaluation runs.
- Added damage-dealt metric fields.
- Added damage-taken metric fields.
- Added damage-delta and damage-ratio metric helpers.
- Added self-damage metrics.
- Added fall-damage metrics.
- Added explosive self-damage metrics.
- Added desired-range uptime metrics.
- Added retreat-success metrics.
- Added orbit-stability metrics.
- Added crit-setup conversion metrics.
- Added sprint-hit conversion metrics.
- Added shield-break success metrics.
- Added web-conversion metrics.
- Added pearl re-entry success metrics.
- Added action-legality metrics.
- Added fake-action count metrics.
- Added instant-consume count metrics.
- Added illegal same-tick action metrics.
- Added action-interruption metrics.
- Added round-limit configuration for reinforcement training through `ai.training.max-round-minutes`.
- Kept command override support for one-session reinforcement round duration.
- Added clearer report-only behavior so missing arena metrics are visible and intentional.

## Loadouts, Inventory, and Presets

- Preserved full hotbar, storage, armor, and offhand inventory support.
- Preserved inventory GUI editing.
- Preserved `/bot give`, `/bot armor`, and `/bot weapons` workflows.
- Preserved YAML preset support with NBT round-trip behavior.
- Preserved loadout mix presets for broad training coverage.
- Preserved newer PvP loadout families used by movement training.
- Kept direct NMS inventory writes for bot hotbar/storage updates so Paper 26 container rollback does not undo bot inventory changes.

## Commands and Debugging

- Preserved `/bot combatdebug` for combat trace logging.
- Preserved `/ai brain` movement-bank management commands.
- Preserved `/ai movement` bot spawning for movement-controller bots.
- Preserved `/ai reinforcement` training entry points.
- Preserved `/ai evaluate` movement-bank report exports.
- Added documentation for how the bot architecture works.
- Added documentation for the release workflow and review checklist.
- Added a duel test plan for validating real PvP behavior.
- Added a Phase D/E review document covering training, evaluation, command surface cleanup, and integration risks.

## Safety and Cleanup

- Added clearer boundaries between legacy paths and newer duel-controller paths.
- Added telemetry for remaining direct shortcuts instead of hiding them.
- Added action-controller counters to combat training snapshots.
- Added report fields for action legality and interruption.
- Kept NMS entity-data fallback behavior needed across modern Paper variants.
- Kept Paper 26 skin-customization slot handling based on Mojang constants instead of hardcoded indices.
- Kept MockConnection and lifecycle cleanup work from the 5.2.x line.
- Kept skin-cache cleanup work from the 5.2.x line.
- Updated installation, API, troubleshooting, changelog, and release documentation to point at the 6.0.x line.

## Important Fixes Included From the 5.2.x Line

- Added configurable reinforcement round limits.
- Fixed mace dive vertical velocity handling.
- Fixed movement-training specialist brain replacement so only matching route samples can replace that specialist family.
- Added mixed-family movement training that ranks and saves eligible specialist brains by family.
- Made movement training autosave the default path.
- Made `/ai reinforcement <population-size> <name> [skin]` default to movement-controller training.
- Added movement-bank evaluation support.
- Added manifest and persistence for movement brains.
- Added legacy brain import as `general_fallback`.
- Added weighted movement-training loadout assignment.
- Added per-family reward profiles and rollout metrics.
- Added movement-only source guardrails.

## Known Limitations

- This release is Paper 26.1.2 only.
- Some live evaluator fields are intentionally `null` in report-only exports until a live arena runner supplies them.
- Vanilla mining/block breaking is not fully replaced yet.
- Full legal block, projectile, and explosive action replacement is still follow-up work; the new telemetry makes remaining shortcuts measurable.
- Runtime PvP arena testing is still required before every metric can be treated as gameplay-calibrated.

## Why This Is a Major Update

6.0.0 is the release where TerminatorPlus stops being just a strong scripted combat bot and becomes a structured training and evaluation platform for player-like PvP bots. The combat layer, movement layer, action timing layer, and metrics layer now have the contracts needed to keep improving without guessing what the bot did or why it did it.
