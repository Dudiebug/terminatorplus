# TerminatorPlus Wiki

TerminatorPlus 5.2.x is a Paper 26.1.2 plugin that spawns server-side player bots with combat
AI, editable inventories, movement neural networks, a movement-brain bank, and a
preset system. Each bot is an NMS `ServerPlayer` subclass: it takes and deals
real damage, uses real items, and reacts through vanilla mechanics.

## Pages

- [Installation](Installation)
- [Quick Start](Quick-Start)
- [Commands](Commands)
- [Loadouts](Loadouts)
- [Combat Behaviors](Combat-Behaviors)
- [Movement Network](Movement-Network)
- [Movement Brain Bank](Movement-Brain-Bank)
- [AI Training](AI-Training)
- [Brain Persistence](Brain-Persistence)
- [Configuration](Configuration)
- [Presets](Presets)
- [Inventory GUI](Inventory-GUI)
- [API](API)
- [Troubleshooting](Troubleshooting)
- [Changelog](Changelog)
- [Release Notes 5.1.1](Release-Notes-5.1.1)

## Movement Brain Bank

- Movement brains are locomotion-only; `CombatDirector` retains full authority
  over combat.
- Movement routing uses `MovementBranchFamily`: lock, intent branch, range/role
  fallback, then `general_fallback`.
- Brain persistence uses `ai/movement/manifest.json` plus per-family files under
  `ai/movement/brains/`.
- Legacy `ai/brain.json` imports as `general_fallback` when compatible.
- `/ai reinforcement ...` defaults to movement training and automatically samples weighted loadouts from
  `movement_balanced` or curriculum mixes.
- Mixed reinforcement trains eligible specialist brains from the same population
  when those bots captured matching route samples.
- Training rounds default to a 1-minute cap through
  `ai.training.max-round-minutes`.
- `/ai evaluate` exports repeatable seed/scenario reports with route/family
  distributions and fallback state.

