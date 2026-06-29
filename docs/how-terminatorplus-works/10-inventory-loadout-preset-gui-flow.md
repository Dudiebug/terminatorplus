# 10. Inventory, Loadout, Preset, and GUI Flow

This section explains how bot equipment is stored, mutated, persisted, and
edited.

## Why this area is high-risk

On Paper 26.x, fake-player inventory behavior is not safe to treat like normal
Bukkit player inventory behavior.

The most important trap is that some Bukkit container-transaction flows can
roll back inventory writes on the next tick because the bot uses
`MockConnection` and never behaves like a real client acknowledging slot
packets.

Because of that, inventory code in this repo is intentionally NMS-aware and
must not be "cleaned up" casually.

## `BotInventory` as the core equipment owner

`BotInventory.java` is the central inventory/loadout runtime class.

It tracks:

- a 41-slot logical inventory state
- selected hotbar slot
- loadout respect state
- armor/offhand behavior
- automatic movement/combat item provisioning

This class is doing more than mirroring Bukkit inventory. It is enforcing a bot
inventory model that can survive fake-player constraints.

## Selected hotbar tracking

The selected hotbar slot matters because combat timing, held item, and visible
mainhand item all depend on it.

`BotInventory` keeps explicit selected-slot state and updates both:

- internal tracked state
- the NMS/packet-facing held-item representation

Without that, the bot could "own" items that do not line up with its visible
or effective mainhand state.

## NMS-backed writes

Main inventory writes use direct NMS writes instead of ordinary Bukkit mutation
paths.

Typical reasons:

- Bukkit container transaction machinery can roll back on Paper 26.x fake
  players
- direct NMS writes avoid waiting for a client acknowledgment that will never
  happen
- bot inventory state has to stay stable enough for combat timing and loadout
  logic to trust it

This is why methods like main inventory slot setters and snapshot application
go through NMS-backed logic.

## Auto-equip logic

`BotInventory.autoEquip()` is the bridge between "what the bot owns" and
"what the bot should actively hold/wear."

It handles:

- armor equipping
- offhand decisions
- selecting a primary hotbar item
- pairing support items with primary combat items

Examples of pairing logic include:

- mace with wind-charge support
- crystals/anchors with support blocks or related utility items

This means the inventory system has combat implications without becoming the
combat brain itself.

## Movement-kit upkeep

`Bot.tick()` periodically calls `BotInventory.ensureMovementKit()`.

This replenishes movement-support items such as pearls or wind charges unless
the bot is in a mode that should strictly respect a user-applied loadout.

This is one of the easiest places to accidentally change bot behavior while
thinking you are "just cleaning up inventory logic."

## Loadouts

Loadouts represent structured equipment choices exposed through command flows
such as `/bot loadout` and `/bot loadoutmix`.

They are broader than the current duel-focused strategic core, but they are
still active command/runtime behavior.

Loadouts influence:

- what the bot carries
- what the bot equips
- how special-tech branches become available
- how training/evaluation scenarios are composed

## Presets

`PresetManager.java` persists presets to disk under:

- `plugins/TerminatorPlus/presets/*.yml`

A preset can include more than raw inventory contents.

It can also store:

- the 41-slot inventory snapshot
- selected hotbar slot
- `addToPlayerList`
- target-goal-related manager settings
- `mobTarget`

This is an important architectural fact: presets are not just inventory
templates. They can mutate broader runtime behavior when applied.

## GUI flow

`BotInventoryGUI.java` creates a chest-style GUI for editing bot inventory.

`BotInventoryListener.java` handles inventory click/close events and syncs GUI
state back to the bot on close.

The GUI sync flow is roughly:

1. player opens bot inventory GUI
2. filler slots and mirrored slots are displayed
3. player edits inventory view
4. on close, GUI contents are translated back into bot inventory state
5. main inventory is pushed through NMS-backed snapshot application
6. armor/offhand are applied through equipment-aware methods
7. `autoEquip()` and loadout-state markers are updated
8. matching same-name bots may also receive the synced inventory

This is much more than a cosmetic editor. It is a real state mutation path into
high-risk inventory code.

## Offhand behavior

Offhand handling is partly special because some support items matter tactically
even when they are not the primary combat item.

This includes things like:

- shields
- wind-charge support
- crystals or anchors with supporting utility

Because offhand choice affects visible and effective combat behavior, it is part
of the combat-support pipeline even though it is not owned by `CombatDirector`.

## What persists to disk

Runtime persistence in this area includes:

- preset YAML files

There is no evidence that every inventory mutation persists automatically. The
main persistence surface is presets, not live autosave of arbitrary bot
inventory state.

## What should not be touched without runtime tests

The following should be treated as protected:

- direct NMS write paths for main inventory
- selected hotbar slot sync
- auto-equip ordering
- GUI-to-bot sync logic
- preset application ordering
- offhand handling
- movement-kit replenishment

These are exactly the kinds of systems that can look straightforward in source
and still fail in subtle runtime-only ways.
