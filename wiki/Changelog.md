# Changelog

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

- Mixed movement training records per-family telemetry but updates
  `general_fallback`.
- Curriculum mode updates the configured `ai.training.curriculum-family` brain.
- Live evaluation metrics require an arena runner; the current evaluation export
  initializes and reports route/fallback/schema state.

## 5.1.1 — Combat Reliability + Movement Neural Network

See [Release Notes 5.1.1](Release-Notes-5.1.1) for full details.

### Added

**Combat Reliability (Issue #6)**
- Vanilla attack ordering fix — `Bot.attack()` calls vanilla attack before swing/punch to prevent charge reset.
- Charge-aware planning — CombatDirector waits for full attack charge before committing to attacks.
- Mace recharge planning with gravity-aware airtime tracking.
- Mace airborne tracking with ground clip tolerance and velocity-aware horizontal damping.
- Sweep instrumentation and telemetry (sweep-check, sweep-skip).
- Combat telemetry fields: `critPred`, `sweepPred`, `chargeAtVanillaAttack`, `chargeAfterVanillaAttack`, `targetHp`, `targetHpDelta`.
- `/bot combatdebug` command for per-bot combat trace logging.

**Movement Neural Network (Issue #7)**
- Movement-only neural network — controls footwork (strafing, spacing, sprint/jump timing, approach/retreat). Does **not** control combat.
- CombatDirector.plan() / execute() split with CombatIntent / MovementState coupling.
- 37-value MovementInput schema, 8-value MovementOutput schema.
- MovementOutputApplier with threshold-based movement decisions.
- In-JVM genetic algorithm: tournament selection, uniform crossover, adaptive Gaussian mutation, elite preservation.
- Configurable fitness scoring with 17 weighted factors.
- Training loadout mixing with weighted random pool.
- Brain persistence: `brain.json` with schema validation, safe fallback, auto-backup on reset.
- `/ai brain <status|load|save|reset>` commands.
- `/ai movement` command for spawning movement-controller bots.
- `/ai reinforcement` now accepts `movement-controller` (default) or `legacy` mode.

**Loadouts**
- New loadouts: `vanilla`, `axe`, `smp`, `pot`, `spear`.
- `/bot loadoutmix` command for distributing loadouts across bots.
- Loadout mix presets: `alltypes`, `core`, `problem`.

**Configuration**
- Full `ai.movement-network` config section (mode, shape, fallback, brain path, autosave, debug).
- Full `ai.training` config section (population, generations, GA parameters, adaptive mutation).
- `ai.training.fitness-weights` with 17 configurable factors.
- `ai.training.loadouts` with weighted pool configuration.

### Changed

- Training mode defaults to `movement-controller` (NN handles movement, Director handles combat) instead of legacy full-replacement.
- CombatDirector split into `plan()` and `execute()` phases for movement NN integration.

### Fixed

- Bots no longer waste swings on uncharged attacks (charge-aware planning).
- Vanilla attack damage calculation now runs before swing/punch animation (correct ordering).
- Mace smash no longer commits when airtime is insufficient for recharge.
- Mace airborne tracking no longer overshoots on moving targets.

### Known limitations

- Changing `hidden-layers` after training invalidates saved brains.
- Training with large populations impacts server TPS.
- Movement NN is disabled by default for normal bots.

---

## Unreleased (pre-5.1.1) — Combat + Inventory + Presets Overhaul

### Added

**Combat AI**
- Weapon-aware `CombatDirector` that picks the right behavior per tick based on inventory, distance, cooldowns, and dimension.
- Melee, Mace Smash, Trident Momentum Throw, Wind Charge, Ender Pearl, Crystal PvP, Anchor Bomb, Cobweb, Elytra Glide, Totem Swap, and Heal behaviors.
- Neural-network bots bypass the director to preserve deterministic fitness.

**Inventory**
- Full per-bot inventory: 9 hotbar + 27 storage + 4 armor + 1 offhand.
- `/bot inventory <name>` GUI editor.
- `/bot give`, `/bot armor`, `/bot weapons` commands.

**Loadouts**
- Built-in loadouts: `sword`, `mace`, `trident`, `windcharge`, `skydiver`, `hybrid`, `crystalpvp`, `anchorbomb`, `pvp`, `clear`.
- `/bot loadout <name> [bot]`.

**Presets**
- YAML preset system with full NBT round-trip.
- `/bot preset save`, `apply`, `list`, `delete`.

**Permissions**
- `terminatorplus.admin`, `terminatorplus.manage`, `terminatorplus.*`.

**API**
- `Terminator.combatTick(LivingEntity)`.
- `Terminator.getDimension()`.

### Changed
- Normal bots use vanilla damage (real crits, shields, enchantments).
- `/bot loadout` is per-bot when a name is passed.
- `/bot give` accepts `[bot] [slot]` for targeted placement.

### Fixed
- Hotbar slot sync when combat director switches weapons.
- Fall damage stacking on mace smash.

## Previous versions

Pre-overhaul. See Git history.
