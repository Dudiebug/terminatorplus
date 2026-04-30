# Troubleshooting / FAQ

## Version Mismatch

The jar must match the target Paper branch, such as 26.1.2 or 1.21.11. Spigot
and CraftBukkit are not supported.

## Movement-Bank Bot Does Not Move

Possible causes:

- The bot was spawned with `/bot create`, which uses legacy movement. Use
  `/ai movement` to spawn movement-bank bots.
- The movement bank is disabled or missing a valid fallback. Check
  `/ai brain status`.
- The target goal gives the bot no target. Check `/bot settings setgoal`.
- `CombatDirector` requested hold-position for a committed phase.

## Specialist Brain Falls Back

Missing or incompatible specialist files route through `general_fallback`.
`/ai brain status` lists missing optional experts and schema/fallback state.

Fix options:

- `/ai brain load`
- `/ai brain reset`
- retrain with `/ai reinforcement ... family=<family>`

## Legacy Brain Import

`ai/brain.json` is only a legacy import path. Compatible files import as
`general_fallback`; incompatible files are backed up or quarantined depending on
config.

## Shape or Schema Mismatch

The current movement shape is `[37, 32, 16, 8]` by default. If
`ai.movement.layer-shape` changes after training, existing brains may fail shape
validation. Reset or retrain the bank, or restore the previous shape.

## Evaluation Metrics Are Null

`/ai evaluate` currently exports a report-only route/fallback initializer. Live
combat metrics such as win rate, damage delta, self-damage, mace connects, and
trident hit rate remain `null` until an arena runner records them.

## Training Causes Lag

Training spawns many real `ServerPlayer` bots. Reduce population size, use a
time limit argument, and run in a pre-generated arena.

## Commands Not Found

Most commands require `terminatorplus.manage`; destructive diagnostics require
`terminatorplus.admin`.

## Loadout Name Invalid

Valid built-in names include `sword`, `axe`, `smp`, `pot`, `mace`, `spear`,
`trident`, `windcharge`, `skydiver`, `hybrid`, `vanilla`, `pvp`,
`crystalpvp`, `anchorbomb`, and `clear`.

## Bot Will Not Glide

Use `/bot weapons <name>` and confirm the bot has an elytra in chest slot 38 and
firework rockets in the hotbar.

## Bot Will Not Throw Trident

The trident must be in the hotbar, the target must be in the configured throw
window, and trident cooldown must have elapsed.

## Crystal or Anchor Behavior Does Nothing

Crystal PvP is Overworld/End gated. Anchor bombing is Nether gated. Both require
their setup items in the hotbar.

## Getting Help

Run `/terminatorplus debuginfo` and share the mclo.gs link with the issue.

