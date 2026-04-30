# Loadouts

Loadouts are named, built-in kits applied via `/bot loadout <name> [bot-name]`. They write a full 41-slot layout to the bot: hotbar, storage, armor, and offhand.

If `bot-name` is omitted, the kit is applied to every spawned bot.

## Slot map

| Slot | Meaning |
| ---: | --- |
| 0--8 | Hotbar |
| 9--35 | Storage |
| 36 | Boots |
| 37 | Leggings |
| 38 | Chestplate or elytra |
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
- Hotbar 0: Wind Charge x16
- Hotbar 1: Iron Sword
- Armor: full Iron

### `skydiver`
The elytra-trident combo. Bot glides, fires rockets, and dives to spear targets. Chestplate is stored at slot 9; `ElytraBehavior` auto-swaps to it when on the ground.
- Hotbar 0: Trident
- Hotbar 1: Iron Sword
- Hotbar 2: Firework Rocket x8
- Storage 9: Diamond Chestplate
- Armor: Diamond boots/legs/helmet, Elytra on body

### `hybrid`
Multi-weapon all-rounder: the "punch this bot" kit from [Quick Start](Quick-Start).
- Hotbar 0: Netherite Sword
- Hotbar 1: Mace
- Hotbar 2: Trident
- Hotbar 3: Wind Charge x16
- Hotbar 8: Golden Apple x4
- Armor: full Netherite
- Offhand: Shield

### `crystalpvp`
Crystal-burst kit for the Overworld or End.
- Hotbar 0: Netherite Sword
- Hotbar 1: End Crystal x32
- Hotbar 2: Obsidian x32
- Hotbar 3: Ender Pearl x16
- Hotbar 4: Golden Apple x8
- Hotbar 7: Totem of Undying
- Armor: full Netherite
- Offhand: Totem of Undying

### `anchorbomb`
Nether detonation kit.
- Hotbar 0: Netherite Sword
- Hotbar 1: Respawn Anchor x16
- Hotbar 2: Glowstone x32
- Hotbar 3: Ender Pearl x16
- Hotbar 4: Fire Resistance Potion
- Hotbar 7: Totem of Undying
- Armor: full Netherite
- Offhand: Totem of Undying

### `pvp`
Full arsenal; every behavior unlocked simultaneously.
- Hotbar 0--8: Sword, Mace, Trident, Wind Charge x16, Pearl x16, Crystal x16, Obsidian x32, Cobweb x16, Golden Apple x8
- Storage 9: Firework Rocket x16
- Storage 10: Diamond Chestplate
- Armor: Netherite boots/legs/helmet, Elytra on body
- Offhand: Totem of Undying

### `vanilla`
Full arsenal minus elytra; anchors are stashed in storage for Nether use.
- Hotbar 0--8: Sword, Mace, Crystal x16, Obsidian x32, Wind Charge x16, Pearl x16, Golden Apple x8, Cobweb x8, Totem
- Storage 9: Respawn Anchor x16
- Storage 10: Glowstone x16
- Armor: full Netherite
- Offhand: Shield

### `axe`
Shield-breaking axe PvP; axe disables shields, sword as secondary.
- Hotbar 0: Netherite Axe
- Hotbar 1: Netherite Sword
- Hotbar 2: Golden Apple x4
- Armor: full Netherite
- Offhand: Shield

### `smp`
SMP-style netherite PvP; sword primary, axe fallback. No mace/crystals/anchors.
- Hotbar 0: Netherite Sword
- Hotbar 1: Netherite Axe
- Hotbar 2: Golden Apple x4
- Armor: full Netherite
- Offhand: Shield

### `pot`
Splash healing PvP; healing potions are the core mechanic. No shield.
- Hotbar 0: Netherite Sword
- Hotbar 1--4: Splash Potion of Strong Healing
- Hotbar 5: Ender Pearl x4
- Hotbar 6: Golden Apple x4
- Armor: full Netherite

### `spear`
Trident-only melee in heavy armor.
- Hotbar 0: Trident
- Hotbar 1: Golden Apple x4
- Armor: full Netherite
- Offhand: Shield

### `clear`
Wipes every slot. Useful before hand-crafting a kit with `/bot give` or the [Inventory GUI](Inventory-GUI).

## Loadout Mixes

`/bot loadoutmix <mix> [bot-prefix]` distributes different loadouts across spawned bots. This is for live spawned bots; movement training uses the separate weighted pool below.

| Mix name | Aliases | Loadouts included |
| --- | --- | --- |
| `alltypes` | `all`, `balanced` | All 14 combat loadout types evenly distributed |
| `core` | none | `sword`, `axe`, `smp`, `mace`, `trident`, `spear`, `pot` |
| `problem` | `combatdata`, `bugs` | `mace` (3x), `axe` (3x), `smp` (2x), `vanilla`, `hybrid` |

## Movement Training Loadout Pool

Movement-controller training does not use `/bot loadoutmix`. `/ai reinforcement`
samples named loadouts automatically from the weighted mix selected by
`ai.training.loadout-mix`; the named mixes live under
`ai.training.loadout-mixes` in `config.yml`.

The default `movement_balanced` mix is intentionally not equal weight:

```yaml
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

`pvp`, `crystalpvp`, and `anchorbomb` total 8% so explosive movement gets
coverage without turning every generation into crystal/anchor chaos.

Default loadout-to-training-family mapping:

| Loadouts | Training family |
| --- | --- |
| `sword`, `axe`, `smp`, `pot` | `melee` |
| `mace` | `mace` |
| `spear` | `spear_melee` |
| `trident` | `trident_ranged` |
| `windcharge`, `skydiver`, `hybrid` | `mobility` |
| `crystalpvp`, `anchorbomb`, `pvp` | `explosive_survival` |
| anything unknown | `general_fallback` |

In mixed mode, each training bot is seeded from and scored against its assigned
loadout family. A family brain is saved only when that round captured matching
route samples for that family. Curriculum mode overrides that and trains the
configured `family=<name>` for all candidates.

## Saving your own

Edit a bot's inventory with `/bot inventory <name>` and then run `/bot preset save <name> <bot>`. That YAML file can be re-applied via `/bot preset apply <name> [bot]`. See [Presets](Presets) for the file format.
