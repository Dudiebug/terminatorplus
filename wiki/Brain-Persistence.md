# Brain Persistence

Trained movement networks are saved to disk as JSON and can be loaded, reset, or inspected via commands. Invalid or corrupt brain files do not crash the server — the bot falls back to legacy movement.

## File location

Default path: `plugins/TerminatorPlus/ai/brain.json`

Configurable via `ai.movement-network.brain-path` in `config.yml`. The path is relative to the plugin's data folder.

## Commands

### `/ai brain status`
Show the current brain state: whether a brain is loaded, its generation, best fitness, average fitness, shape, and metadata.

### `/ai brain save`
Save the current best movement network to disk. Safe to run mid-training. Overwrites the existing file.

### `/ai brain load`
Load a brain from the file on disk. Validates schema version, mode, and network shape before accepting.

### `/ai brain reset`
Generate a fresh random brain. If an existing brain file is present, it is **backed up** before the reset (the backup filename includes a timestamp).

## File format

```json
{
  "schemaVersion": 1,
  "mode": "movement-controller",
  "inputSchema": "MovementInput:v1:30",
  "outputSchema": "MovementOutput:v1:8",
  "shape": [30, 32, 24, 8],
  "weights": [[[...]]],
  "biases": [[...]],
  "training": {
    "generation": 42,
    "bestFitness": 1523.7,
    "averageFitness": 891.2,
    "timestamp": "2026-04-28T14:30:00Z",
    "configHash": "a1b2c3d4",
    "loadoutMix": "sword=1, mace=1, trident=1",
    "source": "movement-training"
  }
}
```

### Fields

| Field | Purpose |
| --- | --- |
| `schemaVersion` | File format version (currently 1) |
| `mode` | Must be `"movement-controller"` for movement brains |
| `inputSchema` | Input format identifier with count |
| `outputSchema` | Output format identifier with count |
| `shape` | Layer dimensions array (e.g. [30, 32, 24, 8]) |
| `weights` | 3D array: [layer][node][input] |
| `biases` | 2D array: [layer][node] |
| `training.generation` | Generation number when saved |
| `training.bestFitness` | Best fitness score at save time |
| `training.averageFitness` | Population average fitness at save time |
| `training.timestamp` | ISO 8601 save timestamp |
| `training.configHash` | Hash of training config for compatibility tracking |
| `training.loadoutMix` | Loadout pool and weights used during training |
| `training.source` | How the brain was created: `"movement-training"`, `"reset"`, or `"manual"` |

## Validation on load

When loading a brain file, the following checks run:

1. **Schema version** — must match the current version (1).
2. **Mode** — must be `"movement-controller"`.
3. **Shape** — layer dimensions must match the expected network shape from config (`[input-size, ...hidden-layers, output-size]`).
4. **Weights and biases** — all values must be finite (no NaN or infinity).
5. **Network integrity** — the loaded network must pass `MovementNetwork.validate()`.

If any check fails, the load is rejected with a descriptive message and the bot falls back to legacy movement or a random network.

## Missing file behavior

If the brain file doesn't exist:

- `/ai brain load` reports that no file was found. Bots use legacy movement or a random network.
- `/ai brain save` creates the file and any parent directories.
- `/ai brain status` reports no brain loaded.
- Training sessions start with a random population regardless.

## Corrupt file behavior

If the brain file exists but is corrupt (malformed JSON, wrong schema, invalid values):

- The load reports the specific validation failure.
- The bot falls back to legacy movement — no crash, no exception in console.
- `/ai brain reset` can be used to generate a fresh brain (the corrupt file is backed up first).

## Autosave behavior

When `ai.movement-network.autosave-best-brain` is `true` (default):

- After each training generation, the best network is compared to the previously saved best.
- If `ai.movement-network.save-only-improved-brain` is `true` (default), the brain is only saved when the new best fitness exceeds the previous best.
- If `save-only-improved-brain` is `false`, the best brain from every generation is saved.

## Shape compatibility

If you change `ai.movement-network.hidden-layers` in config after training, the saved brain's shape won't match the new expected shape. On load, this triggers a shape mismatch error. Options:

1. Reset the brain with `/ai brain reset` and retrain.
2. Revert the config to match the saved brain's shape.

The `shape` field in the JSON file records the exact layer dimensions for reference.

## Backups

- `/ai brain reset` creates a timestamped backup of the existing file before overwriting.
- Manual backups are recommended before changing training config significantly — copy `brain.json` to a safe location.
