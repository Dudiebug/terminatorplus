# Movement Brain Bank

TerminatorPlus movement AI is split into combat ownership and movement ownership.
`CombatDirector` remains the sole owner of combat decisions: weapon selection,
hotbar changes, blocking, attacks, item use, crystals, anchors, pearls, wind
charges, fireworks, potions, cobwebs, lava, and behavior timing. Movement brains
output locomotion only: forward/retreat pressure, strafe pressure, jump, sprint,
facing adjustment, urgency, and hold-position cooperation.

## Runtime Routing

Movement routing is typed by `MovementBranchFamily`, not by raw item name or a
numeric branch code. The router uses this precedence:

1. Lock or committed family, when `CombatIntent` is in a non-interruptible phase.
2. `CombatIntent.branchFamily`, emitted by `CombatDirector.plan(...)`.
3. Desired range and planned role fallback.
4. `general_fallback`.

The current families are:

- `general_fallback`
- `melee`
- `mace`
- `trident_ranged`
- `spear_melee`
- `mobility`
- `explosive_survival`
- `projectile_ranged`

Scanner plays are mapped to these semantic families. The optional play id remains
telemetry and does not become its own movement family.

## Persistence Layout

The bank lives under `plugins/TerminatorPlus/ai/movement/`:

```text
ai/movement/manifest.json
ai/movement/brains/general.json
ai/movement/brains/melee.json
ai/movement/brains/mace.json
ai/movement/brains/trident_ranged.json
ai/movement/brains/spear_melee.json
ai/movement/brains/mobility.json
ai/movement/brains/explosive_survival.json
ai/movement/brains/projectile_ranged.json
```

The manifest records the manifest schema version, observation schema hash, action
schema hash, route table version, default brain, route-to-file table, training
build version, and legacy import metadata. Each brain file stores its own family,
architecture, weights, normalization metadata, training metadata, and rollout
metrics.

If only the legacy `ai/brain.json` exists, compatible files import as
`general_fallback`. Specialized routes then continue to fall back safely until
specialist files exist. Bad manifests or bad brain files are backed up or
quarantined according to config, and the loader keeps any valid subset plus a
safe fallback.

## Commands

`/ai brain status|load|save|reset` is bank-aware.

- `status` shows manifest/schema hashes, route table version, fallback state,
  missing optional experts, loadout mix, curriculum family, and active session.
- `load` reloads the manifest and per-brain files.
- `save` writes the current bank, or a movement-controller bot's bank when a bot
  name is supplied.
- `reset` creates a fresh `general_fallback` bank and backs up existing files.

`/ai movement <amount> <name> [skin] [loc]` spawns fighting bots that use the
loaded movement bank. They are not training bots; `CombatDirector` still handles
combat.

`/ai reinforcement ... movement` automatically samples weighted named loadouts
for training bots and records assigned loadout/family telemetry.

## Training Mixes

The default movement-training mix is `movement_balanced`:

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

`pvp + crystalpvp + anchorbomb` total 8%. That keeps explosive survival visible
without letting crystal/anchor chaos dominate every generation.

Curriculum mixes exist for `melee`, `mace`, `trident`, `mobility`, and
`explosive_survival`. Set `ai.training.curriculum-family` to train a specialist
family. Leave it as `general_fallback` for mixed training.

Current limitation: mixed training records per-family telemetry and reward
components, but updates `general_fallback`. Curriculum mode updates the configured
family brain.

## Reward Profiles

Reward scoring is family-specific:

- `general_fallback`: balanced damage delta, survival, range control, low
  fallback rate, low route thrash.
- `melee`: melee threat range, legal crit/sprint-hit setup, hit conversion.
- `mace`: charge/launch/airborne/smash conversion and low self-fall damage.
- `trident_ranged`: charge completion, line of sight, throw range, hit rate.
- `spear_melee`: close trident pressure and stable melee spacing.
- `mobility`: gap close, escape from bad range, vertical setup, safe handoff.
- `explosive_survival`: target explosive damage, safe blast spacing, escape,
  low self-damage.
- `projectile_ranged`: line-of-sight control, lateral strafing, projectile
  hit/interrupt success.

Rollout exports include per-family totals and component keys where training has
recorded them.

## Evaluation

Use:

```text
/ai evaluate list
/ai evaluate [variant] [scenario] [seed[,seed...]]
```

Default:

```text
/ai evaluate branch_family_latched all 1337,7331,424242
```

Reports are written to:

```text
plugins/TerminatorPlus/ai/movement/evaluations/
```

The export includes plugin version, Paper target version, manifest/schema hashes,
route table version, seeds, variant, scenario, loadout distribution, requested
and active branch-family distribution, fallback counts/rates, missing or
incompatible brain fallback counts, route switch/thrash probes, per-family reward
components, and placeholders for live arena metrics.

Supported/report-only variants:

- `general_brain`
- `branch_family_latched`

Explicit pending or unsupported variants:

- `legacy`: pending until an arena runner can force legacy movement for the same
  seed/scenario matrix.
- `weapon_family`: unsupported because runtime routing is branch-family based.
- `branch_family_no_latch`: unsupported because commit latching is integrated in
  the current runtime router.

Live metrics such as win rate, damage delta, self-damage, self-KO rate, time in
desired range, committed conversion rate, mace-smash connect rate, trident hit
rate, mobility gap-close success, safe explosive execution, and projectile
hit/interrupt success are present in the JSON schema but remain `null` in
report-only exports until a live arena evaluator records them.

## Config Keys

```yaml
ai:
  movement:
    enabled: true
    mode: movement_controller
    layer-shape: [37, 32, 16, 8]
    legacy-brain-path: ai/brain.json
    bank:
      enabled: true
      manifest-path: ai/movement/manifest.json
      brains-directory: ai/movement/brains
      fallback-brain-name: general_fallback
      autosave-best-brain: false
      save-only-improved-brain: true
      quarantine-bad-files: true
      legacy-import-behavior: import-compatible-or-reset
      debug-logging: false
  training:
    loadout-mix: movement_balanced
    curriculum-family: general_fallback
    loadout-mixes: {}
  evaluation:
    default-variant: branch_family_latched
    default-scenario: all
    default-seeds: [1337, 7331, 424242]
    export-directory: ai/movement/evaluations
```

## Regression Guard

The Gradle task `checkMovementOnlyContract` scans movement-layer Java sources for
banned combat-authority calls. It is wired into `check` and therefore into
`build`.

```text
.\gradlew.bat checkMovementOnlyContract -q
.\gradlew.bat build -q
```

