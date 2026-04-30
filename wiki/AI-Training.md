# AI Training

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
`0` for unlimited rounds.

Modes:

- Empty mode: train the movement brain bank.
- `movement`, `movement_controller`, or `movement-controller`: train the
  movement brain bank.
- `legacy`: original full-replacement neural-network training.

## Automatic Loadout Assignment

Movement training samples named loadouts automatically from
`ai.training.loadout-mix`. No separate `/bot loadoutmix` step is needed.

The default `movement_balanced` mix is:

```yaml
sword: 12
axe: 12
smp: 12
pot: 8
mace: 10
spear: 8
trident: 8
windcharge: 6
skydiver: 5
hybrid: 6
vanilla: 5
pvp: 3
crystalpvp: 3
anchorbomb: 2
```

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

Mixed training ranks candidates by assigned loadout family and updates every
eligible specialist brain represented in the round. Curriculum mode forces all
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

```text
/ai stop
/ai brain status
/ai brain load
/ai brain save [bot-name]
/ai brain reset
/ai movement <amount> <name> [skin] [loc]
/ai evaluate [variant] [scenario] [seeds]
```

`/ai movement` spawns fighting bots using the loaded movement bank. They are not
training bots.

`/ai evaluate` exports a report-only seed/scenario matrix and route/fallback
summary under `ai/movement/evaluations/`. Live win/damage metrics require an
arena run.

See [Movement Brain Bank](Movement-Brain-Bank) and
[Brain Persistence](Brain-Persistence) for the bank layout and file formats.

