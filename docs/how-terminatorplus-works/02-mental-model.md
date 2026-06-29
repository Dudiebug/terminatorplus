# 2. One-Paragraph Mental Model

TerminatorPlus is a Paper plugin that creates fake `ServerPlayer` bots and
runs them through a layered PvP stack: legacy runtime code still owns target
selection, obstacle handling, survival heuristics, and much of the per-tick
orchestration, while newer systems centralize combat legality and timing in
`CombatDirector` and move learned locomotion into a movement-controller neural
network pipeline. The result is not a clean-sheet duel engine yet. It is a
hybrid plugin where old broad-plugin behavior and newer duel-focused design
coexist, and understanding that coexistence is the key to making safe archive,
docs, or refactor decisions.

## Expanded mental model

If you reduce the plugin to its essential moving parts, it looks like this:

- the plugin enables and constructs a bot manager plus command system
- the bot manager constructs the only live agent, which is still
  `LegacyAgent`
- commands spawn fake players that are backed by NMS `ServerPlayer` objects
- each server tick, the agent loops over live bots
- the agent finds targets, runs obstacle and hazard logic, and chooses how the
  bot should move
- combat decisions are increasingly handed to `CombatDirector`
- movement for newer NN bots is derived from `CombatIntent`, routed through a
  movement brain, and applied back as locomotion only
- inventory, loadouts, and presets are carefully managed through NMS-aware
  code because fake-player inventory behavior is fragile on Paper 26.x

## Why the hybrid model matters

A lot of architectural confusion disappears once you stop asking
"is this old or new?" and instead ask "is this still on the active runtime
path?"

Examples:

- `LegacyAgent` is clearly legacy in style, but it is still the live top-level
  tick orchestrator
- the movement-controller NN is clearly newer and more aligned with the
  current strategy, but it still depends on old orchestration and fallback
  paths
- advanced combat behaviors look modern and specialized, but they still live
  inside a broad combat surface that is bigger than the current narrow 1v1
  goal

So the real architecture is not:

- old plugin vs new plugin

It is:

- legacy runtime scaffolding that still matters
- newer systems layered on top of it
- older broad surfaces that are now better treated as reference/admin/training
  instead of primary user strategy

## Fast summary by subsystem

Plugin lifecycle:

- `TerminatorPlus.java` wires everything together and owns startup/shutdown

Bot existence:

- `Bot.java`, `BotManagerImpl.java`, `MockConnection.java`, and NMS helpers
  make fake players exist and stay rendered

Per-tick orchestration:

- `LegacyAgent.java`

Combat:

- `CombatDirector.java` and behavior classes

Movement:

- old heuristic movement in `LegacyAgent.java`
- old full-replacement NN mode in legacy AI classes
- newer movement-controller NN path in movement classes

Inventory/loadouts:

- `BotInventory.java`, `PresetManager.java`, `BotInventoryGUI.java`

User/admin/training surfaces:

- `BotCommand.java`, `AICommand.java`, `BotEnvironmentCommand.java`,
  `MainCommand.java`

## The right mindset for reading the code

Do not read the plugin expecting strong module boundaries everywhere.

Read it expecting:

- a runtime that grew over time
- newer design ideas added without fully removing older layers
- NMS compatibility code that may look awkward but exists for real reasons
- command surfaces that are wider than the current duel-focused vision

That mindset matches the current source much better than assuming the plugin is
already a minimal 1v1 bot engine.
