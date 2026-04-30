# TerminatorPlus Wiki

TerminatorPlus is a Paper plugin that spawns server-side player bots with combat
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
- `/ai reinforcement ... movement` automatically samples weighted loadouts from
  `movement_balanced` or curriculum mixes.
- `/ai evaluate` exports repeatable seed/scenario reports with route/family
  distributions and fallback state.

