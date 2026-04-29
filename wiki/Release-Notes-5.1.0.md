# Release Notes — 5.1.0

## Combat Reliability (Issue #6)

### Vanilla attack ordering fix
`Bot.attack()` now calls `getBukkitEntity().attack(entity)` before punch/swing animations. Previously, swing/punch could reset the attack charge before the damage calculation, causing ghost hits where the animation plays but no damage lands.

### Charge-aware planning
The CombatDirector now checks `BotCombatTiming.chargeReady()` before committing to attack branches. Bots wait for full attack charge (0.95) before swinging, preventing low-damage spam and swing-block cycling.

### Mace recharge planning
The mace smash branch accounts for custom gravity and required airtime. The Director won't commit to a mace smash unless the bot can realistically complete the jump-fall cycle and reach smash-ready charge (0.848).

### Mace airborne tracking
When mid-mace-fall, tracking uses:
- Ground clip tolerance to avoid premature landing detection.
- Velocity-aware horizontal damping to stay centered on moving targets without overshooting.

### Sweep instrumentation
Added sweep-check and sweep-skip telemetry to the combat trace system.

### Combat telemetry
Preserved and documented debug fields: `critPred`, `sweepPred`, `chargeAtVanillaAttack`, `chargeAfterVanillaAttack`, `targetHp`, `targetHpDelta`. Enable with `/bot combatdebug`.

---

## Movement Neural Network Overhaul (Issue #7)

### Movement-only network
The new movement-controller neural network handles footwork — strafing, spacing, sprint/jump timing, approach/retreat, facing adjustment, and hold-position cooperation. **It does not control combat.** The CombatDirector retains full authority over weapon selection, attack timing, and execution.

### CombatIntent / MovementState coupling
- **CombatDirector.plan()** produces a CombatIntent (desired range, urgency, crit/sprint/hold hints).
- The NN receives CombatIntent as part of its 30-value input.
- After movement, MovementOutputApplier updates MovementState.
- **CombatDirector.execute()** reads MovementState to validate timing windows before attacking.

### Network architecture
- 30-value input (position, velocity, facing, combat intent, weapon info)
- Configurable hidden layers (default: [32, 24])
- 8-value output (forward, strafe, jump, sprint, retreat, facing, urgency, hold)
- tanh activation

### In-JVM GA training
- Tournament selection (size 5), uniform crossover, Gaussian mutation with adaptive cooling.
- Elite preservation (top 6 networks per generation).
- Configurable population (10--240, default 120).
- All training runs in the JVM — no Python, ONNX, GPU, or external tooling.

### Loadout mixing
Each training generation draws from a weighted loadout pool (all 13 combat loadouts by default). This generalizes movement learning across weapon types.

### Fitness scoring
17 weighted fitness factors (configurable): range-control, range-urgency, crit-setup, sprint-hit, hold-position, circling, retreat, survival, damage-dealt, plus penalties for stuck movement, oscillation, jump/sprint spam, fallback activation, and hold violations.

### Brain persistence
- Trained networks save to `brain.json` with schema version, shape, weights, biases, and training metadata.
- Commands: `/ai brain status`, `/ai brain save`, `/ai brain load`, `/ai brain reset`.
- Shape and schema validation on load. Corrupt or incompatible files trigger safe fallback — no crash.
- Auto-backup on reset.

### New commands
- `/ai brain <status|load|save|reset>` — manage persisted movement brains.
- `/ai movement <amount> <name>` — spawn bots using the saved movement brain.
- `/ai reinforcement ... [movement-controller|legacy]` — choose training mode (movement-controller is default).
- `/bot loadoutmix <mix>` — distribute loadouts across bots.
- `/bot combatdebug <name|all> <on|off>` — toggle combat trace logging.

### Legacy compatibility
- Legacy full-replacement NN mode remains available via `/ai reinforcement ... legacy`.
- Non-NN bots (spawned via `/bot create`) are unaffected.
- Legacy movement fallback is enabled by default.

---

## New loadouts

Added: `vanilla`, `axe`, `smp`, `pot`, `spear`.

See [Loadouts](Loadouts) for details.

---

## Configuration additions

New config sections under `ai`:
- `movement-network` — network shape, mode, fallback, brain path, autosave, debug.
- `training` — population, generations, GA parameters, adaptive mutation.
- `training.fitness-weights` — 17 weighted fitness factors.
- `training.loadouts` — weighted loadout pool for training.

See [Configuration](Configuration) for the full reference.

---

## Compatibility

- Built against Paper 26.1.2 and Paper 1.21.11.
- Java 25.
- Brain files from earlier versions are not compatible (different schema). Use `/ai brain reset` to start fresh.

## Known cautions

- Training spawns many bots fighting simultaneously. Monitor server TPS and reduce population if needed.
- Changing `hidden-layers` after training invalidates saved brains. Reset and retrain.
- The movement NN is disabled by default for normal bots. Enable via `ai.movement-network.enabled: true` in config.
