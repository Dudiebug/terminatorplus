# TerminatorPlus

A Paper plugin for creating server-side player bots with configurable loadouts, weapon-aware combat AI, movement neural networks, and in-JVM genetic-algorithm training.

[![License: EPL-2.0](https://img.shields.io/github/license/Dudiebug/terminatorplus?color=violet&labelColor=000000&style=for-the-badge)](LICENSE)
[![Discord](https://img.shields.io/discord/357333217340162069?color=5865F2&label=Discord&logo=Discord&labelColor=23272a&style=for-the-badge)](https://discord.gg/vZVSf2D6mz)

## What is TerminatorPlus?

TerminatorPlus spawns NMS `ServerPlayer` bots that take and deal real damage, use real items, and react to combat. Unlike cosmetic NPC plugins, each bot is a true server-side player — crits, shields, enchantments, and knockback all work through vanilla mechanics.

Useful for combat practice, server stress testing, AI experimentation, and PvP arena tooling.

## Highlights

- **Weapon-aware CombatDirector** — picks the right weapon per tick: swords, axes, maces (jump-smash), tridents (momentum throw), wind charges, ender pearls, end crystals, respawn anchors, cobwebs, totems, and elytra gliding with firework boosts.
- **Combat reliability** — charge-aware attack planning prevents swing spam, with crit/sweep/sprint-hit timing and mace recharge tracking.
- **Full editable inventory** — 41-slot layout (hotbar, storage, armor, offhand) with a double-chest GUI editor.
- **15 built-in loadouts** — `sword`, `mace`, `trident`, `windcharge`, `skydiver`, `hybrid`, `crystalpvp`, `anchorbomb`, `pvp`, `vanilla`, `axe`, `smp`, `pot`, `spear`, `clear`.
- **Preset system** — save and restore a bot's full kit and behavior settings as YAML.
- **Movement neural network** — a feed-forward NN that controls footwork (strafing, spacing, sprint/jump timing, approach/retreat). The NN does **not** control combat decisions; the CombatDirector retains full authority over weapon selection and attack timing.
- **In-JVM GA training** — population-based genetic algorithm with tournament selection, uniform crossover, and adaptive mutation. No Python, GPU, or external tooling required.
- **Brain persistence** — trained movement networks save to `brain.json` with schema validation and safe fallback for missing or corrupt files.
- **Loadout mixing** — training sessions rotate through weighted loadout pools for generalized movement learning.
- **API module** — drive bots from your own plugin via `Terminator.combatTick(target)`.

## Version Support

Built against **Paper 26.1.2** and **Paper 1.21.11** (Java 25). Spigot and CraftBukkit are not supported — TerminatorPlus uses Paper-only APIs and NMS internals.

## Installation

1. Download the jar from the [releases page](https://github.com/Dudiebug/terminatorplus/releases).
2. Drop it into your server's `plugins/` directory.
3. Start the server. The plugin generates its config on first run.
4. Edit `plugins/TerminatorPlus/config.yml` as needed and restart.

> **Note:** This plugin uses NMS internals tied to specific Paper builds. Running on an unsupported version may produce unexpected behavior.

## Quick Start

```
/bot create TestBot                    # spawn a bot
/bot loadout hybrid TestBot            # give it a combat loadout
/bot inventory TestBot                 # open the inventory editor GUI
/bot preset save mykit TestBot         # save the kit as a preset
/bot preset apply mykit                # apply to all bots
```

Train a movement brain and deploy it:

```
/ai reinforcement 120 TrainBot         # train (autosaves best brain to brain.json)
/ai brain status                       # check brain state
/ai stop                               # end training
/ai movement 5 Soldier                 # spawn 5 fighting bots that use the trained brain
```

Bots spawned via `/ai movement` are fighting bots, not training bots. They use the trained brain for movement and the CombatDirector for combat. `/bot create` bots do not use the brain — they always run legacy movement.

## Loadouts

| Name | Role |
| --- | --- |
| `sword` | Vanilla netherite duel kit |
| `mace` | Mace-smash specialist with sword fallback |
| `trident` | Spear-thrower in iron armor |
| `windcharge` | Long-range zoning kit |
| `skydiver` | Elytra + trident dive combo |
| `hybrid` | Multi-weapon all-rounder |
| `crystalpvp` | End crystal burst (Overworld/End) |
| `anchorbomb` | Respawn anchor detonation (Nether) |
| `pvp` | Full arsenal — every behavior unlocked |
| `vanilla` | Full arsenal, no elytra, anchors in storage |
| `axe` | Shield-breaking axe PvP |
| `smp` | SMP-style sword + axe, no explosives |
| `pot` | Splash healing PvP |
| `spear` | Trident-only melee in heavy armor |
| `clear` | Wipe all slots |

Loadout mixes for bulk assignment: `/bot loadoutmix alltypes`, `core`, or `problem`.

## Combat AI

The **CombatDirector** owns all combat decisions: weapon selection, attack timing, charge gating, and cooldown management. It runs a priority pipeline each tick, committing to the first matching behavior.

Key behaviors include melee (sword/axe), mace jump-smash, trident momentum throw, wind charge zoning, ender pearl gap-closing, crystal PvP, anchor bombing, cobweb utility, elytra gliding, totem swapping, and healing.

Combat telemetry fields (`critPred`, `sweepPred`, `chargeAtVanillaAttack`, `targetHp`, `targetHpDelta`, etc.) are available for debugging via `/bot combatdebug`.

See the [Combat Behaviors](https://github.com/Dudiebug/terminatorplus/wiki/Combat-Behaviors) wiki page for the full priority table and per-weapon breakdown.

## Movement Neural Network

The movement-controller neural network handles **footwork only**:

| Movement NN controls | CombatDirector controls |
| --- | --- |
| Strafing and spacing | Weapon selection |
| Sprint / jump timing | Attack timing and charge gates |
| Approach / retreat | Mace, trident, and combat actions |
| Facing adjustment | Opportunity evaluation |
| Movement urgency | `bot.attack()` execution |
| Hold-position cooperation | Consumables and passive behaviors |

The Director sends a **CombatIntent** (desired range, urgency, crit/sprint/hold hints) to the NN. The NN returns a **MovementOutput** (8 values). After the NN moves the bot, it reports a **MovementState** back to the Director, which uses it to validate timing windows before executing attacks.

The NN never selects weapons or triggers attacks.

### Network shape

- **Input:** 30 values (relative position, velocity, facing, combat intent, weapon info)
- **Hidden layers:** [32, 24] (configurable)
- **Output:** 8 values (forward, strafe, jump, sprint, retreat, facing, urgency, hold)
- **Activation:** tanh

### Fallback

When the movement network is disabled or the brain file is missing, bots fall back to legacy movement. Non-NN bots (spawned via `/bot create`) are unaffected.

## Training

Training runs entirely in the JVM. No Python, ONNX, or GPU required.

```
/ai reinforcement 120 TrainBot                    # movement-controller (default)
/ai reinforcement 120 TrainBot legacy              # legacy full-replacement mode
```

- **Population:** configurable, default 120 (range 10--240)
- **Selection:** tournament (size 5)
- **Crossover:** uniform per-gene
- **Mutation:** Gaussian with adaptive cooling
- **Elite preservation:** top 6 networks carried forward
- **Fitness:** rewards range control, urgency compliance, circling, retreat, crit/sprint/hold cooperation; penalizes stuck movement, oscillation, jump/sprint spam, fallback activation
- **Loadout mixing:** each generation draws from a weighted loadout pool so the NN generalizes across weapon types

## Persistence

Trained brains save to `plugins/TerminatorPlus/ai/brain.json` (configurable).

```
/ai brain save          # save current best brain
/ai brain load          # load brain from disk
/ai brain reset         # generate a fresh random brain (backs up existing)
/ai brain status        # show brain state and metadata
```

The file includes schema version, network shape, weights, biases, and training metadata (generation, fitness, config hash, loadout mix). On load, shape and schema are validated — a corrupt or incompatible file triggers a safe fallback to legacy movement without crashing.

## Configuration

All settings live in `config.yml` under the `ai` section:

| Section | Key settings |
| --- | --- |
| `ai.movement-network` | `enabled`, `mode`, `hidden-layers`, `brain-path`, `fallback-to-legacy-movement`, `autosave-best-brain`, `debug` |
| `ai.training` | `population-size`, `generations`, `mutation-rate`, `mutation-strength`, `adaptive-mutation`, `elite-count`, `tournament-size` |
| `ai.training.fitness-weights` | 17 weighted factors (range-control, crit-setup, damage-dealt, jump-spam-penalty, etc.) |
| `ai.training.loadouts` | `pool` (list of loadout names) and `weights` (per-loadout integer weight) |

See the [Configuration](https://github.com/Dudiebug/terminatorplus/wiki/Configuration) wiki page for the full reference.

## API

The `TerminatorPlus-API` module lets other plugins drive bots:

```java
BotManager bots = TerminatorPlusAPI.getBotManager();
Terminator bot = bots.createBot(location, "MyBot", null, null);
boolean handled = bot.combatTick(target);
```

See the [API source](https://github.com/Dudiebug/terminatorplus/tree/master/TerminatorPlus-API/src/main/java/net/nuggetmc/tplus/api) and the [API wiki page](https://github.com/Dudiebug/terminatorplus/wiki/API).

## Documentation

Full documentation is on the [Wiki](https://github.com/Dudiebug/terminatorplus/wiki):

[Installation](https://github.com/Dudiebug/terminatorplus/wiki/Installation) ·
[Quick Start](https://github.com/Dudiebug/terminatorplus/wiki/Quick-Start) ·
[Commands](https://github.com/Dudiebug/terminatorplus/wiki/Commands) ·
[Loadouts](https://github.com/Dudiebug/terminatorplus/wiki/Loadouts) ·
[Combat Behaviors](https://github.com/Dudiebug/terminatorplus/wiki/Combat-Behaviors) ·
[Movement Network](https://github.com/Dudiebug/terminatorplus/wiki/Movement-Network) ·
[AI Training](https://github.com/Dudiebug/terminatorplus/wiki/AI-Training) ·
[Brain Persistence](https://github.com/Dudiebug/terminatorplus/wiki/Brain-Persistence) ·
[Configuration](https://github.com/Dudiebug/terminatorplus/wiki/Configuration) ·
[Presets](https://github.com/Dudiebug/terminatorplus/wiki/Presets) ·
[API](https://github.com/Dudiebug/terminatorplus/wiki/API) ·
[Troubleshooting](https://github.com/Dudiebug/terminatorplus/wiki/Troubleshooting)

## License

[Eclipse Public License 2.0](LICENSE)
