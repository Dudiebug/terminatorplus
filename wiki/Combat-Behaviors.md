# Combat Behaviors

The **CombatDirector** owns all combat decisions. Every tick it evaluates a priority pipeline and commits to the first matching weapon behavior. The movement neural network (if active) handles footwork only — it never selects weapons or triggers attacks.

## Priority pipeline

Highest first. The director commits to the first behavior that matches and returns.

| # | Behavior | Condition | Cooldown |
| --: | --- | --- | --- |
| 0 | Mid-attack commit | Bot is mid-mace-fall or mid-trident-charge | — |
| 1 | Crystal PvP | Not Nether, has kit, distance <= 6 | 30t |
| 2 | Anchor Bomb | Nether, has kit, distance <= 5 | 50t |
| 3 | Mace Smash | Grounded, has mace, distance <= 3.5 | 80t |
| 4 | Sword Melee | Distance <= 3.5 | — |
| 5 | Trident Throw | Has trident, 5 <= distance <= 28 | 60t |
| 6 | Ender Pearl | Has pearl, 14 <= distance <= 35 | 80t |
| 7 | Wind Charge | Has wind charge, distance >= 4 | 40t |
| 8 | Cobweb | Has cobweb, target fleeing, distance <= 4.5 | 30t |
| 9 | Heal | Health < 40% | 100t |

Passive (not in priority — always run): **Elytra Glide**, **Totem Swap**.

Cooldowns are measured in server ticks (1t = 50ms).

## Combat Reliability (5.1.1)

### Charge-aware attack planning

The CombatDirector checks `BotCombatTiming.chargeReady()` before committing to attack branches. A bot won't swing until its attack strength reaches 0.95 (full charge), preventing low-damage spam. This also prevents swing-block cycling where bots repeatedly swing and raise shields without dealing meaningful damage.

### Vanilla attack ordering

`Bot.attack()` now calls `getBukkitEntity().attack(entity)` **before** punch/swing animations. Previously, swing/punch could reset the attack charge before the damage calculation ran, causing ghost hits.

### Mace recharge planning

The mace smash branch accounts for custom gravity and the required airtime to reach smash-ready charge (0.848). The director won't commit to a mace smash unless the bot can realistically complete the jump-fall cycle.

### Mace airborne tracking

When a bot is mid-mace-fall, the tracking system uses:
- Ground clip tolerance to avoid premature landing detection.
- Velocity-aware horizontal damping to keep the bot centered on a moving target without overshooting.

### Telemetry fields

Enable combat trace logging with `/bot combatdebug <name|all> on`. Available fields:

| Field | Meaning |
| --- | --- |
| `critPred` | Whether a crit was predicted for this swing |
| `sweepPred` | Whether a sweep was predicted |
| `chargeAtVanillaAttack` | Attack charge at the moment vanilla damage runs |
| `chargeAfterVanillaAttack` | Attack charge after the vanilla call resets it |
| `targetHp` | Target HP before the attack |
| `targetHpDelta` | HP change after the attack |

## Behaviors

### Sword / Axe Melee

- Trigger: target within 3.5 blocks.
- Uses vanilla `Player.attack()` for real damage (crits, shields, enchants).
- Picks a sword over an axe; axe over fist.
- Waits for full charge before swinging.

### Mace Smash

- Trigger: grounded + mace in hotbar + target <= 3.5 blocks + cooldown ready.
- Jumps, waits to reach peak, dives onto target. Fall-damage stacking applies via vanilla.
- While airborne the director commits to mace regardless of other weapons.
- Requires smash-ready charge (0.848) — lower threshold than melee because the fall provides extra damage scaling.

### Trident Throw

- Trigger: trident in hotbar + 5 <= distance <= 28 + cooldown ready.
- **Momentum build-up**: sprints toward target for a few ticks (charging phase) then releases. Run-up velocity stacks with the projectile.
- While charging, director commits to trident.

### Wind Charge

- Trigger: wind charge in hotbar + distance >= 4 + cooldown ready.
- Lobs a wind charge aimed at the target. Vanilla wind knockback applies.

### Ender Pearl

- Trigger: pearl in hotbar + 14 <= distance <= 35 + cooldown ready.
- Leads target velocity with a small upward arc.
- Vanilla handles teleport on hit. Consumes one pearl.

### Crystal PvP

- Trigger: Overworld or End + kit has end crystal + obsidian + distance <= 6 + cooldown ready.
- Places obsidian near target, spawns crystal, detonates for 6.0f explosion.

### Anchor Bomb

- Trigger: Nether + kit has respawn anchor + glowstone + distance <= 5 + cooldown ready.
- Places anchor, charges with glowstone, detonates for 5.0f explosion.

### Elytra Glide (passive)

- If the bot has an elytra and is falling, starts gliding.
- Uses firework rockets from hotbar for forward boost.
- Auto-swaps elytra and chestplate when state changes.

### Totem of Undying (passive)

- If HP < 6 and a totem exists in the inventory, swaps it into the offhand.
- Vanilla pop triggers on fatal damage.
- Remembers the previous offhand item for restoration.

### Cobweb Utility

- Trigger: target fleeing + cobweb in hotbar + distance <= 4.5.
- Places a cobweb at the target's feet.

### Heal

- Trigger: health < 40%.
- Eats a golden apple (restores 8 HP).

## CombatDirector and Movement NN

With the movement-controller neural network active, the flow is:

1. **CombatDirector.plan()** — evaluates the combat situation and produces a **CombatIntent** (desired range, urgency, crit/sprint/hold hints). Does not execute attacks.
2. **MovementBrainRouter / MovementNetwork** receives the CombatIntent as part of its 37-value input with one-hot branch-family fields, then produces movement outputs only.
3. **MovementOutputApplier** — applies movement to the bot, updates **MovementState**.
4. **CombatDirector.execute()** — reads MovementState (is the bot sprinting? falling? retreating?) and decides whether to commit to an attack, throw, or wait.

The NN cooperates with the Director's requests but makes its own movement decisions. The Director validates timing windows using the NN's reported state before executing combat.

## Fine-tuning

Ranges and cooldowns are constants in each behavior's source file. The combat timing thresholds are in `BotCombatTiming.java`. These are not config-file-driven.
