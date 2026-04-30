# Quick Start

## Spawn and Equip

```text
/bot create TestBot
/bot loadout hybrid TestBot
/bot weapons TestBot
```

The bot uses `CombatDirector` for combat decisions and the normal legacy
movement path unless it was spawned by `/ai movement`.

## Edit and Save a Kit

```text
/bot inventory TestBot
/bot preset save mykit TestBot
/bot create T2
/bot preset apply mykit T2
```

## Train Movement

```text
/ai reinforcement 120 TrainBot Steve movement
/ai brain status
/ai stop
```

Movement training assigns weighted loadouts automatically from
`ai.training.loadout-mix`. The default `movement_balanced` mix keeps crystal,
anchor, and all-chaos kits low-weight while still sampling them.

To train a specialist:

```text
/ai reinforcement 120 TrainBot Steve movement:family=mace:mix=mace_curriculum
```

Mixed mode records per-family telemetry but updates `general_fallback`.
Curriculum mode updates the configured family brain.

## Spawn Movement-Bank Bots

```text
/ai movement 5 Soldier
```

These are fighting bots, not training bots. The movement bank handles footwork
only; `CombatDirector` remains responsible for all combat actions.

## Export an Evaluation Report

```text
/ai evaluate list
/ai evaluate branch_family_latched all 1337,7331,424242
```

Reports are written under `plugins/TerminatorPlus/ai/movement/evaluations/` and
include seed/scenario metadata, schema versions, loadout distribution, active
branch-family distribution, fallback counts, route switch probes, and reward
component summaries.

## Next

- [Commands](Commands)
- [Loadouts](Loadouts)
- [Combat Behaviors](Combat-Behaviors)
- [Movement Network](Movement-Network)
- [Movement Brain Bank](Movement-Brain-Bank)
- [AI Training](AI-Training)

