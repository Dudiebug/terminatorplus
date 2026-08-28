# Commands

> See [Legacy Status](Legacy-Status) for this page's reference status and
> [Current Strategy](Current-Strategy) for the current target.


All bot management commands live under `/bot` (alias `/npc`). AI training is under `/ai`. Environment configuration is under `/botenvironment` (alias `/botenv`). Plugin info is under `/terminatorplus` (alias `/tplus`).

## Canonical command layout

New command paths use the following groups. The older flat paths documented below remain available as compatibility aliases.

```text
/bot spawn single|multiple
/bot inspect list|info|weapons
/bot move gather|scatter
/bot settings combat-goal|target-mobs|target-player|target-region|show-in-player-list|auto-respawn|movement-v2|placement-material
/bot equipment inventory|give|armor|loadout|mixed-loadout
/bot preset save|apply|list|delete
/bot debug behavior|combat|movement
/bot admin reset
/bot environment inspect material
/bot environment solid-block add|remove|list|clear
/bot environment custom-mob add|remove|list|clear
/bot environment mob-list-mode get|set

/ai spawn random|movement
/ai train reinforcement|stop
/ai brain status|load|save|reset
/ai evaluate
/ai inspect info
```

`/bot` still opens the management UI for players. `/botenvironment` and `/botenv` remain supported as legacy environment roots.

## Spawning

### `/bot spawn single <name> [skin] [loc]`
Spawn one bot. `skin` defaults to a Mojang lookup of `<name>`. `loc` is either a player name or `x y z [world]`.

### `/bot spawn multiple <amount> <name> [skin] [loc]`
Spawn many bots at once.

### `/bot admin reset`
Remove every spawned bot. **Requires** `terminatorplus.admin`.

## Equipment

### `/bot equipment inventory <bot-name>` (legacy `/bot inventory`, alias `inv`)
Open a 54-slot chest GUI that edits one exact bot. Click **Save changes** to
apply; closing discards unsaved edits. Ambiguous duplicate names are rejected.
See [Inventory GUI](Inventory-GUI).

### `/bot equipment give <item> [bot-name] [slot]`
- One arg: sets the default item for every bot.
- Two args: drop the item into the first empty hotbar slot on the named bot.
- Three args: place into the specified inventory slot. See the [Inventory GUI slot map](Inventory-GUI#slot-map).

### `/bot settings placement-material <material>`
Set the global block used by bot building and clutch placement. The default is `COBBLESTONE`.

Example: `/bot settings placement-material DIAMOND_BLOCK`

### `/bot equipment armor <tier>`
Apply an armor tier to every bot. Tiers: `none`, `leather`, `chain`, `gold`, `iron`, `diamond`, `netherite`.

### `/bot equipment loadout <name> [bot-name]`
Apply a predefined combat loadout. If `bot-name` is omitted, applies to all bots. See [Loadouts](Loadouts).

### `/bot equipment mixed-loadout <mix> [bot-prefix]`
Apply rotating combat loadouts across bots. Each bot gets a different loadout from the mix.

| Mix | Loadouts |
| --- | --- |
| `alltypes` / `all` / `balanced` | All 14 loadout types distributed evenly |
| `core` | `sword`, `axe`, `smp`, `mace`, `trident`, `spear`, `pot` |
| `problem` / `combatdata` / `bugs` | `mace` (3x), `axe` (3x), `smp` (2x), `vanilla`, `hybrid` |

### `/bot inspect weapons [bot-name]`
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

### `/bot inspect info <bot-name>`
Print the bot's name, world, position, velocity.

### `/bot inspect list` (legacy `/bot count`, alias `list`)
Count bots by name.

## Settings

### `/bot settings combat-goal <goal>`
Change the global target-selection strategy. Goals: `PLAYER`, `NEAREST`, `NEAREST_PLAYER`, etc.

### `/bot settings target-mobs <true|false>`
Whether hostile mobs target spawned bots.

### `/bot settings show-in-player-list <true|false>`
Whether newly-spawned bots appear in the tab list (and are affected by `@a`/`@p` selectors).

### `/bot settings target-player <name>`
Set the player that bots focus on when goal is `PLAYER`.

### `/bot settings target-region <x1> <y1> <z1> <x2> <y2> <z2> [<wX> <wY> <wZ>|strict]`
Set region for bot prioritization.

### `/bot settings auto-respawn <true|false>`
Enable or disable automatic bot respawning. **Requires** `terminatorplus.admin`.

### `/bot settings movement-v2 <on|off|status>`
Enable, disable, or inspect Movement V2. **Requires** `terminatorplus.admin`.

## Utility

### `/bot move gather` (legacy `/bot gather`, alias `tpall`)
Teleport all bots to your location.

### `/bot move scatter [radius]`
Distribute living bots in a safe circular spread around your location.
The radius has no command-defined maximum; the world border still determines
whether destinations are valid.

### `/bot debug behavior <expression>`
Run a debugger behavior expression. **Requires** `terminatorplus.admin`.

### `/bot debug movement [bot-name]`
Show Movement V2 route and fallback status. **Requires** `terminatorplus.admin`.

### `/bot debug combat <name|all> <on|off>` (legacy `/bot combatdebug`, aliases `cdbg`, `comatdebug`)
Toggle combat trace logging for specific bots or all bots. Shows telemetry fields like `critPred`, `sweepPred`, `chargeAtVanillaAttack`, `targetHp`, and `targetHpDelta`. **Requires** `terminatorplus.admin`.

## AI Training (`/ai`)

### `/ai train reinforcement <population-size> <name> [skin] [mode-or-options] [round-minutes]`
Begin a training session. Must be run as a player.

- Empty mode defaults to **movement-controller** training.
- `mode` may be `movement`, `movement-controller`, `movement_controller`, or `legacy`.
- In **movement-controller** mode, the NN controls movement only and the CombatDirector handles combat.
- In **legacy** mode, the NN fully replaces both movement and combat (the original training pipeline).
- Movement mode automatically samples weighted training loadouts from `ai.training.loadout-mix`.
- With the default config, each generation round is capped at `1` minute. Pass `[round-minutes]` to override for one session, or set `ai.training.max-round-minutes: 0` for unlimited rounds. Optional arguments are positional, so use `movement` or an options string in the mode slot when passing a minute override.
- Mixed movement training ranks bots by the family they actually produced route samples for and autosaves eligible specialist brains, subject to `save-only-improved-brain`.
- Use options such as `family=mace:mix=mace_curriculum` or `movement:family=mace:mix=mace_curriculum` for curriculum runs.

### `/ai spawn random <amount> <name> [skin] [loc]`
Spawn bots with random neural networks.

### `/ai spawn movement <amount> <name> [skin] [loc]`
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

### `/ai inspect info <bot-name>`
Display neural network info about a specific bot.

### `/ai train stop`
End the current AI training session.

## Environment (`/bot environment`; legacy `/botenvironment`, alias `/botenv`)

Configure how bots understand blocks and mobs.

| Subcommand | Purpose |
| --- | --- |
| `inspect material <x> <y> <z>` | Print the block material at a location (player only) |
| `solid-block add <material>` | Add a material to the "solid" list |
| `solid-block remove <material>` | Remove a material from the solid list |
| `solid-block list` / `clear` | List or clear custom solid materials |
| `custom-mob add <entity>` | Mark a mob as target-eligible |
| `custom-mob remove <entity>` | Remove a custom mob |
| `custom-mob list` / `clear` | List or clear custom mobs |
| `mob-list-mode get` / `set <mode>` | Read or change custom mob list behavior |

## Plugin

### `/terminatorplus debuginfo` (alias `/tplus debuginfo`)
Upload debug info to mclo.gs. Share the link when reporting bugs.
