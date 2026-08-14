# AI Training

> See [Legacy Status](Legacy-Status) for this page's reference status and
> [Current Strategy](Current-Strategy) for the current target.


TerminatorPlus can train movement-controller brains in the JVM. The neural
network controls movement only; `CombatDirector` remains the sole owner of
combat actions and item decisions.

## Start Training

```text
/ai reinforcement <population-size> <name> [skin] [mode-or-options] [round-minutes]
```

Examples:

```text
/ai reinforcement 120 TrainBot Steve
/ai reinforcement 120 TrainBot Steve family=mace:mix=mace_curriculum 5
/ai reinforcement 120 TrainBot Steve movement:family=mace:mix=mace_curriculum 5
/ai reinforcement 80 TrainBot Steve legacy
```

If `[round-minutes]` is omitted, the command uses
`ai.training.max-round-minutes` from `config.yml`. The default is `1`; set it to
`0` for unlimited rounds. Optional command arguments are positional, so pass
`movement` or an options string in the mode slot when you want to override only
the minute cap:

```text
/ai reinforcement 120 TrainBot Steve movement 5
```

Modes:

- Empty mode: train the movement brain bank.
- `movement`, `movement_controller`, or `movement-controller`: train the
  movement brain bank.
- `legacy`: original full-replacement neural-network training.

## Automatic Loadout Assignment

Movement training samples named loadouts automatically from
`ai.training.loadout-mix`. No separate `/bot loadoutmix` step is needed.

The default `movement_balanced` weights are documented in
[Configuration](Configuration).

`pvp`, `crystalpvp`, and `anchorbomb` total 8%, which gives explosive/survival
movement enough exposure without letting those kits dominate every generation.

## Curriculum Families

Set `ai.training.curriculum-family` or pass `family=<name>` in the training
options argument to train one specialist family:

- `melee`
- `mace`
- `trident_ranged`
- `spear_melee`
- `mobility`
- `explosive_survival`
- `projectile_ranged`

Curriculum mixes are configured under `ai.training.loadout-mixes`, including
`melee_curriculum`, `mace_curriculum`, `trident_curriculum`,
`mobility_curriculum`, and `explosive_survival_curriculum`.

Mixed training seeds each candidate from its assigned loadout family, ranks
results by the movement family that actually produced route samples, and updates
every eligible specialist brain represented in the round. A family brain is not
saved from unrelated survival or aggregate fitness. Curriculum mode forces all
candidates to update the configured family brain.

## Reward Profiles

Movement reward scoring is family-specific:

- `general_fallback`: balanced damage delta, survival, range control, low
  fallback rate, and low route thrash.
- `melee`: melee threat range, legal crit/sprint-hit setup, and hit conversion.
- `mace`: launch/airborne/smash phase conversion, tracking, and self-damage
  avoidance.
- `trident_ranged`: charge completion, line of sight, throw range, and hit rate.
- `spear_melee`: close trident pressure and stable melee spacing.
- `mobility`: gap close, escape, vertical setup, and route handoff success.
- `explosive_survival`: target explosive damage, safe blast spacing, escape, and
  low self-damage.
- `projectile_ranged`: line-of-sight control, lateral strafing, and projectile
  hit/interrupt success.

Rollout metrics are saved with the trained brain and surfaced by evaluation
reports.

## Commands

See [Commands](Commands) for full `/ai` syntax, including training, bank,
movement, and evaluation commands. [Movement Brain Bank](Movement-Brain-Bank)
and [Brain Persistence](Brain-Persistence) cover bank behavior and file formats.
