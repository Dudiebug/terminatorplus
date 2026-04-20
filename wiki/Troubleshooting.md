# Troubleshooting / FAQ

## Updating the plugin

1. Stop the server.
2. Delete `plugins/TerminatorPlus-*.jar` (both the plugin jar and the API jar if present).
3. Download the new jar(s) from the [Releases page](https://github.com/Dudiebug/terminatorplus/releases) — choose the build matching your server version (`mc26.1.2` or `mc1.21.11`).
4. Drop the new jar(s) into `plugins/`.
5. Start the server and confirm the version line in the startup log reads `correct version: true`.

> **Never mix jars from different releases.** If you see `TerminatorPlus-API-*.jar` listed separately, make sure both the API jar and plugin jar are from the same release.

## Version mismatch banner on startup

```
[TerminatorPlus] Running on version: 1.21.X, required version: 1.21.1, correct version: false
```

Bots will spawn but the NMS subclass is targeting a different mapping. **Fix**: run Paper **1.21.1** exactly. Spigot and CraftBukkit are not supported (Paper-only API is used throughout).

## "Bot won't glide"

The `ElytraBehavior` is passive — it checks every tick for:

1. An elytra in the chestplate slot (slot 38).
2. The bot falling more than a few blocks.

If either fails, no glide. Check with:

```
/bot weapons
```

If `elytra` is dark-grey instead of yellow, the bot doesn't have one equipped. Fix with `/bot loadout skydiver` or `/bot loadout pvp`, or put an elytra in slot 38 via `/bot inventory <name>`.

For firework boosts, you also need a `FIREWORK_ROCKET` in the hotbar.

## "Bot won't throw trident"

Run `/bot weapons <bot>` to check. Common causes:
- Trident isn't in the **hotbar** (slots 0–8). Tridents in storage don't count.
- Target is outside the 5–28 block window. At < 5 the bot defaults to melee; at > 28 it prefers wind charge or pearl.
- Trident cooldown (60t) hasn't elapsed.

## "Preset didn't apply"

Scenarios:

1. **Bot is a training bot** — neural-network bots ignore the combat director entirely. Preset items still apply, but behaviors won't trigger.
2. **Preset file outdated** — check `plugins/TerminatorPlus/presets/<name>.yml`. If `version:` isn't `1`, you may be on a format that's not supported yet (unlikely at this stage).
3. **Typo in preset name** — `/bot preset list` shows all saved names. They're case-sensitive.

## "Crystal PvP / anchor bomb does nothing"

These behaviors are dimension-gated:

| Behavior | Allowed dimensions |
| --- | --- |
| Crystal PvP | Overworld, The End |
| Anchor Bomb | The Nether only |

In the wrong dimension, the director falls through to the next behavior (mace / melee / trident).

Both also require the full kit in the hotbar:
- Crystal: `END_CRYSTAL` + `OBSIDIAN` (auto-placed next to target).
- Anchor: `RESPAWN_ANCHOR` + `GLOWSTONE`.

## "Inventory GUI won't open"

- The command takes a bot name: `/bot inventory TestBot`.
- Bot must exist (`/bot count` shows all).
- You need `terminatorplus.manage` (default: op).
- The bottom row of the GUI is decorative — clicks there are cancelled.

## "I can't hit bots with weapons / bots don't take damage"

If you have a PvP plugin (e.g. WorldGuard region `pvp deny`), it may block damage to/from bots. Bots are real players to the server, so PvP-gating plugins treat them as PvP interactions.

## "Bot's /bot reset was rejected"

`/bot reset` (and `/bot preset delete`) require `terminatorplus.admin`, not just `terminatorplus.manage`. See [Installation](Installation) permissions table.

## "Firework rockets auto-fire outside combat"

`ElytraBehavior` only fires rockets while the bot is gliding. If you see them launching on the ground, the bot is transitioning between states — the auto-chestplate-swap should kick in within a few ticks. If it doesn't, verify the bot has a backup chestplate in storage (the `skydiver` and `pvp` loadouts stash one at slots 9 and 10 respectively).

## "Bot teleports to a random spot instead of my target" (ender pearl)

The pearl targets `target.getLocation() + targetVelocity * (distance/12)` — it leads moving targets. If the target stops suddenly between throw and impact, the pearl may land behind them. This is working as intended; vanilla handles the teleport destination.

## "Totem doesn't pop"

Pre-check:
- A totem must be in the **offhand** (slot 40) at the moment of fatal damage.
- `TotemBehavior` swaps it in automatically when HP drops below 6, but fatal damage faster than one tick (e.g. crystal explosion while already at 4 HP) can outrun the swap. Put a totem in slot 40 via `/bot inventory <name>` to keep one parked there permanently.

## "I'm on Paper 1.21.3 / 1.21.4"

Unsupported. The NMS `ServerPlayer` subclass is bound to 1.21.1 mappings. Run 1.21.1 or wait for a compatibility update.

## Getting help

- `/terminatorplus debuginfo` uploads a debug log to mclo.gs — share the link when reporting bugs.
- Issues: [GitHub Issues](https://github.com/HorseNuggets/TerminatorPlus/issues)
- Discord: [invite](https://discord.gg/vZVSf2D6mz)
