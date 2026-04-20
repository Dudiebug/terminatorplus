# Presets

Presets are YAML files that snapshot a bot's loadout **and** behavior settings. They live in:

```
plugins/TerminatorPlus/presets/<preset-name>.yml
```

## Commands

```
/bot preset save <preset> <bot>
/bot preset apply <preset> [bot]    # bot optional → apply to all
/bot preset list
/bot preset delete <preset>         # requires terminatorplus.admin
```

## What's captured

Everything a new bot needs to replay the same kit:

- **Every item slot** (0–40): hotbar, storage, armor, offhand — with full NBT.
- **Selected hotbar slot** (which item the bot is holding).
- **Behavior settings**:
  - `goal` — target-selection goal (`PLAYER`, `NEAREST`, `NEAREST_PLAYER`, etc.)
  - `mob-target` — whether hostile mobs target this bot.
  - `add-player-list` — whether new bots show up in the tab list.
  - `shield` — whether the bot is actively blocking.

## File format

```yaml
version: 1
selected-slot: 0
goal: PLAYER
mob-target: true
add-player-list: false
shield: false
items:
  0: "AQEACwEAAA... (Base64)"
  1: "AQEADAEAAA..."
  8: "AQEAAgQAAA..."
  36: "AQEABAEAAA..."
  37: "AQEABQEAAA..."
  38: "AQEABgEAAA..."
  39: "AQEABwEAAA..."
  40: "AQEACAEAAA..."
```

### Item encoding

Items are serialized with Paper's `ItemStack.serializeAsBytes()` and Base64-encoded. This preserves **everything**: display name, lore, enchantments, potion effects, durability, custom NBT. Items round-trip exactly.

Only non-empty slots are written; missing keys deserialize as `AIR`.

### Slot indices

| Range | Meaning |
| --- | --- |
| 0–8 | Hotbar |
| 9–35 | Storage |
| 36 | Boots |
| 37 | Leggings |
| 38 | Chestplate (or elytra) |
| 39 | Helmet |
| 40 | Offhand |

## Editing by hand

You can open a preset file in any text editor. To drop an item, delete its key. To add one, you need a valid Base64 blob — the easiest way to produce one is:

1. `/bot create T1`
2. Edit T1's inventory with `/bot inventory T1` (place the exact item you want).
3. `/bot preset save scratch T1`
4. Copy the relevant key from `scratch.yml` into your real preset.

## Versioning

The `version` field future-proofs the format. v1 is the current format. If a future release needs to change the layout, it will bump the number and provide a migration path.

## Applying to all bots

```
/bot preset apply mykit
```

Omitting the `bot-name` applies the preset to every currently spawned bot. Useful for "everyone switch to trident mode".

## Deleting

`/bot preset delete <name>` removes the YAML file. This is destructive, so it's gated behind `terminatorplus.admin` (see [Installation](Installation)).
