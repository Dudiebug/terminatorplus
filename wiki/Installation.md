# Installation

## Requirements

- **Paper 26.1.2** or **Paper 1.21.11** (version-specific jars are published on the releases page).
- **Java 25** (matches the Paper runtime).
- Spigot and CraftBukkit are **not** supported — TerminatorPlus uses Paper-only APIs and NMS internals.

## Install

1. Download the jar matching your server version from the [releases page](https://github.com/Dudiebug/terminatorplus/releases).
2. Drop `TerminatorPlus-<version>.jar` into your server's `plugins/` directory.
3. Start (or restart) the server.
4. Verify the load banner in console — it shows whether your server version matches the build target.

## File Layout

After the first run:

```
plugins/
└── TerminatorPlus/
    ├── config.yml          # generated on first startup
    ├── presets/
    │   └── mykit.yml       # created by /bot preset save
    └── ai/
        └── brain.json      # created by /ai brain save
```

- `config.yml` — all plugin settings including neural network, training, and fitness configuration.
- `presets/` — saved bot loadouts as YAML. See [Presets](Presets).
- `ai/brain.json` — persisted movement network brain. See [Brain Persistence](Brain-Persistence).

## Permissions

The plugin declares three permission nodes (see `plugin.yml`):

| Node | Default | Covers |
| --- | --- | --- |
| `terminatorplus.manage` | op | Spawning, loadouts, inventory GUI, presets (save/apply/list), AI training, brain commands |
| `terminatorplus.admin`  | op | Destructive commands (`reset`, `preset delete`, `combatdebug`) |
| `terminatorplus.*`      | op | Parent node granting both of the above |

Grant to non-op staff via any permission plugin.

## First Startup

After the plugin loads for the first time:

1. Review `config.yml` — neural network features are disabled by default for normal bots.
2. Try `/bot create TestBot` to spawn your first bot.
3. See [Quick Start](Quick-Start) for a guided walkthrough.

## Troubleshooting

If the plugin fails to load or bots behave unexpectedly:

- Run `/terminatorplus debuginfo` to upload a debug log to mclo.gs.
- Check that your Paper version matches the jar's target version.
- See [Troubleshooting](Troubleshooting) for common issues.
