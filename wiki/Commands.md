# Commands

All bot management commands live under `/bot` (alias `/npc`). AI training is under `/ai`. Block / mob environment is under `/botenvironment`.

## Spawning

### `/bot create <name> [skin] [loc]`
Spawn one bot. `skin` defaults to a Mojang lookup of `<name>`. `loc` is either a player name or `x y z [world]`.

### `/bot multi <amount> <name> [skin] [loc]`
Spawn many bots at once.

### `/bot reset`
Remove every spawned bot. **Requires** `terminatorplus.admin`.

## Inventory

### `/bot inventory <bot-name>` (alias `inv`)
Open a 54-slot chest GUI that mirrors the bot's inventory. Edits save on close. See [Inventory GUI](Inventory-GUI).

### `/bot give <item> [bot-name] [slot]`
- One arg: legacy form — sets the default item for every bot (`ItemStack` served when the bot's hand is unset).
- Two args: drop the item into the first empty hotbar slot on the named bot.
- Three args: place into the specified inventory slot (0–8 hotbar, 9–35 storage, 36 boots, 37 legs, 38 chest, 39 head, 40 offhand).

### `/bot armor <tier>`
Apply an armor tier to every bot. Tiers: `none`, `leather`, `chain`, `gold`, `iron`, `diamond`, `netherite`.

### `/bot loadout <name> [bot-name]`
Apply a predefined combat loadout. If `bot-name` is omitted, applies to all bots. See [Loadouts](Loadouts).

### `/bot weapons [bot-name]`
Print a per-bot summary of which combat behaviors its inventory unlocks. Handy for debugging "why isn't my bot using the trident?" (answer: usually, it's not in the hotbar).

## Presets

### `/bot preset save <preset-name> <bot-name>`
Capture the bot's loadout + behavior settings into `plugins/TerminatorPlus/presets/<preset-name>.yml`.

### `/bot preset apply <preset-name> [bot-name]`
Apply a preset. If `bot-name` is omitted, applies to every spawned bot. Alias: `load`.

### `/bot preset list`
List all saved preset names.

### `/bot preset delete <preset-name>`
Delete a preset file. **Requires** `terminatorplus.admin`.

## Info

### `/bot info <bot-name>`
Print the bot's name, world, position, velocity.

### `/bot count` (alias `list`)
Count bots by name.

## Settings

### `/bot settings setgoal <goal>`
Change the global target-selection strategy. Goals: `PLAYER`, `NEAREST`, `NEAREST_PLAYER`, etc. (see `EnumTargetGoal`).

### `/bot settings mobtarget <true|false>`
Whether hostile mobs target spawned bots.

### `/bot settings addplayerlist <true|false>`
Whether newly-spawned bots appear in the tab list (and are affected by `@a`/`@p` selectors).

### `/bot settings playertarget <name>`
Set the player that bots focus on when goal is `PLAYER`.

### `/bot settings region <x1> <y1> <z1> <x2> <y2> <z2> <wX> <wY> <wZ>`
Define a region that bots prioritize. Entities inside the bounding box are preferred; entities outside have their effective distance multiplied by the weight values before target selection.

### `/bot settings region <x1> <y1> <z1> <x2> <y2> <z2> strict`
Same as above but bots **only** target entities inside the region (zero-weight strict mode).

### `/bot settings region clear`
Remove the active region.

Coordinates support `~` for relative-to-player notation. Run `/bot settings region` with no args to display the current region.

## AI Training (`/ai`)

### `/ai random <amount> <name> [skin] [loc]`
Spawn `<amount>` bots with random neural networks and begin collecting feed data.

### `/ai reinforcement <population-size> <name> [skin]`
Begin a reinforcement-learning session with a fixed population size. Player-only.

### `/ai stop`
End the current AI session.

### `/ai info <bot-name>`
Show a bot's neural-network stats.

## Environment (`/botenvironment`, alias `/botenv`)

Configure how bots understand blocks and mobs.

| Subcommand | Purpose |
| --- | --- |
| `help` | Show help. |
| `getMaterial <x> <y> <z>` | Print the block material at a location. |
| `addSolid <material>` | Add a material to the "solid" list. |
| `removeSolid <material>` | Remove. |
| `listSolids` / `clearSolids` | List or clear. |
| `addCustomMob <entity>` | Mark a mob as hostile/target-eligible. |
| `removeCustomMob` / `listCustomMobs` / `clearCustomMobs` | — |
| `mobListType <ALLOW\|DENY>` | Flip allowlist/denylist semantics. |

## Plugin

### `/terminatorplus debuginfo` (alias `/tplus debuginfo`)
Upload debug info to mclo.gs.
