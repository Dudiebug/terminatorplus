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
| `/bot spawn single` (legacy `/bot create`) | `BotCommand.spawn(...)` | Spawn a bot at/near a location with naming/skin options | Yes | Active/current 1v1 path |
| `/bot spawn multiple` (legacy `/bot multi`) | `BotCommand.spawn(...)` | Spawn multiple bots quickly | Yes | Active but broad-plugin behavior; strategy mismatch |
| `/bot admin reset` (legacy `/bot reset`) | `BotCommand.admin(...)` | Remove/reset bots and AI state | Yes | Active admin/runtime control |
| `/bot equipment give` (legacy `/bot give`) | `BotCommand.equipment(...)` | Give items to bot inventory/mainhand path | Yes | Active but high-risk inventory mutation |
| `/bot settings placement-material` (legacy `/bot place`) | `BotCommand.settings(...)` | Set the global block used by generic bot building and clutch placement; defaults to `COBBLESTONE` | Yes | Active legacy compatibility control |
| `/bot equipment armor` (legacy `/bot armor`) | `BotCommand.equipment(...)` | Set armor/offhand equipment | Yes | Active but high-risk inventory mutation |
| `/bot inspect info` (legacy `/bot info`) | `BotCommand.inspect(...)` | Show bot info; partial UX and placeholder behavior remain | No | Active but partially incomplete |
| `/bot inspect list` (legacy `/bot count`) | `BotCommand.inspect(...)` | Show bot count | No | Active inspection utility |
| `/bot settings` | `BotCommand.settings(...)` | Change target goals, region, and related globals | Yes | Active but legacy/protected runtime control |
| `/bot debug behavior` (legacy `/bot debug`) | `BotCommand.debug(...)` | Hidden reflective debugger/admin surface | Yes | Optional/debug/admin; risky |
| `/bot inspect weapons` (legacy `/bot weapons`) | `BotCommand.inspect(...)` | Show weapon/loadout-related information | No | Active reference/inspection |
| `/bot debug combat` (legacy `/bot combatdebug`) | `BotCommand.debug(...)` | Toggle combat logging/tracing for bots | Yes | Optional/debug/admin |
| `/bot move gather` (legacy `/bot gather`) | `BotCommand.move(...)` | Pull/gather bots around a player | Yes | Active but broad-plugin/admin behavior |
| `/bot equipment inventory` (legacy `/bot inventory`) | `BotCommand.equipment(...)` | Open the exact-target transactional inventory editor | Yes | Active but high-risk |
| `/bot preset save/apply/delete` | `BotCommand.preset(...)` | Persist or apply full presets | Yes | Active/current support system |
| `/bot equipment loadout` (legacy `/bot loadout`) | `BotCommand.equipment(...)` | Apply structured loadouts | Yes | Active, but broader than narrow duel-core default |
| `/bot equipment mixed-loadout` (legacy `/bot loadoutmix`) | `BotCommand.equipment(...)` | Apply mixed or grouped loadout scenarios | Yes | Active training/broad-surface feature |
| `/ai` | `AICommand.root(...)` | Main AI command entry/help surface | No | Active shell |
| `/ai train reinforcement` (legacy `/ai reinforcement`) | `AICommand.train(...)` | Start reinforcement/training flows; defaults toward movement-controller work | Yes | Active training-only |
| `/ai reinforcement legacy` | `AICommand.reinforcement(...)` legacy mode branch | Start older legacy/full-NN training path | Yes | Legacy/protected training surface |
| `/ai spawn movement` (legacy `/ai movement`) | `AICommand.spawn(...)` | Spawn movement-controller bots | Yes | Active training/runtime support |
| `/ai brain` | `AICommand.brain(...)` | Save/load/reset movement brains | Yes | Active persistence/training support |
| `/ai evaluate` | `AICommand.evaluate(...)` | Export evaluation results/reports | Yes | Active training/debug surface |
| `/ai spawn random` (legacy `/ai random`) | `AICommand.spawn(...)` | Use older random/full-NN style path | Yes | Legacy/reference |
| `/ai train stop` (legacy `/ai stop`) | `AICommand.train(...)` | Stop AI/training sessions | Yes | Active runtime/training control |
| `/ai inspect info` (legacy `/ai info`) | `AICommand.inspect(...)` | Show AI mode/status information | No | Active inspection utility |
| `/bot environment` (legacy `/botenvironment`) | `BotCommand.environment(...)` | Adjust solid-material overrides and custom mob-list behavior | Yes | Active but legacy/protected admin tooling |
| `/terminatorplus` | `MainCommand.root(...)` | Plugin info/help surface | No | Active shell |
| `/terminatorplus debuginfo` | `MainCommand.debugInfo(...)` | Upload debug info externally through `mclo.gs` | Yes | Optional/debug/admin with external side effect |

## Reading the table

The table above is the canonical command legend: active rows include the
current `/bot create` flow and its inventory, preset, and loadout support;
training, admin/debug, and legacy rows remain live but secondary to the narrow
duel path. Treat rows that mutate spawn, inventory, presets, target/manager
state, or environment overrides as high-risk. See [inventory flow](./10-inventory-loadout-preset-gui-flow.md)
and [scope and safety](./01-scope-and-safety.md) before changing them.
