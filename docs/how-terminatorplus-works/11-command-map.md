# 11. Command Map

This section explains the visible command surface and how it maps to current,
legacy, admin, and training behavior.

## How commands are registered

Commands are declared in `src/main/resources/plugin.yml` and wired in runtime
through `CommandHandler.java`.

`CommandHandler` maps annotated methods from command classes such as:

- `MainCommand`
- `BotCommand`
- `AICommand`
- `BotEnvironmentCommand`

The command system is not only a UX layer. Several commands directly mutate
high-risk runtime systems:

- bot creation/removal
- inventory and equipment
- presets
- AI/training state
- target goals and manager globals
- environment material overrides

## Command classification model used here

This file classifies each command into one or more of:

- current 1v1 path
- active but legacy/protected
- optional/debug/admin
- training-only
- archive candidate from default strategy

Some commands sit in more than one category. For example, a command can be
active and useful while still reflecting broad old-plugin strategy rather than
the desired future default user path.

## Command table

| Command | Class/method | Purpose | Runtime state changed? | Current/legacy/archive status |
|---|---|---|---|---|
| `/bot` | `BotCommand.root(...)` | Main bot command entry and help surface | No | Active shell; docs should be narrowed |
| `/bot create` | `BotCommand.create(...)` | Spawn a bot at/near a location with naming/skin options | Yes | Active/current 1v1 path |
| `/bot multi` | `BotCommand.multi(...)` | Spawn multiple bots quickly | Yes | Active but broad-plugin behavior; strategy mismatch |
| `/bot reset` | `BotCommand.reset(...)` | Remove/reset bots and AI state | Yes | Active admin/runtime control |
| `/bot give` | `BotCommand.give(...)` | Give items to bot inventory/mainhand path | Yes | Active but high-risk inventory mutation |
| `/bot armor` | `BotCommand.armor(...)` | Set armor/offhand equipment | Yes | Active but high-risk inventory mutation |
| `/bot info` | `BotCommand.info(...)` | Show bot info; partial UX and placeholder behavior remain | No | Active but partially incomplete |
| `/bot count` | `BotCommand.count(...)` | Show bot count | No | Active inspection utility |
| `/bot settings` | `BotCommand.settings(...)` | Change target goals, region, and related globals | Yes | Active but legacy/protected runtime control |
| `/bot debug` | `BotCommand.debug(...)` | Hidden reflective debugger/admin surface | Yes | Optional/debug/admin; risky |
| `/bot weapons` | `BotCommand.weapons(...)` | Show weapon/loadout-related information | No | Active reference/inspection |
| `/bot combatdebug` | `BotCommand.combatDebug(...)` | Toggle combat logging/tracing for bots | Yes | Optional/debug/admin |
| `/bot gather` | `BotCommand.gather(...)` | Pull/gather bots around a player | Yes | Active but broad-plugin/admin behavior |
| `/bot inventory` | `BotCommand.inventory(...)` | Open and sync inventory GUI editor | Yes | Active but high-risk |
| `/bot preset save/apply/delete` | `BotCommand.preset(...)` | Persist or apply full presets | Yes | Active/current support system |
| `/bot loadout` | `BotCommand.loadout(...)` | Apply structured loadouts | Yes | Active, but broader than narrow duel-core default |
| `/bot loadoutmix` | `BotCommand.loadoutMix(...)` | Apply mixed or grouped loadout scenarios | Yes | Active training/broad-surface feature |
| `/ai` | `AICommand.root(...)` | Main AI command entry/help surface | No | Active shell |
| `/ai reinforcement` | `AICommand.reinforcement(...)` | Start reinforcement/training flows; defaults toward movement-controller work | Yes | Active training-only |
| `/ai reinforcement legacy` | `AICommand.reinforcement(...)` legacy mode branch | Start older legacy/full-NN training path | Yes | Legacy/protected training surface |
| `/ai movement` | `AICommand.movement(...)` | Manage movement-controller runtime/training operations | Yes | Active training/runtime support |
| `/ai brain` | `AICommand.brain(...)` | Save/load/reset movement brains | Yes | Active persistence/training support |
| `/ai evaluate` | `AICommand.evaluate(...)` | Export evaluation results/reports | Yes | Active training/debug surface |
| `/ai random` | `AICommand.random(...)` | Use older random/full-NN style path | Yes | Legacy/reference |
| `/ai stop` | `AICommand.stop(...)` | Stop AI/training sessions | Yes | Active runtime/training control |
| `/ai info` | `AICommand.info(...)` | Show AI mode/status information | No | Active inspection utility |
| `/botenvironment` | `BotEnvironmentCommand.*` | Adjust solid-material overrides and custom mob-list behavior | Yes | Active but legacy/protected admin tooling |
| `/terminatorplus` | `MainCommand.root(...)` | Plugin info/help surface | No | Active shell |
| `/terminatorplus debuginfo` | `MainCommand.debugInfo(...)` | Upload debug info externally through `mclo.gs` | Yes | Optional/debug/admin with external side effect |

## Commands closest to the current narrow 1v1 path

These are the commands most aligned with the plugin's current strategic
direction:

- `/bot create`
- `/bot reset`
- `/bot give`
- `/bot armor`
- `/bot inventory`
- `/bot preset`
- `/bot loadout`
- `/bot combatdebug`
- `/ai movement`
- `/ai brain`
- `/ai stop`
- `/ai info`

Even within this set, some commands touch high-risk systems and should not be
treated as low-consequence utilities.

## Commands that are active but clearly broader than the current strategy

These still work, but they reflect older, wider plugin ambitions:

- `/bot multi`
- `/bot gather`
- `/bot settings` in its broader target-goal/region sense
- `/bot loadoutmix`
- `/botenvironment`
- `/ai random`
- legacy reinforcement branches

These are strong archive/reclassification candidates at the docs level.

## Commands that touch the highest-risk code paths

These commands deserve extra caution because they mutate fragile runtime state:

- `/bot create`
- `/bot give`
- `/bot armor`
- `/bot inventory`
- `/bot preset apply`
- `/bot loadout`
- `/bot loadoutmix`
- `/bot settings`
- `/botenvironment`

The common high-risk surfaces they touch are:

- NMS bot spawn/removal
- inventory/loadout sync
- target-goal and manager globals
- legacy movement/environment assumptions

## Documentation implication

The current command surface should not be documented as one flat set of equal
"main features."

A better docs split is:

- current duel-focused commands
- training-only commands
- admin/debug commands
- legacy/protected compatibility commands

That split matches the source much better than the older broad-plugin docs
shape.
