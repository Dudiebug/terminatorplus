# Configuration

`config.yml` is generated in `plugins/TerminatorPlus/` on first startup. The
current AI settings live under `ai`.

## Movement Bank

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
      autosave-best-brain: true
      save-only-improved-brain: true
      quarantine-bad-files: true
      legacy-import-behavior: import-compatible-or-reset
      debug-logging: false
```

| Key | Default | Description |
| --- | --- | --- |
| `ai.movement.enabled` | `true` | Enables movement-controller evaluation for bots spawned with a movement bank. |
| `ai.movement.mode` | `movement_controller` | Current movement-controller mode name. |
| `ai.movement.layer-shape` | `[37, 32, 16, 8]` | Input count, hidden layers, output count. Changing it invalidates existing brains. |
| `ai.movement.legacy-brain-path` | `ai/brain.json` | Legacy import source. |
| `ai.movement.bank.manifest-path` | `ai/movement/manifest.json` | Bank manifest path. |
| `ai.movement.bank.brains-directory` | `ai/movement/brains` | Per-brain JSON directory. |
| `ai.movement.bank.fallback-brain-name` | `general_fallback` | Safe default brain for missing specialists. |
| `ai.movement.bank.autosave-best-brain` | `true` | Save training winners automatically. |
| `ai.movement.bank.save-only-improved-brain` | `true` | Autosave only when fitness improves. |
| `ai.movement.bank.quarantine-bad-files` | `true` | Move bad manifests/brains aside instead of reusing them. |
| `ai.movement.bank.legacy-import-behavior` | `import-compatible-or-reset` | Import compatible `ai/brain.json` as fallback, otherwise reset safely. |
| `ai.movement.bank.debug-logging` | `false` | Log route changes to console. |

## Training

```yaml
ai:
  training:
    loadout-mix: movement_balanced
    curriculum-family: general_fallback
    loadout-mixes:
      movement_balanced:
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

`ai.training.loadout-mix` selects the automatic weighted loadout pool for
movement training. The default keeps `pvp + crystalpvp + anchorbomb` at 8% total
so explosive movement is represented without dominating the population.

`ai.training.curriculum-family` controls the training mode. Leave it as
`general_fallback` for mixed mode, which trains eligible families from the
loadout mix in one generation. Set it to a family such as `mace`, `mobility`, or
`explosive_survival` to force a focused specialist curriculum.

## Evaluation

```yaml
ai:
  evaluation:
    default-variant: branch_family_latched
    default-scenario: all
    default-seeds: [1337, 7331, 424242]
    export-directory: ai/movement/evaluations
```

`/ai evaluate` uses these defaults when arguments are omitted. Exported reports
include plugin version, Paper target version, movement manifest version,
observation/action schema hashes, bank route table version, seeds, variant,
scenario, loadout distribution, active `MovementBranchFamily` distribution,
fallback counts, missing/incompatible fallback counts, route switch/thrash
probes, and per-family reward components.

Supported report-only variants are `general_brain` and
`branch_family_latched`. `legacy`, `weapon_family`, and
`branch_family_no_latch` are exposed as pending or unsupported rather than being
silently treated as another mode.
