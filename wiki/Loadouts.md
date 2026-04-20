# Loadouts

Loadouts are named, built-in kits applied via `/bot loadout <name> [bot-name]`. They write a full 41-slot layout to the bot (hotbar + storage + armor + offhand).

If `bot-name` is omitted, the kit is applied to every spawned bot.

## Slot map

| Slot | Meaning |
| ---: | --- |
| 0–8 | Hotbar |
| 9–35 | Storage |
| 36 | Boots |
| 37 | Leggings |
| 38 | Chestplate (or elytra) |
| 39 | Helmet |
| 40 | Offhand |

## Catalog

### `sword`
Vanilla duel kit.
- Hotbar 0: Netherite Sword
- Armor: full Netherite
- Offhand: Shield

### `mace`
Mace-smash specialist with a sword fallback.
- Hotbar 0: Mace
- Hotbar 1: Iron Sword
- Armor: full Netherite

### `trident`
Spear-thrower in light armor.
- Hotbar 0: Trident
- Hotbar 1: Iron Sword
- Armor: full Iron

### `windcharge`
Zoning kit for long ranges.
- Hotbar 0: Wind Charge ×16
- Hotbar 1: Iron Sword
- Armor: full Iron

### `skydiver`
The elytra-trident combo. Bot glides, fires rockets, dives to spear targets. Chestplate is stored at slot 9 — `ElytraBehavior` auto-swaps to it when on the ground.
- Hotbar 0: Trident
- Hotbar 1: Iron Sword
- Hotbar 2: Firework Rocket ×8
- Storage 9: Diamond Chestplate (swap target)
- Armor: Diamond boots/legs/helmet, Elytra on body

### `hybrid`
The "Punch this bot" kit from [Quick Start](Quick-Start).
- Hotbar 0: Netherite Sword
- Hotbar 1: Mace
- Hotbar 2: Trident
- Hotbar 3: Wind Charge ×16
- Hotbar 8: Golden Apple ×4
- Armor: full Netherite
- Offhand: Shield

### `crystalpvp`
Crystal-burst kit for the Overworld / End.
- Hotbar 0: Netherite Sword
- Hotbar 1: End Crystal ×32
- Hotbar 2: Obsidian ×32
- Hotbar 3: Ender Pearl ×16
- Hotbar 4: Golden Apple ×8
- Hotbar 7: Totem of Undying
- Armor: full Netherite
- Offhand: Totem of Undying

### `anchorbomb`
Nether detonation kit.
- Hotbar 0: Netherite Sword
- Hotbar 1: Respawn Anchor ×16
- Hotbar 2: Glowstone ×32
- Hotbar 3: Ender Pearl ×16
- Hotbar 4: Potion (fire resistance — bring your own enchanted potion)
- Hotbar 7: Totem of Undying
- Armor: full Netherite
- Offhand: Totem of Undying

### `pvp`
Everything bagel. Unlocks every behavior simultaneously so you can see the combat director pick the right weapon at every range.
- Hotbar 0–8: Sword, Mace, Trident, Wind Charge ×16, Pearl ×16, Crystal ×16, Obsidian ×32, Cobweb ×16, Golden Apple ×8
- Storage 9: Firework Rocket ×16
- Storage 10: Diamond Chestplate (elytra swap target)
- Armor: Netherite boots/legs/helmet, Elytra on body
- Offhand: Totem of Undying

### `clear`
Wipes every slot. Useful before hand-crafting a kit with `/bot give` or the [Inventory GUI](Inventory-GUI).

## Saving your own

Edit a bot's inventory (`/bot inventory <name>`) and then `/bot preset save <name> <bot>`. That YAML file can be re-applied to any bot via `/bot preset apply <name> [bot]`. See [Presets](Presets) for the file format.
