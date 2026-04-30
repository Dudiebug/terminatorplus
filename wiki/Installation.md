# Installation

## Requirements

- Paper matching the jar target branch, such as Paper 26.1.2 or Paper 1.21.11.
- Java 25.
- Spigot and CraftBukkit are not supported.

## Install

1. Download the jar matching your server version from the releases page.
2. Drop `TerminatorPlus-<version>.jar` into `plugins/`.
3. Start or restart the server.
4. Check the console banner for the server version and required target version.

## File Layout

After first run and movement-bank use:

```text
plugins/
  TerminatorPlus/
    config.yml
    presets/
      mykit.yml
    ai/
      brain.json                         # optional legacy import source
      movement/
        manifest.json
        brains/
          general.json
          melee.json
          mace.json
          trident_ranged.json
          spear_melee.json
          mobility.json
          explosive_survival.json
          projectile_ranged.json
        evaluations/
          movement-eval-<timestamp>.json
```

- `config.yml`: movement bank, training, and evaluation defaults.
- `presets/`: saved bot loadouts.
- `ai/movement/`: movement brain-bank manifest, per-brain files, and evaluation
  exports.
- `ai/brain.json`: legacy import path only.

## Permissions

| Node | Default | Covers |
| --- | --- | --- |
| `terminatorplus.manage` | op | Spawning, loadouts, inventory GUI, presets, AI training, brain and evaluation commands |
| `terminatorplus.admin` | op | Destructive or diagnostic commands |
| `terminatorplus.*` | op | Parent node granting both |

## First Startup

1. Review `config.yml`.
2. Spawn a bot with `/bot create TestBot`.
3. Apply a loadout with `/bot loadout hybrid TestBot`.
4. Use `/ai brain status` to inspect movement-bank state.
