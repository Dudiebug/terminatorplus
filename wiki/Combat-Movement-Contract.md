# Combat Movement Contract

Movement is combat-informed but not combat-authoritative.

`CombatDirector` owns combat decisions, weapon choice, item use, timing,
committed phases, and execution. Movement may consume combat intent and report
movement state, but it must not directly perform combat actions.

## Movement May Consume

- desired range
- urgency
- branch family
- crit setup request
- sprint-hit request
- hold-position request
- committed phase state
- weapon range
- target velocity and bot velocity
- obstruction and reachability data

## Movement May Report

- sprinting
- falling
- retreating
- strafing or circling
- approach speed
- facing
- just jumped

## Movement Must Not Directly

- attack or punch
- block or use items
- select hotbar slots
- apply loadouts
- throw pearls or projectiles
- place or detonate combat blocks
- call weapon behavior internals

The systems are tied through contracts. They are not unrelated, but combat
authority remains centralized in `CombatDirector`.
