# Configuration

All settings live in `plugins/TerminatorPlus/config.yml`, generated on first startup. The file is organized under the `ai` section.

## Movement Network

```yaml
ai:
  movement-network:
    enabled: false
    mode: movement-controller
    tick-rate: 1
    input-size: 30
    hidden-layers: [32, 24]
    output-size: 8
    fallback-to-legacy-movement: true
    legacy-full-replacement-mode: false
    movement-output-scaling: 1.0
    hold-position-behavior: true
    brain-path: ai/brain.json
    autosave-best-brain: true
    save-only-improved-brain: true
    debug: false
```

| Key | Type | Default | Description |
| --- | --- | --- | --- |
| `enabled` | bool | `false` | Gates whether bots that **already have a network** (spawned via `/ai movement` or `/ai reinforcement`) actually run the network. When `false`, those bots fall back to legacy movement. **Does not affect `/bot create` bots** — those always spawn without a network. To deploy a trained brain, use `/ai movement`. |
| `mode` | string | `movement-controller` | NN mode. `movement-controller` = movement only; Director handles combat. |
| `tick-rate` | int (1--20) | `1` | Evaluate the network every N ticks. Higher values reduce CPU cost but lower movement responsiveness. |
| `input-size` | int | `30` | Number of input values. Must match MovementInput schema (30). |
| `hidden-layers` | int list | `[32, 24]` | Hidden layer sizes. Changing this requires retraining. |
| `output-size` | int | `8` | Number of output values. Must match MovementOutput schema (8). |
| `fallback-to-legacy-movement` | bool | `true` | When NN fails (invalid input, shape mismatch, etc.), fall back to legacy movement instead of stopping. |
| `legacy-full-replacement-mode` | bool | `false` | Use the legacy full-replacement NN instead of movement-controller. |
| `movement-output-scaling` | float (0.1--4.0) | `1.0` | Scale all movement outputs. Values > 1 make movement more aggressive. |
| `hold-position-behavior` | bool | `true` | Respect CombatDirector's hold-position requests. |
| `brain-path` | string | `ai/brain.json` | Path to the brain file, relative to the plugin data folder. |
| `autosave-best-brain` | bool | `true` | Automatically save the best brain after each training generation. |
| `save-only-improved-brain` | bool | `true` | Only autosave when the new best fitness exceeds the previous best. |
| `debug` | bool | `false` | Log movement decisions to console. |

## Training

```yaml
ai:
  training:
    population-size: 120
    generations: 0
    tournament-size: 5
    elite-count: 6
    crossover-rate: 1.0
    mutation-rate: 0.035
    mutation-strength: 0.12
    adaptive-mutation: true
    random-seed: 0
    max-training-ticks: 0
```

| Key | Type | Default | Description |
| --- | --- | --- | --- |
| `population-size` | int (10--240) | `120` | Bots per generation. |
| `generations` | int | `0` | Max generations before auto-stop. 0 = unlimited. |
| `tournament-size` | int (2--32) | `5` | Number of candidates per tournament selection. |
| `elite-count` | int (0--64) | `6` | Top networks carried forward unchanged each generation. |
| `crossover-rate` | float (0--1) | `1.0` | Probability of crossover per mating pair. |
| `mutation-rate` | float (0--1) | `0.035` | Probability of mutating each weight/bias. |
| `mutation-strength` | float (0--2) | `0.12` | Standard deviation of Gaussian mutation noise. |
| `adaptive-mutation` | bool | `true` | Cool down mutation rate and strength over generations. |
| `random-seed` | long | `0` | RNG seed. 0 = derived from session start time. |
| `max-training-ticks` | int | `0` | Max ticks per generation. 0 = no limit. |

## Fitness Weights

```yaml
ai:
  training:
    fitness-weights:
      range-control: 300.0
      range-urgency: 220.0
      crit-setup: 75.0
      sprint-hit: 45.0
      hold-position: 35.0
      strafing-circling: 90.0   # legacy alias, currently unused by the runtime
      circling: 90.0
      retreat: 70.0
      survival: 0.25
      damage-dealt: 275.0
      damage-taken-penalty: 4.0
      stuck-penalty: 45.0
      fallback-penalty: 35.0
      invalid-output-penalty: 35.0
      oscillation-penalty: 12.0
      jump-spam-penalty: 180.0
      sprint-spam-penalty: 120.0
      hold-violation-penalty: 85.0
```

| Key | Range | Default | Description |
| --- | --- | --- | --- |
| `range-control` | 0--2000 | 300 | Reward for maintaining distance close to desired range |
| `range-urgency` | 0--2000 | 220 | Reward for closing/opening range when urgency is high |
| `crit-setup` | 0--1000 | 75 | Reward for being in a crit-fall position when the Director requests it |
| `sprint-hit` | 0--1000 | 45 | Reward for sprinting when the Director requests a sprint-hit |
| `hold-position` | 0--1000 | 35 | Reward for holding position when requested |
| `strafing-circling` | 0--1000 | 90 | Reward for lateral/circular movement at range |
| `circling` | 0--1000 | 90 | Reward for circling behavior |
| `retreat` | 0--1000 | 70 | Reward for retreating when appropriate |
| `survival` | 0--20 | 0.25 | Bonus per alive tick |
| `damage-dealt` | 0--2000 | 275 | Reward for kills (kills * weight) |
| `damage-taken-penalty` | 0--500 | 4 | Penalty for damage deficit |
| `stuck-penalty` | 0--1000 | 45 | Penalty for not moving |
| `fallback-penalty` | 0--1000 | 35 | Penalty when NN falls back to legacy movement |
| `invalid-output-penalty` | 0--1000 | 35 | Penalty for invalid network outputs |
| `oscillation-penalty` | 0--500 | 12 | Penalty for rapid direction changes |
| `jump-spam-penalty` | 0--1000 | 180 | Penalty for excessive jumping |
| `sprint-spam-penalty` | 0--1000 | 120 | Penalty for excessive sprinting |
| `hold-violation-penalty` | 0--1000 | 85 | Penalty for moving when hold-position is requested |

## Training Loadouts

```yaml
ai:
  training:
    loadouts:
      pool:
        - sword
        - mace
        - trident
        - windcharge
        - skydiver
        - hybrid
        - crystalpvp
        - anchorbomb
        - pvp
        - axe
        - smp
        - pot
        - spear
      weights:
        sword: 1
        mace: 1
        trident: 1
        windcharge: 1
        skydiver: 1
        hybrid: 1
        crystalpvp: 1
        anchorbomb: 1
        pvp: 1
        axe: 1
        smp: 1
        pot: 1
        spear: 1
```

Each generation, bots are randomly assigned loadouts from the pool weighted by their integer weights. Higher weight = more likely to be selected. This helps the NN generalize across weapon types rather than overfitting to one loadout's movement patterns.

Remove a loadout from the `pool` list or set its weight to 0 to exclude it from training.
