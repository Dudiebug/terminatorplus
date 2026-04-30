# Commands

All bot management commands live under `/bot` (alias `/npc`). AI training is under `/ai`. Environment configuration is under `/botenvironment` (alias `/botenv`). Plugin info is under `/terminatorplus` (alias `/tplus`).

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
- One arg: sets the default item for every bot.
- Two args: drop the item into the first empty hotbar slot on the named bot.
- Three args: place into the specified inventory slot (0--8 hotbar, 9--35 storage, 36 boots, 37 legs, 38 chest, 39 head, 40 offhand).

### `/bot armor <tier>`
Apply an armor tier to every bot. Tiers: `none`, `leather`, `chain`, `gold`, `iron`, `diamond`, `netherite`.

### `/bot loadout <name> [bot-name]`
Apply a predefined combat loadout. If `bot-name` is omitted, applies to all bots. See [Loadouts](Loadouts).

### `/bot loadoutmix <mix> [bot-prefix]`
Apply rotating combat loadouts across bots. Each bot gets a different loadout from the mix.

| Mix | Loadouts |
| --- | --- |
| `alltypes` / `all` / `balanced` | All 14 loadout types distributed evenly |
| `core` | `sword`, `axe`, `smp`, `mace`, `trident`, `spear`, `pot` |
| `problem` / `combatdata` / `bugs` | `mace` (3x), `axe` (3x), `smp` (2x), `vanilla`, `hybrid` |

### `/bot weapons [bot-name]`
Print a per-bot summary of which combat behaviors its inventory unlocks. Useful for debugging "why isn't my bot using the trident?" (answer: usually, it's not in the hotbar).

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
Change the global target-selection strategy. Goals: `PLAYER`, `NEAREST`, `NEAREST_PLAYER`, etc.

### `/bot settings mobtarget <true|false>`
Whether hostile mobs target spawned bots.

### `/bot settings addplayerlist <true|false>`
Whether newly-spawned bots appear in the tab list (and are affected by `@a`/`@p` selectors).

### `/bot settings playertarget <name>`
Set the player that bots focus on when goal is `PLAYER`.

### `/bot settings region <x1> <y1> <z1> <x2> <y2> <z2> [<wX> <wY> <wZ>|strict]`
Set region for bot prioritization.

## Utility

### `/bot gather` (alias `tpall`)
Teleport all bots to your location.

### `/bot combatdebug <name|all> <on|off>` (aliases `cdbg`, `comatdebug`)
Toggle combat trace logging for specific bots or all bots. Shows telemetry fields like `critPred`, `sweepPred`, `chargeAtVanillaAttack`, `targetHp`, and `targetHpDelta`. **Requires** `terminatorplus.admin`.

## AI Training (`/ai`)

### `/ai reinforcement <population-size> <name> [skin] [mode-or-options]`
Begin a training session. Must be run as a player.

- Empty mode defaults to **movement-controller** training.
- `mode` may be `movement`, `movement-controller`, or `legacy`.
- In **movement-controller** mode, the NN controls movement only and the CombatDirector handles combat.
- In **legacy** mode, the NN fully replaces both movement and combat (the original training pipeline).
- Movement mode automatically samples weighted training loadouts from `ai.training.loadout-mix`.
- Use options such as `family=mace:mix=mace_curriculum` or `movement:family=mace:mix=mace_curriculum` for curriculum runs.

### `/ai random <amount> <name> [skin] [loc]`
Spawn bots with random neural networks.

### `/ai movement <amount> <name> [skin] [loc]`
Spawn movement-controller bots that use the loaded movement brain bank.

### `/ai brain <status|load|save|reset> [bot-name]`
Manage movement brain-bank persistence.

| Subcommand | Effect |
| --- | --- |
| `status` | Show manifest/schema/fallback state, route table version, missing experts, loadout mix, and metadata |
| `load` | Load the manifest and per-brain files |
| `save` | Save the current bank, or a named bot's movement bank |
| `reset` | Generate a fresh `general_fallback` bank and back up existing files |

### `/ai evaluate [variant] [scenario] [seed[,seed...]]`
Export a movement-brain evaluation report under `ai/movement/evaluations/`.

Useful variants:

| Variant | Status |
| --- | --- |
| `general_brain` | Report-only route/fallback probe |
| `branch_family_latched` | Report-only probe of the current router |
| `legacy` | Pending live-arena runner |
| `weapon_family` | Unsupported in the branch-family architecture |
| `branch_family_no_latch` | Unsupported until latching can be disabled for ablation |

Use `/ai evaluate list` to print all variants and scenarios.

### `/ai info <bot-name>`
Display neural network info about a specific bot.

### `/ai stop`
End the current AI training session.

## Environment (`/botenvironment`, alias `/botenv`)

Configure how bots understand blocks and mobs.

| Subcommand | Purpose |
| --- | --- |
| `help [blocks\|mobs]` | Show help |
| `getMaterial <x> <y> <z>` | Print the block material at a location (player only) |
| `addSolid <material>` | Add a material to the "solid" list |
| `removeSolid <material>` | Remove a material from the solid list |
| `listSolids` / `clearSolids` | List or clear custom solid materials |
| `addCustomMob <entity>` | Mark a mob as target-eligible |
| `removeCustomMob <entity>` | Remove a custom mob |
| `listCustomMobs` / `clearCustomMobs` | List or clear custom mobs |
| `mobListType <mode>` | Change custom mob list behavior |

## Plugin

### `/terminatorplus debuginfo` (alias `/tplus debuginfo`)
Upload debug info to mclo.gs. Share the link when reporting bugs.
