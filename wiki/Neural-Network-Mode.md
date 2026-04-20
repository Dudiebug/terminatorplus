# Neural Network Mode

TerminatorPlus was originally built around population-based reinforcement learning for bot PvP. That training pipeline is still in the plugin and takes priority over the new weapon-aware AI whenever a bot has a neural network attached.

## The two modes

| Mode | Damage source | Weapon director |
| --- | --- | --- |
| **Normal** (`/bot create`) | Vanilla `Player.attack()` — real crits, shields, enchants | `CombatDirector` runs every tick |
| **Training** (`/ai random`, `/ai reinforcement`) | Legacy deterministic damage table | `CombatDirector` is **bypassed** |

The switch is `Bot.hasNeuralNetwork()`. At the top of `CombatDirector.tick()`:

```java
if (bot.hasNeuralNetwork()) return false;
```

When `false` is returned, the caller falls back to the legacy damage code path, preserving deterministic fitness scores. Normal bots fall through to the vanilla `attack()` call for real PvP.

## Starting a training session

```
/ai random          # spawn bots with random networks, collect feedback
/ai reinforcement   # run the reinforcement learning loop
/ai stop            # end the session
/ai info <bot>      # inspect a bot's network
```

Training mode is independent of the `/bot` command surface — `/bot create` still spawns non-training bots, and those will use the combat director.

## Why bypass the director for training?

- **Reproducibility**: the legacy damage table is deterministic — same input produces same output. The vanilla attack pipeline is not (knockback resistance RNG, crits, etc.), which corrupts fitness signals.
- **Speed**: `CombatDirector` runs a dozen behaviors and cooldown checks per tick. Skipping it reduces training overhead.
- **Known baseline**: historical fitness scores from earlier revisions remain comparable.

## Flipping between modes

Currently, a bot is either a training bot (spawned by `/ai ...`) or a normal bot (spawned by `/bot create`). The `hasNeuralNetwork()` flag is set at construction; there's no runtime swap.

If you want a bot created by the training pipeline to use the combat director, you'd need to fork the plugin and clear `Bot.neuralNetwork` before it ticks. No command exposes this.

## Interaction with presets

Presets only capture inventory and behavior settings — not neural-network weights. Applying a preset to a training bot doesn't affect its network; applying a preset to a normal bot doesn't create a network. The two systems are independent.

## Future work

The plugin's `README` lists `AI data saved to the plugin data folder, able to be loaded into bots` as a future update. Until that lands, trained networks disappear when the server restarts.
