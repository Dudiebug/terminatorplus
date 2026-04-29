# AI Training

TerminatorPlus includes a fully in-JVM genetic algorithm training pipeline for evolving bot movement neural networks. No Python, ONNX, GPU, or external tooling is required.

## Starting a training session

```
/ai reinforcement <population-size> <name> [skin] [mode]
```

- **`population-size`** — number of bots per generation (default: 120, range: 10--240).
- **`name`** — display name and skin lookup for training bots.
- **`mode`** — `movement-controller` (default) or `legacy`.

### Movement-controller mode (default)

The neural network controls **movement only**. The CombatDirector still handles all combat: weapon selection, attack timing, charge gating, and execution.

This is the recommended mode. Bots learn footwork (strafing, spacing, sprint/jump timing, approach/retreat) while the Director provides consistent combat behavior. Fitness rewards cooperation between the NN's movement and the Director's combat intent.

### Legacy mode

```
/ai reinforcement 120 TrainBot legacy
```

The original training pipeline. The NN fully replaces both movement and combat. This mode uses the legacy deterministic damage table rather than vanilla attack mechanics, preserving comparable fitness scores from earlier development.

## Managing a session

```
/ai stop              # end the current session
/ai info <bot-name>   # inspect a specific bot's network
/ai brain status      # check brain state and metadata
/ai brain save        # save the best brain to disk
/ai brain load        # load a previously saved brain
/ai brain reset       # generate a fresh random brain
```

## Genetic algorithm

### Population lifecycle

1. **Initialization**: random networks (or seeded from a loaded brain) are created.
2. **Evaluation**: bots fight each other using `NEAREST_BOT` targeting. Fitness is sampled every tick.
3. **Selection**: tournament selection picks parents (tournament size: 5).
4. **Crossover**: uniform crossover — each weight/bias is independently chosen from either parent.
5. **Mutation**: Gaussian mutation with configurable rate (default: 0.035) and strength (default: 0.12).
6. **Elite preservation**: the top networks (default: 6) are carried forward unchanged.
7. **Repeat**: the next generation spawns with the evolved population.

### Adaptive mutation

When enabled (default), mutation rate and strength cool down over generations:

- Rate cooling: `max(0.55, 1 - min(0.45, gen * 0.01))`
- Strength cooling: `max(0.45, 1 - min(0.55, gen * 0.012))`

Early generations explore broadly; later generations refine. Parameters are clamped to [-4.0, 4.0].

## Fitness scoring (movement-controller mode)

Fitness is calculated from per-tick samples collected during each bot's lifetime:

### Rewards

| Factor | Weight | What it measures |
| --- | --- | --- |
| `range-control` | 300 | Distance error from desired range (lower is better) |
| `range-urgency` | 220 | Approach speed relative to urgency request |
| `crit-setup` | 75 | Ticks where bot is in a legal crit-fall position when requested |
| `sprint-hit` | 45 | Ticks where bot is sprinting when sprint-hit is requested |
| `hold-position` | 35 | Ticks where bot holds position when requested |
| `circling` | 90 | Lateral movement while at desired range |
| `retreat` | 70 | Backward movement compliance |
| `survival` | 0.25 | Alive ticks bonus |
| `damage-dealt` | 275 | Kills multiplied by weight |

### Penalties

| Factor | Weight | What it penalizes |
| --- | --- | --- |
| `damage-taken-penalty` | 4 | Damage deficit |
| `stuck-penalty` | 45 | Static position (not moving) |
| `fallback-penalty` | 35 | Movement NN fallback to legacy movement |
| `invalid-output-penalty` | 35 | Network produced invalid outputs |
| `oscillation-penalty` | 12 | Rapid direction changes |
| `jump-spam-penalty` | 180 | Excessive jumping |
| `sprint-spam-penalty` | 120 | Excessive sprinting |
| `hold-violation-penalty` | 85 | Moving when hold-position is requested |

All weights are configurable in `config.yml` under `ai.training.fitness-weights`.

## Loadout mixing

Each generation assigns loadouts from a weighted pool. By default, all 13 combat loadouts have equal weight, so bots train against diverse weapon types. This prevents the NN from overfitting to a single weapon's movement patterns.

Configure the pool in `config.yml`:

```yaml
ai:
  training:
    loadouts:
      pool:
        - sword
        - mace
        - trident
        # ... add or remove as needed
      weights:
        sword: 1
        mace: 1
        trident: 1
        # ... adjust weights to bias training
```

Higher weight = more frequent selection. Set weight to 0 or remove from the pool to exclude a loadout.

## Training tips

- **Start small**: 60--80 population for faster iteration while tuning config. Scale to 120+ once you're happy with fitness weights.
- **Watch for convergence**: if average fitness plateaus, the population may have converged. Try resetting with `/ai brain reset` and adjusting mutation rates.
- **Save often**: `/ai brain save` saves the current best brain. Saves are safe to run mid-generation.
- **Autosave**: when `ai.movement-network.autosave-best-brain` is `true` (default), the best brain from each generation auto-saves if it improves on the previous best.
- **Check fitness**: `/ai brain status` shows the best and average fitness from the latest generation.
- **Server performance**: training spawns many bots fighting simultaneously. Reduce population size or increase `tick-rate` if the server lags.
- **Generations limit**: set `ai.training.generations` to a non-zero value to auto-stop after N generations. Default is 0 (unlimited).

## Spawning trained bots

After training, spawn bots that use the saved brain:

```
/ai movement 5 Soldier
```

These bots load the saved brain from `brain.json` and use the movement-controller NN for footwork while the CombatDirector handles combat. If no brain file exists, they get a random network.

## Configuration reference

See [Configuration](Configuration) for the complete list of training, fitness, and loadout settings.

See [Brain Persistence](Brain-Persistence) for details on save/load/reset behavior and file format.
