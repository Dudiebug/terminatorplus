# Installation

## Requirements

- **Paper 1.21.1** (tested). Spigot/CraftBukkit are not supported — TerminatorPlus uses Paper-only API (`ItemStack.serializeAsBytes`, CraftBukkit internals, NMS `ServerPlayer` subclass).
- **Java 21** (matches Paper 1.21's runtime).

## Install

1. Drop `TerminatorPlus-<version>.jar` into your server's `plugins/` directory.
2. Start (or restart) the server.
3. Verify the load banner in console:
   ```
   [TerminatorPlus] Running on version: 1.21.1, required version: 1.21.1, correct version: true
   ```
   If it shows `correct version: false`, you're on the wrong Minecraft version and bots will still spawn but may behave unexpectedly.

## File Layout

After the first run:

```
plugins/
└── TerminatorPlus/
    └── presets/
        └── mykit.yml       # created by /bot preset save
```

`presets/` stores saved bot loadouts as YAML. See [Presets](Presets) for the format.

## Permissions

The plugin declares three nodes (see `plugin.yml`):

| Node | Default | Covers |
| --- | --- | --- |
| `terminatorplus.manage` | op | Spawning, loadouts, inventory GUI, `preset save/apply/list` |
| `terminatorplus.admin`  | op | Destructive commands (`reset`, `preset delete`) |
| `terminatorplus.*`      | op | Parent node granting both of the above |

Grant to non-op staff via any permission plugin.
