# Troubleshooting / FAQ

## Version mismatch on startup

If the console shows a version mismatch, your server version doesn't match the jar's target build. Bots may spawn but behave unexpectedly. **Fix**: run the Paper version that matches your jar (26.1.2 or 1.21.11). Spigot and CraftBukkit are not supported.

## Bot does not move

Possible causes:
- **Movement NN fallback**: if the movement network is enabled but the brain file is missing or corrupt, the bot may fall back to legacy movement. Check with `/ai brain status`. Fix with `/ai brain load` or `/ai brain reset`.
- **No target**: bots need a target to move toward. Check the target goal with `/bot settings setgoal`.
- **Hold-position**: the CombatDirector may be requesting hold-position. This is normal during certain combat phases.

## Movement NN falls back to legacy

The movement network falls back to legacy movement when:
- The brain file is missing or corrupt.
- The network shape doesn't match the config (e.g. `hidden-layers` was changed after training).
- Input validation fails.

Check `/ai brain status` for details. Fix with `/ai brain load` (if a valid file exists) or `/ai brain reset` followed by retraining.

## Brain file missing

- Default location: `plugins/TerminatorPlus/ai/brain.json`
- Check the configured path in `config.yml` under `ai.movement-network.brain-path`.
- Use `/ai brain save` to create the file after training, or `/ai brain reset` to generate a random brain.

## Brain file corrupt

If the brain file contains invalid JSON, wrong schema version, or NaN values:
- The load is rejected with a descriptive error message.
- Bots fall back to legacy movement — no crash.
- Use `/ai brain reset` to generate a fresh brain. The corrupt file is backed up automatically.

## Shape mismatch

If `ai.movement-network.hidden-layers` was changed after a brain was saved, the saved brain's shape won't match:
- The load reports a shape mismatch error.
- Either revert the config to match the saved brain, or `/ai brain reset` and retrain.

## Training causes lag

Training spawns many bots fighting simultaneously. To reduce server load:
- Lower `ai.training.population-size` (try 60--80).
- Increase `ai.movement-network.tick-rate` (evaluate every 2--3 ticks instead of every tick).
- Ensure the training arena is in a pre-generated area to avoid chunk loading overhead.

## Commands not found

- Make sure you're running the correct jar version.
- Check permissions: most commands require `terminatorplus.manage` (default: op).
- `/bot reset`, `/bot preset delete`, and `/bot combatdebug` require `terminatorplus.admin`.

## Loadout name invalid

Loadout names are case-sensitive. Valid names: `sword`, `mace`, `trident`, `windcharge`, `skydiver`, `hybrid`, `crystalpvp`, `anchorbomb`, `pvp`, `vanilla`, `axe`, `smp`, `pot`, `spear`, `clear`.

## Bot won't glide

`ElytraBehavior` checks every tick for:
1. An elytra in the chestplate slot (slot 38).
2. The bot falling more than a few blocks.

Check with `/bot weapons`. If elytra is not listed, equip one via `/bot loadout skydiver` or `/bot inventory <name>`.

For firework boosts, you also need `FIREWORK_ROCKET` in the hotbar.

## Bot won't throw trident

Common causes:
- Trident isn't in the **hotbar** (slots 0--8). Tridents in storage don't count.
- Target is outside the 5--28 block window. At < 5 the bot defaults to melee; at > 28 it prefers wind charge or pearl.
- Trident cooldown (60t) hasn't elapsed.

Check with `/bot weapons <bot>`.

## Mace/trident behavior seems odd

- **Mace**: the bot needs to be grounded and within 3.5 blocks. It jumps, waits for peak, then dives. If the target moves away during the jump, the bot may abort.
- **Trident**: the bot sprints toward the target before throwing (momentum build-up phase). This looks like a brief charge-up delay.
- **Charge timing**: the CombatDirector waits for full attack charge (0.95) before swinging, and smash-ready charge (0.848) for mace. This prevents low-damage swings but may look like hesitation.

## Crystal PvP / anchor bomb does nothing

These are dimension-gated:

| Behavior | Allowed dimensions |
| --- | --- |
| Crystal PvP | Overworld, The End |
| Anchor Bomb | The Nether only |

Both also require the full kit in the hotbar (Crystal: `END_CRYSTAL` + `OBSIDIAN`; Anchor: `RESPAWN_ANCHOR` + `GLOWSTONE`).

## Preset didn't apply

1. **Bot is a legacy training bot** — full-replacement NN bots bypass the combat director. Items apply, but weapon behaviors won't trigger.
2. **Preset file format** — check `plugins/TerminatorPlus/presets/<name>.yml` has `version: 1`.
3. **Typo in preset name** — `/bot preset list` shows all saved names. Case-sensitive.

## Inventory GUI won't open

- Provide a bot name: `/bot inventory TestBot`.
- Bot must exist (`/bot count`).
- You need `terminatorplus.manage` (default: op).
- The bottom row of the GUI is decorative — clicks there are cancelled.

## Bots don't take damage from players

If a PvP plugin (e.g. WorldGuard region `pvp deny`) is active, it may block damage to/from bots. Bots are real `ServerPlayer` entities, so PvP-gating plugins treat them as PvP interactions.

## Totem doesn't pop

- A totem must be in the **offhand** (slot 40) at the moment of fatal damage.
- `TotemBehavior` auto-swaps at HP < 6, but instant-kill damage can outrun the swap. Park a totem at slot 40 via `/bot inventory <name>`.

## Firework rockets fire outside combat

`ElytraBehavior` only fires rockets while gliding. Ground launches indicate a brief state transition — the auto-chestplate-swap should kick in within a few ticks. Verify a backup chestplate exists in storage.

## Combat debug telemetry

Enable with `/bot combatdebug <name|all> on`. Outputs per-tick fields to console for diagnosing combat issues. See [Combat Behaviors](Combat-Behaviors) for field descriptions.

## Getting help

- `/terminatorplus debuginfo` uploads a debug log to mclo.gs — share the link when reporting bugs.
- Issues: [GitHub Issues](https://github.com/Dudiebug/terminatorplus/issues)
- Discord: [invite](https://discord.gg/vZVSf2D6mz)
