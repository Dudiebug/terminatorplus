# Inventory GUI

Open any bot's inventory as a double-chest GUI:

```
/bot inventory <bot-name>
```

Alias: `/bot inv <bot-name>`.

## Slot map

The top 5 rows (45 slots) mirror the bot's real inventory. The bottom row (9 slots) is locked — it's reserved decoration.

```
Row 1 (slots  0 .. 8): Hotbar (slot 0 = selected-hotbar slot index)
Row 2 (slots  9 ..17): Storage row 1
Row 3 (slots 18 ..26): Storage row 2
Row 4 (slots 27 ..35): Storage row 3
Row 5 (slots 36 ..44): Armor + offhand
  36 = boots
  37 = leggings
  38 = chestplate (or elytra)
  39 = helmet
  40 = offhand
  41-44 = empty / locked
Row 6 (slots 45 ..53): locked (decorative glass panes)
```

## Interaction rules

- **Click / drag**: normal Bukkit inventory behavior, with the usual stack-splitting shortcuts.
- **Shift-click**: moves items between hotbar and storage as normal.
- **Armor slots**: only accept the matching material class — putting a sword in the helmet slot is refused by vanilla.
- **Locked slots**: any attempt to drop items into the bottom row is cancelled server-side.

## Saving

Close the GUI (Escape / `E`) to push changes back to the bot:

1. Reads all 45 editable slots.
2. Diffs against the bot's current inventory.
3. Writes back via NMS `Inventory.setItem` so NBT (enchantments, custom names, durability) survives.
4. Re-syncs the selected hotbar slot via packet so the bot's main hand reflects the change.

There's no explicit "save" button — the close handler does it.

## Why 54 slots?

Bukkit chest inventories come in 27- or 54-slot sizes. 45 slots of editable space (5 rows) fit the 41-slot bot inventory with one spare row, and using a double chest gives you the visual affordance of "this is bigger than my own inventory" so it's obvious you're looking at the bot.

## Pairing with presets

Typical workflow:

1. `/bot create T1 + /bot loadout pvp T1` — get a starting kit.
2. `/bot inventory T1` — fine-tune (add custom enchanted gear, edit durability, etc.).
3. `/bot preset save mykit T1` — snapshot.

See [Presets](Presets) for how the snapshot is stored.

## Permissions

Opening the GUI requires `terminatorplus.manage` (default: op). See [Installation](Installation).
