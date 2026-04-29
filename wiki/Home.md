# TerminatorPlus Wiki

TerminatorPlus is a Paper plugin that spawns server-side player bots with combat AI, a full editable inventory, movement neural networks, and a preset system. Each bot is an NMS `ServerPlayer` subclass — it takes and deals real damage, uses real items, and reacts to its surroundings through vanilla mechanics.

## Pages

- [Installation](Installation)
- [Quick Start](Quick-Start)
- [Commands](Commands)
- [Loadouts](Loadouts)
- [Combat Behaviors](Combat-Behaviors)
- [Movement Network](Movement-Network)
- [AI Training](AI-Training)
- [Brain Persistence](Brain-Persistence)
- [Configuration](Configuration)
- [Presets](Presets)
- [Inventory GUI](Inventory-GUI)
- [API](API)
- [Troubleshooting](Troubleshooting)
- [Changelog](Changelog)
- [Release Notes 5.1.1](Release-Notes-5.1.1)

## What's New in 5.1.1

**Combat reliability (Issue #6)**
- Vanilla attack ordering fix — `Bot.attack()` calls `getBukkitEntity().attack(entity)` before swing/punch to prevent charge reset before damage.
- Charge-aware planning — CombatDirector skips attack branches until charge is ready, reducing swing-block spam.
- Mace recharge planning with custom gravity and required airtime tracking.
- Mace airborne tracking with ground clip tolerance and velocity-aware horizontal damping.
- Sweep instrumentation and telemetry cleanup (critPred, sweepPred, chargeAtVanillaAttack, targetHp, etc.).

**Movement neural network (Issue #7)**
- Movement-only neural network — the NN controls footwork (strafing, spacing, sprint/jump timing, approach/retreat). It does **not** control combat decisions.
- **CombatDirector** retains full authority over weapon selection, attack timing, and combat execution.
- CombatIntent/MovementState coupling — the Director tells the NN what it wants (range, urgency, crit/sprint/hold hints), and the NN reports its movement state back for timing validation.
- In-JVM genetic algorithm training with tournament selection, uniform crossover, and adaptive mutation.
- Brain persistence — trained networks save to `brain.json` with schema validation and safe fallback.
- Configurable training loadout mixing for generalized movement learning.
- Legacy movement and full-replacement NN modes remain available.
- Non-NN bots are unaffected.
