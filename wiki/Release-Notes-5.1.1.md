# Release Notes - 5.1.1

This is the historical release note for the 5.1.1 combat reliability and first movement-network release. For the current 5.2.x movement brain-bank behavior, see [Movement Brain Bank](Movement-Brain-Bank), [AI Training](AI-Training), and [Configuration](Configuration).

## Combat Reliability

### Vanilla Attack Ordering Fix

`Bot.attack()` calls `getBukkitEntity().attack(entity)` before punch/swing animations. Previously, swing/punch could reset the attack charge before the damage calculation, causing ghost hits where the animation plays but no damage lands.

### Charge-Aware Planning

The CombatDirector checks `BotCombatTiming.chargeReady()` before committing to attack branches. Bots wait for full attack charge before swinging, preventing low-damage spam and swing-block cycling.

### Mace Recharge Planning

The mace smash branch accounts for custom gravity and required airtime. The Director does not commit to a mace smash unless the bot can realistically complete the jump-fall cycle and reach smash-ready charge.

### Mace Airborne Tracking

When mid-mace-fall, tracking uses:

- Ground clip tolerance to avoid premature landing detection.
- Velocity-aware horizontal damping to stay centered on moving targets without overshooting.

### Sweep Instrumentation

Sweep-check and sweep-skip telemetry were added to the combat trace system.

### Combat Telemetry

Debug fields include `critPred`, `sweepPred`, `chargeAtVanillaAttack`, `chargeAfterVanillaAttack`, `targetHp`, and `targetHpDelta`. Enable with `/bot combatdebug`.

## Movement Neural Network Overhaul

### Movement-Only Network

The movement-controller neural network handles footwork: strafing, spacing, sprint/jump timing, approach/retreat, facing adjustment, and hold-position cooperation. **It does not control combat.** CombatDirector retains full authority over weapon selection, attack timing, and execution.

### CombatIntent / MovementState Coupling

- `CombatDirector.plan()` produces a CombatIntent.
- The NN receives CombatIntent as part of its input.
- After movement, MovementOutputApplier updates MovementState.
- `CombatDirector.execute()` reads MovementState to validate timing windows before attacking.

### Network Architecture

- 37-value input in current 5.2.x builds.
- 8-value output: forward, strafe, jump, sprint, retreat, facing, urgency, hold.
- Configurable hidden layers.
- `tanh` activation.

### In-JVM GA Training

- Tournament selection, uniform crossover, Gaussian mutation, and elite preservation.
- Configurable population.
- All training runs in the JVM; no Python, ONNX, GPU, or external tooling.

### Loadout Mixing

The first version used a weighted loadout pool. In 5.2.x this became the movement brain-bank loadout mix under `ai.training.loadout-mix` and `ai.training.loadout-mixes`, with mixed rounds training eligible specialist families.

### Brain Persistence

The first version used a single `brain.json`. In 5.2.x this became the manifest plus per-family bank under `ai/movement/`. Legacy compatible `ai/brain.json` files import as `general_fallback`.

### Commands Introduced

- `/ai brain <status|load|save|reset>` manages movement brains.
- `/ai movement <amount> <name>` spawns bots using the saved movement bank.
- `/ai reinforcement ... [movement-controller|legacy]` chooses training mode. In 5.2.x, movement-controller is the default when mode is omitted.
- `/bot loadoutmix <mix>` distributes loadouts across live spawned bots.
- `/bot combatdebug <name|all> <on|off>` toggles combat trace logging.

## New Loadouts

Added: `vanilla`, `axe`, `smp`, `pot`, `spear`.

See [Loadouts](Loadouts) for details.

## Configuration Notes

The 5.2.x config uses:

- `ai.movement` for movement-bank runtime settings.
- `ai.movement.bank` for manifest/per-brain persistence settings.
- `ai.training.max-round-minutes` for the reinforcement round cap.
- `ai.training.loadout-mix` and `ai.training.loadout-mixes` for weighted training loadout selection.
- `ai.training.curriculum-family` for focused specialist training.

See [Configuration](Configuration) for the current full reference.

## Compatibility

- Current 5.2.x release jars target Paper 26.1.2 only.
- Java 25.
- Brain files from earlier versions may be incompatible. Use `/ai brain reset` to start fresh if validation fails.

## Known Cautions

- Training spawns many bots fighting simultaneously. Monitor server TPS and reduce population if needed.
- Changing movement layer shape after training invalidates saved brains. Reset and retrain.
- `/bot create` bots do not use the movement bank. Use `/ai movement` to spawn bots with the trained movement bank.
