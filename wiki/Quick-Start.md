# Quick Start

A 60-second tour of TerminatorPlus.

## 1. Spawn a bot

```
/bot create TestBot
```

The bot spawns at your feet. Skin defaults to a Mojang lookup of the name.

## 2. Give it a combat loadout

```
/bot loadout hybrid TestBot
```

The `hybrid` loadout gives netherite armor, a sword, a mace, a trident, 16 wind charges, 4 golden apples, and a shield. See [Loadouts](Loadouts) for the full catalog.

## 3. Watch it fight

Punch the bot (or set it to target you). It will:

- Attack with the sword at melee range.
- Jump-smash with the mace when grounded and in range.
- Sprint and throw the trident from 5--28 blocks away.
- Lob wind charges beyond 30 blocks.
- Eat a golden apple when it drops below 40% HP.

## 4. Edit its inventory

```
/bot inventory TestBot
```

A double-chest GUI opens. The top 5 rows mirror the bot's inventory (hotbar, storage, armor, offhand). Edit items, drag in stacks, then close — changes are saved automatically.

## 5. Save a preset

```
/bot preset save mykit TestBot
```

A file appears at `plugins/TerminatorPlus/presets/mykit.yml`. It captures every item slot, the selected hotbar index, and behavior settings.

## 6. Apply to a fresh bot

```
/bot create T2
/bot preset apply mykit T2
```

T2 now mirrors TestBot's loadout.

## 7. Check weapon behaviors

```
/bot weapons
```

Prints a per-bot summary of which combat behaviors the bot's inventory unlocks.

## 8. Try movement-controller training

```
/ai reinforcement 120 TrainBot
```

Spawns 120 bots with random movement networks. They fight each other while the genetic algorithm evolves better footwork. The CombatDirector still handles all combat decisions — the NN only controls movement.

```
/ai brain status        # check current brain state
/ai brain save          # save the best brain to disk
/ai stop                # end the training session
```

## Next

- [Commands](Commands) — full command reference
- [Combat Behaviors](Combat-Behaviors) — how each weapon behavior triggers
- [Loadouts](Loadouts) — all 15 built-in kits
- [Movement Network](Movement-Network) — how the movement NN works
- [AI Training](AI-Training) — training guide and configuration
