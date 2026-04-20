# Combat Behaviors

Every tick the [CombatDirector](../TerminatorPlus-Plugin/src/main/java/net/nuggetmc/tplus/bot/combat/CombatDirector.java) picks one weapon behavior to run for each bot, based on the bot's inventory, the distance to its target, and per-behavior cooldowns. Passive behaviors (elytra glide, totem swap) run every tick in addition.

Bots with a neural network (AI training mode) bypass the director entirely — see [Neural Network Mode](Neural-Network-Mode).

## Priority pipeline

Highest first. The director commits to the first behavior that matches and returns.

| # | Behavior | Condition | Cooldown |
| --: | --- | --- | --- |
| 0 | Mid-attack commit | Bot is mid-mace-fall or mid-trident-charge | — |
| 1 | Crystal PvP | Not Nether, has kit, distance ≤ 6 | 30t |
| 2 | Anchor Bomb | Nether, has kit, distance ≤ 5 | 50t |
| 3 | Mace Smash | Grounded, has mace, distance ≤ 3.5 | 80t |
| 4 | Sword Melee | Distance ≤ 3.5 | — |
| 5 | Trident Throw | Has trident, 5 ≤ distance ≤ 28 | 60t |
| 6 | Ender Pearl | Has pearl, 14 ≤ distance ≤ 35 | 80t |
| 7 | Wind Charge | Has wind charge, distance ≥ 4 | 40t |
| 8 | Cobweb | Has cobweb, target fleeing, distance ≤ 4.5 | 30t |
| 9 | Heal | Health < 40% | 100t |

Passive (not in priority — always run): **Elytra Glide**, **Totem Swap**.

Cooldowns are measured in server ticks (1t = 50ms).

## Behaviors

### Sword / Axe Melee
File: `MeleeBehavior.java`

- Trigger: target within 3.5 blocks.
- Uses vanilla `Player.attack()` for real damage (crits, shield breaks, knockback enchants).
- Picks a sword over an axe; axe over fist.

### Mace Smash
File: `MaceBehavior.java`

- Trigger: grounded + mace in hotbar + target ≤ 3.5 blocks + cooldown ready.
- Jumps, waits to reach peak, dives onto target. Fall-damage stacking applies via vanilla.
- While airborne the director commits to mace regardless of other weapons.

### Trident Throw
File: `TridentBehavior.java`

- Trigger: trident in hotbar + 5 ≤ distance ≤ 28 + cooldown ready.
- **Momentum build-up**: sprints toward target for a few ticks (`CombatState.Phase.CHARGING`) then releases the trident. Run-up velocity stacks with the projectile for heavy impact.
- While charging, director commits to trident.

### Wind Charge
File: `WindChargeBehavior.java`

- Trigger: wind charge in hotbar + distance ≥ 4 + cooldown ready.
- Lobs a wind charge aimed at the target. Vanilla wind knockback does the rest.

### Ender Pearl
File: `EnderPearlBehavior.java`

- Trigger: pearl in hotbar + 14 ≤ distance ≤ 35 + cooldown ready.
- Leads target velocity (`target + targetVel * distance/12`) with a small upward arc.
- Vanilla handles teleport on projectile hit.
- Consumes one pearl from the stack.

### Crystal PvP
File: `CrystalBehavior.java`

- Trigger: overworld or end + kit has end crystal + obsidian + distance ≤ 6 + cooldown ready.
- Finds a valid host block next to the target (existing obsidian / bedrock / glowstone) or places one.
- Spawns an `EnderCrystal`, removes it immediately, and calls `world.createExplosion(..., 6.0f, false, true, bot)` so vanilla damage applies.

### Anchor Bomb
File: `AnchorBombBehavior.java`

- Trigger: `World.Environment.NETHER` + kit has respawn anchor + glowstone + distance ≤ 5 + cooldown ready.
- Places an anchor, charges it with glowstone, then detonates via `world.createExplosion(..., 5.0f, false, true, bot)` and clears the block.

### Elytra Glide (passive)
File: `ElytraBehavior.java`

- If the bot has an elytra **and** is falling more than a few blocks, starts gliding.
- If a firework rocket is in the hotbar, uses it to boost forward — trident throws in this phase are devastating because of stacked momentum.
- When the bot lands, auto-swaps elytra ↔ chestplate if both are present (mainline chestplate slot is item 38, backup lives in storage).

### Totem of Undying (passive)
File: `TotemBehavior.java`

- If HP < 6 and a totem exists anywhere in the inventory, `equipOffhand(Material.TOTEM_OF_UNDYING)` swaps it into the offhand.
- Vanilla's "pop totem on fatal damage" fires automatically.
- Remembers the previous offhand so melee / elytra can restore the shield after the pop.

### Cobweb Utility
File: `UtilityBehavior.java`

- Trigger: target sprinting away (velocity dotted with bot→target direction > 0.25) + cobweb in hotbar + distance ≤ 4.5.
- Places a cobweb at the target's feet to slow the escape.

### Heal
Handled inline by the director (`tryHeal`). Eats a golden apple (restores 8 HP) when health drops below 40%.

## Fine-tuning

Ranges and cooldowns are constants in each behavior file. If you fork the plugin and want a slower Ender Pearl, bump the `COOLDOWN_TICKS` field. Nothing is config-file-driven today.
