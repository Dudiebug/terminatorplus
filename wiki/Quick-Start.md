# Quick Start

> See [Legacy Status](Legacy-Status) for this page's reference status and
> [Current Strategy](Current-Strategy) for the current target.


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
/bot equipment inventory TestBot
# edit the working copy, then click Save changes
/bot preset save mykit TestBot
/bot create T2
/bot preset apply mykit T2
```

## Train Movement

```text
/ai reinforcement 120 TrainBot Steve
/ai brain status
/ai stop
```

Movement training defaults to the movement-controller mode and uses the
configured weighted loadout mix. See [AI Training](AI-Training) for modes,
curriculum families, and command options, and [Configuration](Configuration) for
the defaults.

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
component summaries. See [Movement Brain Bank](Movement-Brain-Bank) for the full
report field and variant reference.

## Next

See [Home](Home) for the full wiki index.
