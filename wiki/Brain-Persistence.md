# Brain Persistence

Movement-controller persistence uses a bank, not a single primary brain file.
The bank is safe to partially load: bad specialist files can be quarantined while
valid brains and `general_fallback` continue working.

## Layout

Default data folder layout:

```text
plugins/TerminatorPlus/
  ai/brain.json                         # legacy import source
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

`ai/brain.json` is only the legacy import path. Compatible legacy brains import
as `general_fallback`; new saves write the manifest and per-brain files.

## Manifest

The manifest records:

- manifest schema version
- observation schema hash
- action schema hash
- default brain name
- routing table version
- training build version
- route-to-file mapping
- legacy import metadata

## Brain Files

Each brain file records:

- brain name and `MovementBranchFamily`
- observation/action schema hashes
- layer shape, input/output counts, parameter count, activation metadata
- weights
- normalization metadata
- training generation, best fitness, average fitness
- rollout and per-family reward metrics

## Commands

`/ai brain status` shows manifest path, schema hashes, route table version,
fallback brain, missing optional experts, configured loadout mix, curriculum
family, and active session.

`/ai brain load` reloads the manifest and valid per-brain files.

`/ai brain save [bot-name]` saves the loaded bank, or the named
movement-controller bot's bank.

`/ai brain reset` creates a fresh random `general_fallback` bank and backs up
existing files.

## Validation and Quarantine

Loading validates manifest schema, observation/action schema hashes, brain schema,
family compatibility, network shape, parameter count, finite weights, and network
integrity. Incompatible files are backed up or quarantined when
`ai.movement.bank.quarantine-bad-files` is true.

Missing or incompatible specialist brains route through `general_fallback`.
Missing or incompatible fallback data causes the loader to generate a fresh safe
fallback.

## Evaluation Reports

`/ai evaluate ...` writes reports under
`plugins/TerminatorPlus/ai/movement/evaluations/`. Reports include manifest and
schema information, route fallback state, seed/scenario metadata, active branch
family distribution, loadout distribution, and reward component summaries.

