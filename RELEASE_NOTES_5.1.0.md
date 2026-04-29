# TerminatorPlus 5.1.0

## Highlights

- Added movement-controller neural network mode.
- Added `CombatIntent` and `MovementState` coupling between `CombatDirector` and movement AI.
- Added JVM-only layered `MovementNetwork` with stable movement input/output schemas.
- Wired movement-controller bots through `plan -> movement -> execute`.
- Preserved `CombatDirector` ownership of weapon selection, attack timing, `BotCombatTiming` gates, mace/trident actions, and `bot.attack(...)` execution.
- Added movement-aware GA training with tournament selection, uniform crossover, adaptive mutation, and movement cooperation fitness.
- Added randomized training loadout mixing across supported presets.
- Added movement NN config defaults and training fitness weights.
- Added movement brain persistence, validation, reset/load/save behavior, and `/ai` command support.
- Preserved legacy movement and old four-node full-replacement NN compatibility.

## Compatibility

- Non-NN bots remain unaffected.
- Legacy full-replacement NN mode remains available.
- Movement-controller fallback to legacy movement remains available.
- Invalid, missing, or corrupt movement brain files fail safely.
