# Changelog

> Legacy/reference notice:
> This page may describe the old general TerminatorPlus strategy.
> Current strategy is 1v1 PvP bot quality on `mc-26.1.2`.
> Use this page for technical reference only until it is verified against source code and runtime behavior.

## 6.0.0 - Duel Core V2, Player-Like Actions, Movement Brain Bank, and Evaluation Overhaul

This is the largest TerminatorPlus update in the modern Paper 26 line. Version
6.0.0 turns the project from a collection of strong combat behaviors into a
structured 1v1 PvP bot platform with a cleaner duel brain, player-like action
timing, movement-brain routing, richer training telemetry, and release-grade
documentation for continuing the work safely.

See [Release Notes 6.0.0](Release-Notes-6.0.0) for the full patch notes.

### Added

- Combat runtime architecture for focused 1v1 PvP bot quality on Paper 26.1.2.
- `CombatIntent`, movement objective, action category, and movement-branch routing data that let combat planning and movement selection share one richer state model.
- Movement brain-bank support with per-family brains, schema validation, manifest state, quarantine behavior, route telemetry, and fixed-seed evaluation exports.
- Player-like action controller foundation with timed consumable use, interruption handling, combat gating, and legality counters for fake actions, instant consumes, and same-tick action violations.
- Live evaluator metric schema v2 with a metric catalog and first-class duel, movement, safety, and action-legality fields.
- Major documentation pass covering the project vision, workflow, review checklist, duel test plan, phase review, installation notes, and API/troubleshooting version references.

### Changed

- Updated the plugin build version to `6.0.0-BETA-mc26.1.2`.
- Consumables now route apples and drink potions through timed use instead of instant effect application.
- Combat execution now respects primary-action budgeting and can hold combat while timed player-like actions are in progress.
- Training and evaluation outputs now expose report-only nulls explicitly so missing live arena metrics are visible instead of ambiguous.

### Notes

- This release targets Paper 26.1.2 and Java 25.
- Some advanced legal-action systems, including vanilla mining/block breaking and full block/projectile/explosive legality replacement, are still tracked as follow-up work; 6.0.0 adds telemetry and guardrails so those shortcuts are visible.
- Runtime PvP arena testing is still required before treating every new metric as gameplay-calibrated.

## 5.2.5 - Reinforcement Round Limit Config

### Added

- `ai.training.max-round-minutes`, defaulting to `1`, controls the per-generation `/ai reinforcement` round cap. The command's `[round-minutes]` argument still overrides the config value for one session.

## 5.2.4 - Mace Fall Velocity Fix

### Fixed

- Bot walking now caps only horizontal movement and preserves vertical velocity, so mace dive bots no longer slow-fall while the movement controller is steering them midair.

## 5.2.3 - Training Sample Guard

### Fixed

- Mixed movement training now only ranks and saves a specialist family brain from bots that actually produced route samples for that same family, preventing unrelated aggregate survival fitness from replacing a specialist brain.

## 5.2.2 - Mixed Family Training

### Changed

- Mixed `/ai reinforcement` now samples the configured loadout mix, seeds each candidate from its assigned loadout family, ranks results per family, and updates every eligible specialist brain from one round.
- Movement training autosave is enabled by default, while `save-only-improved-brain` still prevents worse brains from replacing better ones.

## 5.2.1 - Movement Training Default

### Changed

- `/ai reinforcement <population-size> <name> [skin]` now defaults to the movement-controller trainer.
- `legacy` remains available as an explicit mode for the old full-replacement training pipeline.
- Movement-training option strings such as `family=mace:mix=mace_curriculum` are parsed as movement options even without a leading `movement:` prefix.

## 5.2.0 - Movement Brain Bank Evaluation Support

### Added

- Movement brain bank routing by `MovementBranchFamily`.
- Manifest plus per-brain persistence under `ai/movement/`.
- Legacy `ai/brain.json` import as `general_fallback`.
- Automatic weighted movement-training loadout assignment.
- Per-family reward profiles and rollout metrics.
- `/ai evaluate` report export for fixed-seed movement-bank ablations.
- `checkMovementOnlyContract` Gradle guardrail for movement-only source code.

### Notes

- Initial mixed movement training recorded per-family telemetry but updated `general_fallback`; this was superseded by 5.2.2, which trains eligible specialist families from the mixed loadout population.
- Curriculum mode updates the configured `ai.training.curriculum-family` brain.
- Live evaluation metrics require an arena runner; the current evaluation export initializes and reports route/fallback/schema state.

## 5.1.1 - Combat Reliability + Movement Neural Network

See [Release Notes 5.1.1](Release-Notes-5.1.1) for full details.

### Added

Combat reliability:

- Vanilla attack ordering fix: `Bot.attack()` calls vanilla attack before swing/punch to prevent charge reset.
- Charge-aware planning: CombatDirector waits for full attack charge before committing to attacks.
- Mace recharge planning with gravity-aware airtime tracking.
- Mace airborne tracking with ground clip tolerance and velocity-aware horizontal damping.
- Sweep instrumentation and telemetry.
- Combat telemetry fields: `critPred`, `sweepPred`, `chargeAtVanillaAttack`, `chargeAfterVanillaAttack`, `targetHp`, `targetHpDelta`.
- `/bot combatdebug` command for per-bot combat trace logging.

Movement neural network:

- Movement-only neural network controlling footwork. It does **not** control combat.
- `CombatDirector.plan()` / `execute()` split with `CombatIntent` / `MovementState` coupling.
- 37-value `MovementInput` schema and 8-value `MovementOutput` schema.
- MovementOutputApplier with threshold-based movement decisions.
- In-JVM genetic algorithm with tournament selection, uniform crossover, adaptive Gaussian mutation, and elite preservation.
- Brain persistence with schema validation, safe fallback, and backups.
- `/ai brain <status|load|save|reset>` commands.
- `/ai movement` command for spawning movement-controller bots.
- `/ai reinforcement` accepts movement-controller or `legacy` mode.

Loadouts:

- New loadouts: `vanilla`, `axe`, `smp`, `pot`, `spear`.
- `/bot loadoutmix` command for distributing loadouts across live bots.
- Loadout mix presets: `alltypes`, `core`, `problem`.

### Fixed

- Bots no longer waste swings on uncharged attacks.
- Vanilla attack damage calculation now runs before swing/punch animation.
- Mace smash no longer commits when airtime is insufficient for recharge.
- Mace airborne tracking no longer overshoots on moving targets.

### Known limitations

- Changing the movement layer shape after training invalidates saved brains.
- Training with large populations impacts server TPS.
- Movement-bank bots must be spawned with `/ai movement`; normal `/bot create` bots use the legacy movement path.

## Pre-5.1.1 - Combat + Inventory + Presets Overhaul

### Added

- Weapon-aware `CombatDirector` that picks behavior by inventory, distance, cooldown, and dimension.
- Melee, Mace Smash, Trident Momentum Throw, Wind Charge, Ender Pearl, Crystal PvP, Anchor Bomb, Cobweb, Elytra Glide, Totem Swap, and Heal behaviors.
- Full per-bot inventory: 9 hotbar + 27 storage + 4 armor + 1 offhand.
- `/bot inventory <name>` GUI editor.
- `/bot give`, `/bot armor`, `/bot weapons` commands.
- Built-in loadouts: `sword`, `mace`, `trident`, `windcharge`, `skydiver`, `hybrid`, `crystalpvp`, `anchorbomb`, `pvp`, `clear`.
- YAML preset system with full NBT round-trip.
- Permissions: `terminatorplus.admin`, `terminatorplus.manage`, `terminatorplus.*`.
- API calls: `Terminator.combatTick(LivingEntity)` and `Terminator.getDimension()`.

### Changed

- Normal bots use vanilla damage, including real crits, shields, and enchantments.
- `/bot loadout` is per-bot when a name is passed.
- `/bot give` accepts `[bot] [slot]` for targeted placement.

### Fixed

- Hotbar slot sync when CombatDirector switches weapons.
- Fall damage stacking on mace smash.

## Previous versions

Pre-overhaul. See Git history.
