# Changelog

## Unreleased — Combat + Inventory + Presets Overhaul

### Added

**Combat AI**
- Weapon-aware `CombatDirector` that picks the right behavior per tick based on inventory, distance, cooldowns, and dimension.
- **Melee** behavior using vanilla `Player.attack()` (real crits, shields, enchants).
- **Mace smash**: jump → peak → fall-damage-scaled smash.
- **Trident momentum throw**: sprint run-up builds velocity, released spear keeps the stacked momentum (5–28 block window).
- **Wind charge**: zoning lob at ≥ 4 blocks.
- **Ender pearl**: gap-closer at 14–35 blocks, leads target velocity.
- **End crystal PvP**: auto-places obsidian host block, spawns crystal, detonates (Overworld / End only).
- **Respawn anchor bomb**: Nether-only place/charge/detonate chain.
- **Cobweb utility**: drops web at feet of fleeing target (≤ 4.5 blocks).
- **Elytra glide** (passive): auto-activates when falling, uses firework rockets, dive-trident combo.
- **Elytra ↔ chestplate swap**: bot swaps automatically when both are present and state changes.
- **Totem of undying** (passive): swaps a totem into the offhand when HP < 6 so vanilla pop triggers.
- Neural-network bots bypass the director entirely to preserve deterministic fitness.

**Inventory**
- Full per-bot inventory: 9 hotbar + 27 storage + 4 armor + 1 offhand slots, each independently editable.
- `/bot inventory <name>` — double-chest GUI mirrors the bot's inventory; edits save on close.
- `/bot give <item> [bot] [slot]` — drop items into specific slots of specific bots.
- `/bot armor <tier>` — quick full-armor apply.
- `/bot weapons [bot]` — prints which combat behaviors each bot's inventory unlocks.

**Loadouts**
- Built-in loadouts: `sword`, `mace`, `trident`, `windcharge`, `skydiver`, `hybrid`, `crystalpvp`, `anchorbomb`, `pvp`, `clear`.
- `/bot loadout <name> [bot]` — optional bot target (previously always all-bots).

**Presets**
- YAML preset system: captures all 41 slots + selected hotbar + behavior settings (goal, mob-target, player-list, shield).
- Items serialized via `ItemStack.serializeAsBytes` + Base64 — full NBT round-trip.
- Commands: `/bot preset save`, `apply`, `list`, `delete`.
- `apply` without a bot-name applies to every spawned bot.
- `load` kept as an alias for `apply`.

**Permissions**
- `terminatorplus.admin` for destructive commands (`reset`, `preset delete`).
- `terminatorplus.manage` for day-to-day bot management.
- `terminatorplus.*` parent node.
- All three declared with descriptions in `plugin.yml`.

**API**
- `Terminator.combatTick(LivingEntity)` — drive the new combat director from your own plugin.
- Dimension awareness via `Terminator.getDimension()`.

**Wiki**
- Full documentation set under `wiki/`: Home, Installation, Quick Start, Commands, Loadouts, Combat Behaviors, Presets, Inventory GUI, Neural Network Mode, API, Troubleshooting, Changelog.

### Changed

- `/bot loadout` is now per-bot by default when a bot-name is passed, else applies to all.
- `/bot give` accepts `[bot] [slot]` for targeted placement. Single-arg form (set default item on every bot) still works.
- `plugin.yml` now declares the `bot`, `terminatorplus`, `ai`, `botenvironment` commands with aliases and descriptions so `/help` works correctly.
- Normal bots now take and deal vanilla damage (real crits, shield breaks, enchantment effects). Only neural-network training bots still use the legacy deterministic damage table.

### Fixed

- Bots now correctly switch hotbar slots when the combat director chooses a weapon — main hand reflects the active item via packet sync.
- Fall damage on mace smash now stacks against the target, not the bot.

### Known limitations

- Paper 1.21.1 only. Other versions spawn bots but may break.
- Trained neural-network weights aren't persisted across server restarts.
- Crystal PvP doesn't place obsidian in The End if the arena uses end stone only — bring your own blocks.

## Previous versions

Pre-overhaul. See Git history.
