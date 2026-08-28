# Inventory GUI

> See [Legacy Status](Legacy-Status) for this page's reference status and
> [Current Strategy](Current-Strategy) for the current target.

Open one exact bot's inventory as a double-chest GUI:

```
/bot equipment inventory <bot-name>
```

Legacy path and alias: `/bot inventory <bot-name>` and `/bot inv <bot-name>`.
If more than one active bot has that name, the command refuses to guess. Select
the bot from the `/bot` management menu and use its exact-identity inventory
button instead.

## Slot map

The first 41 slots are a working copy of the bot's inventory. Equipment uses a
stable, explicit mapping; the remaining slots are controls or locked decoration.

```
Row 1 (slots  0 ..  8): hotbar (slot 0 = selected-hotbar slot index)
Row 2 (slots  9 .. 17): storage row 1
Row 3 (slots 18 .. 26): storage row 2
Row 4 (slots 27 .. 35): storage row 3
Row 5:
  36 = boots
  37 = leggings
  38 = chestplate or elytra
  39 = helmet
  40 = offhand
  41-44 = locked decoration
Row 6:
  45 = auto-equip on save
  48 = Save changes
  49 = status and exact bot UUID
  51 = Discard changes
  53 = Close (discard)
  other slots = locked decoration
```

## Interaction rules

- Normal left and right clicks in the first 41 slots edit only the working copy.
- The player's own inventory is locked while the editor is open. Use
  `/bot equipment give` to add a new stack before reopening the editor.
- Shift-click, number-key swaps, double-click collection, offhand swaps,
  creative actions, outside clicks, and drag events are cancelled.
- Armor slots accept only their matching armor class (plus elytra in the
  chest slot and carved pumpkins in the helmet slot). Invalid saves are refused.
- Locked slots cannot receive or provide items.

## Saving

Changes are isolated until **Save changes** is clicked:

1. The editor validates all equipment slots.
2. The exact selected bot is rechecked and the 41-slot snapshot is applied.
3. The selected hotbar slot is preserved unless optional auto-equip is enabled.

**Discard changes** and **Close (discard)** leave the bot untouched and restore
the item cursor to its state from when the editor opened. Closing the
GUI with Escape or `E`, disconnecting, losing permission, removing the bot, or
disabling the plugin also discards unsaved changes. There is one editor lock per
bot, so two players cannot edit the same bot concurrently.

Auto-equip is off by default. Enable it before saving only when the deterministic
combat layout (best armor, prioritized hotbar, and offhand rules) is wanted; it
may reorder items as part of that explicit save choice.

## Pairing with presets

Typical workflow:

1. `/bot spawn single T1` and `/bot equipment loadout pvp T1` — get a starting kit.
2. `/bot equipment inventory T1` — edit it and click **Save changes**.
3. `/bot preset save mykit T1` — snapshot the saved kit.

See [Presets](Presets) for how the snapshot is stored.

## Permissions

Opening and editing the GUI requires `terminatorplus.manage` (default: op).
Permission is rechecked for every action. See [Installation](Installation).
