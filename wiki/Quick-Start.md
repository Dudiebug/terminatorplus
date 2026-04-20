# Quick Start

A 60-second tour of TerminatorPlus.

## 1. Spawn a bot

```
/bot create TestBot
```

The bot spawns at your feet. Skin defaults to a lookup of the name from Mojang.

## 2. Give it a combat loadout

```
/bot loadout hybrid TestBot
```

The `hybrid` loadout gives netherite armor, a sword, a mace, a trident, 16 wind charges, 4 golden apples, and a shield. See [Loadouts](Loadouts) for the full catalog.

## 3. Watch it fight

Punch the bot (or run at it while set as a target). It will:

- Attack with the sword at melee range.
- Jump-smash with the mace when it's on the ground and can hit you (fall-damage scaling applies).
- Sprint toward you and throw the trident when you're 5–28 blocks away (spear momentum is amplified by the run-up).
- Lob wind charges when you get further than ~30 blocks.
- Eat a golden apple when it drops below 40% HP.

## 4. Edit its inventory visually

```
/bot inventory TestBot
```

A double-chest GUI opens. The top 5 rows mirror the bot's real inventory (hotbar, storage, armor, offhand). The bottom row is locked. Edit items, drag in stacks, then close — your changes are pushed back to the bot.

## 5. Save a preset

```
/bot preset save mykit TestBot
```

A file appears at `plugins/TerminatorPlus/presets/mykit.yml`. It captures:

- Every item slot (hotbar, storage, armor, offhand) with full NBT.
- Selected hotbar slot.
- Behavior settings: target goal, mob-target flag, player-list flag, shield state.

## 6. Apply the preset to a fresh bot

```
/bot create T2
/bot preset apply mykit T2
```

T2 now mirrors TestBot's loadout.

## 7. See which behaviors each bot unlocks

```
/bot weapons
```

Prints a per-bot summary of which combat behaviors its inventory makes available (mace, trident, pearl, crystal, anchor, cobweb, totem, elytra, firework).

## Next

- [Commands](Commands) — full `/bot` reference
- [Combat Behaviors](Combat-Behaviors) — what triggers each weapon and why
- [Loadouts](Loadouts) — all built-in kits
