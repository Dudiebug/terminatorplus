# TerminatorPlus

A Paper plugin for creating server-side player bots with configurable loadouts,
weapon-aware combat AI, a movement-only neural network bank, and in-JVM
genetic-algorithm training.

[![License: EPL-2.0](https://img.shields.io/github/license/Dudiebug/terminatorplus?color=violet&labelColor=000000&style=for-the-badge)](LICENSE)
[![Discord](https://img.shields.io/discord/357333217340162069?color=5865F2&label=Discord&logo=Discord&labelColor=23272a&style=for-the-badge)](https://discord.gg/vZVSf2D6mz)

## Highlights

- Server-side `ServerPlayer` bots that take and deal real vanilla damage.
- Weapon-aware `CombatDirector` for swords, axes, maces, tridents, wind charges,
  pearls, crystals, anchors, cobwebs, totems, elytra/fireworks, and consumables.
- Full editable 41-slot inventory with GUI support.
- Built-in loadouts: `sword`, `axe`, `smp`, `pot`, `mace`, `spear`, `trident`,
  `windcharge`, `skydiver`, `hybrid`, `vanilla`, `pvp`, `crystalpvp`,
  `anchorbomb`, and `clear`.
- Movement brain bank under `ai/movement/` with manifest, per-family brains,
  schema validation, legacy import, fallback, and quarantine behavior.
- Movement brains output locomotion only. `CombatDirector` remains the sole owner
  of combat actions, item use, hotbar selection, crystals, anchors, projectiles,
  and behavior timing.
- `/ai reinforcement ...` defaults to movement-controller training and
  automatically assigns weighted training loadouts and records per-family
  telemetry.
- `/ai evaluate` exports repeatable seed/scenario reports with route/family
  distributions, fallback state, schema metadata, and reward components.

## Version Support

Built for the active Paper target branch, such as Paper 26.1.2 or Paper
1.21.11, with Java 25. Spigot and CraftBukkit are not supported.

## Quick Start

```text
/bot create TestBot
/bot loadout hybrid TestBot
/bot inventory TestBot
/bot preset save mykit TestBot
/bot preset apply mykit
```

Train and deploy movement-bank bots:

```text
/ai reinforcement 120 TrainBot Steve
/ai brain status
/ai stop
/ai movement 5 Soldier
```

Run a report-only evaluation export:

```text
/ai evaluate list
/ai evaluate branch_family_latched all 1337,7331,424242
```

Reports are written under
`plugins/TerminatorPlus/ai/movement/evaluations/`.

## Movement Architecture

`CombatDirector.plan(...)` emits a typed `CombatIntent`. `MovementBrainRouter`
then selects a brain by:

1. lock or committed family
2. `CombatIntent.branchFamily`
3. desired range or role fallback
4. `general_fallback`

The movement network receives a 37-value observation vector with one-hot
`MovementBranchFamily` fields and produces 8 locomotion outputs. It may walk,
jump, sprint, strafe, retreat, adjust facing, and report movement state. It may
not call combat actions.

## Persistence

Default layout:

```text
plugins/TerminatorPlus/ai/movement/manifest.json
plugins/TerminatorPlus/ai/movement/brains/general.json
plugins/TerminatorPlus/ai/movement/brains/melee.json
plugins/TerminatorPlus/ai/movement/brains/mace.json
plugins/TerminatorPlus/ai/movement/brains/trident_ranged.json
plugins/TerminatorPlus/ai/movement/brains/spear_melee.json
plugins/TerminatorPlus/ai/movement/brains/mobility.json
plugins/TerminatorPlus/ai/movement/brains/explosive_survival.json
plugins/TerminatorPlus/ai/movement/brains/projectile_ranged.json
```

Compatible legacy `ai/brain.json` files import as `general_fallback`. Missing
specialists fall back safely; bad files can be quarantined.

## Training Notes

The default `movement_balanced` mix keeps `pvp + crystalpvp + anchorbomb` at 8%
total so explosive movement is represented without dominating training.

Current limitation: mixed training records per-family telemetry but updates
`general_fallback`. Curriculum mode updates the configured family brain via
`ai.training.curriculum-family`.

## Documentation

Full documentation is in the [Wiki](https://github.com/Dudiebug/terminatorplus/wiki):

[Commands](https://github.com/Dudiebug/terminatorplus/wiki/Commands) |
[Loadouts](https://github.com/Dudiebug/terminatorplus/wiki/Loadouts) |
[Combat Behaviors](https://github.com/Dudiebug/terminatorplus/wiki/Combat-Behaviors) |
[Movement Network](https://github.com/Dudiebug/terminatorplus/wiki/Movement-Network) |
[Movement Brain Bank](https://github.com/Dudiebug/terminatorplus/wiki/Movement-Brain-Bank) |
[AI Training](https://github.com/Dudiebug/terminatorplus/wiki/AI-Training) |
[Brain Persistence](https://github.com/Dudiebug/terminatorplus/wiki/Brain-Persistence) |
[Configuration](https://github.com/Dudiebug/terminatorplus/wiki/Configuration)

## License

[Eclipse Public License 2.0](LICENSE)

