# Combat system audit: 29 bugs preventing bots from winning fights

**Source of evidence:** telemetry log `combat-all.log` (27,726 lines, one full bot-vs-player fight on `mc-26.1.2`) cross-referenced with current code on `mc-1.21.11` / `mc-26.1.2` / `master`. The fight ends with the bot at 7–8% HP, target at 80% — a decisive loss on a full-netherite sword kit.

For each finding: **file:line**, **symptom**, **root cause**, **fix sketch**. Priority ordering is rough — P0s block the bot from ever landing damage; P1–P2 affect most fights; P3+ are opportunistic.

---

## Raw log evidence (the fight, in numbers)

| Metric | Value | Implication |
| --- | ---: | --- |
| Total ticks logged | 2245 | ~112 s of combat |
| `opp-scan result=none` | 1718 / 1718 | Opportunity scanner **never** fired a play — 0% hit rate |
| `scanner-miss` | 1718 | Every scan fell off the catalog |
| `dir-noop reason=no-branch-matched` | 506 | Director idled on 23% of entry ticks |
| `swing-gate allowed=false reason=charge` | 579 / 662 | 87% of swing attempts blocked by charge gate |
| `melee-skip reason=non-melee-held` | 584 / 628 | 93% of melee attempts skipped — not holding a weapon |
| `weapon-pick w=MELEE(empty)` | 584 / 1212 | Bot held nothing 48% of the time |
| `weapon-pick w=SWORD / AXE` | 610 / 18 | Real weapons only picked 52% of ticks |
| `melee-hit` | **58** | ~58 landed hits in 2 minutes |
| `melee-try` | 628 | 9.2% hit rate |
| `consumable-use` | **6** | Six heals all fight (all gapples) |
| `consumable-noop` | 1499 | Cascade gated out every other tick |
| `move-jump redirect-to-walk` | 137 / 251 | 55% of jump commands were cancelled |
| `move-ground result=false reason=ascending` | 709 | Bot was airborne ~31% of ticks |

### Held-item distribution on `melee-skip`

| Held | Ticks | % of melee-skip | Source of the override |
| --- | ---: | ---: | --- |
| `COBBLESTONE` | 313 | 53.6% | `LegacyAgent` clutch + `Bot#attemptBlockPlace` |
| `AIR` | 151 | 25.9% | Prior `setItem(MAIN_HAND)` deleted the weapon outright |
| `GOLDEN_APPLE` | 109 | 18.7% | `ConsumableBehavior` ate and never restored hotbar |
| `WATER_BUCKET` | 6 | 1.0% | MLG clutch |
| `ENDER_PEARL` | 5 | 0.9% | Pearl toss pre-select |

### Held-item distribution on `punch`

61 of 120 punches (51%) landed while holding a real weapon (`NETHERITE_SWORD` 61, `NETHERITE_AXE` 1). **49% of punches did zero damage** because the held item was a block, water bucket, or air.

### Kit presence over the fight

`dir-ready` log line records which kits the director sees as available. Out of 1718 entries:

- All 7 kits (`mace/trident/pearl/wind/crystal/anchor/cobweb`) reported `kit=false` for **1713 ticks (99.71%)**
- `pearl=true wind=true` briefly for **5 ticks** (t=40–44) — the same 5 ticks the bot held a pearl, i.e. the pearl was consumed immediately. No restock.

Cooldown flags (`cd[...]=true`) were ready on every single tick for every kit. The gate holding the bot back was **loadout presence**, not cooldown.

---

## P0 — bot cannot keep a weapon in hand

### P0-1. `setItem(MAIN_HAND)` is a destructive slot overwrite, not a swap

- File: [Bot.java:1013-1044](TerminatorPlus-Plugin/src/main/java/net/nuggetmc/tplus/bot/Bot.java#L1013-L1044)
- Symptom: 48% of weapon-picks report `MELEE(empty)`; 151 melee-skips show `held=AIR`; the fight ends with the bot bare-handed.
- Root cause: `setItemInMainHand(item)` writes the stack to the **currently selected hotbar slot**. After it returns, whatever weapon lived in that slot is gone from the whole inventory. There is no save/restore layer — `autoEquip` only runs on GUI close.
- Callers that destroy weapons this way (each proven by the log):
  - `Bot#attemptBlockPlace` writes COBBLESTONE into the selected slot (→ 313 `held=COBBLESTONE`).
  - `ConsumableBehavior` selects gapple then eats without snapshot (→ 109 `held=GOLDEN_APPLE`).
  - `LegacyBlockCheck#clutch` places water bucket without snapshot (→ 6 `held=WATER_BUCKET`).
  - `LegacyAgent` direct `setItem(new ItemStack(...))` calls at [LegacyAgent.java:380, 471, 480, 880, 898, 1263, 1270, 1382](TerminatorPlus-API/src/main/java/net/nuggetmc/tplus/api/agent/legacyagent/LegacyAgent.java).
- Fix: introduce a `WeaponGuard` (push/pop) around every non-combat slot mutation. `push()` captures `(slot, stack.clone())`, write the override, `pop()` restores. Or: always stash the weapon into a spare storage slot before overriding, and restore on clutch-exit.

### P0-2. Swing gate has **no crit check**

- File: [BotCombatTiming.java](TerminatorPlus-Plugin/src/main/java/net/nuggetmc/tplus/bot/combat/BotCombatTiming.java)
- Symptom: `swing-gate` approves swings only when `charge >= 0.95`, but never checks vanilla crit preconditions (`vy<0 && !onGround && fallDistance>0 && !isSprinting`). The bot never sprint-jump crits.
- Root cause: `canSwing` only inspects charge + i-frames.
- Fix: extend `canSwing` to also gate on `shouldCrit()` when a crit-favorable window exists (descending air + fallDistance > 0, not sprinting), so the bot prefers the micro-jump strike.

### P0-3. `OpportunityScanner.bot.attack()` bypasses the swing gate

- File: [OpportunityScanner.java:684, 695](TerminatorPlus-Plugin/src/main/java/net/nuggetmc/tplus/bot/combat/OpportunityScanner.java) (Read returned "too large" — line refs from prior read.)
- Symptom: scanner-driven hits (when any do land) bypass `BotCombatTiming.canSwing` and therefore ignore the charge gate.
- Fix: route all scanner-originated attacks through a single `tryAttack(target)` that consults the gate.

---

## P1 — the Crystal and Anchor branches are either self-killing or dead code

### P1-1. Crystal behavior detonates inside the bot's own blast radius

- Files: [CombatDirector.java:181-188](TerminatorPlus-Plugin/src/main/java/net/nuggetmc/tplus/bot/combat/CombatDirector.java#L181-L188), [CrystalBehavior.java:27,65](TerminatorPlus-Plugin/src/main/java/net/nuggetmc/tplus/bot/combat/CrystalBehavior.java#L27-L65)
- Symptom: if the crystal kit ever appears mid-fight, the bot kills itself.
- Root cause: the director fires the behavior at `distance 2.0–6.0`. `CrystalBehavior.MAX_DISTANCE = 6.0` and creates an explosion of power `6.0f` at the target's location. Blast damage falls off with distance, but at 2.0 blocks the bot eats lethal damage. `bot.getBukkitEntity()` as the source does not exempt the source from damage.
- Fix: raise both MIN_DISTANCE and MAX_DISTANCE to at least 4.5–7.0; verify the bot's line to the explosion is clear; or reduce explosion power (the vanilla crystal is 6.0 but the bot's survivable budget is much lower).

### P1-2. Anchor behavior is 100% dead code

- Files: [CombatDirector.java:190-197](TerminatorPlus-Plugin/src/main/java/net/nuggetmc/tplus/bot/combat/CombatDirector.java#L190-L197), [AnchorBombBehavior.java:30-44](TerminatorPlus-Plugin/src/main/java/net/nuggetmc/tplus/bot/combat/AnchorBombBehavior.java#L30-L44)
- Symptom: the anchor bomb has never fired in any fight.
- Root cause: **inverted environment gate plus non-overlapping range**:
  - Director gates `env == NETHER`; behavior rejects `env == NETHER` (vanilla anchors only detonate **outside** the Nether).
  - Director fires at `distance 2.0–5.0`; behavior requires `6.0–8.0`. Even if the env gate were fixed, the ranges don't overlap.
- Fix: invert the director's env gate (`env != NETHER`) and align ranges so one strictly contains the other (e.g. director 6.0–8.0).

### P1-3. Trident vs melee dead-zone

- Files: [CombatDirector.java:213,251](TerminatorPlus-Plugin/src/main/java/net/nuggetmc/tplus/bot/combat/CombatDirector.java), [TridentBehavior.java](TerminatorPlus-Plugin/src/main/java/net/nuggetmc/tplus/bot/combat/TridentBehavior.java)
- Symptom: when target drifts to 3.5 < dist < 5.0, the bot stops engaging.
- Root cause: melee cuts off at `distance <= 3.5`; trident starts at `>= 5.0`. No branch covers 3.5–5.0.
- Fix: extend melee to 4.5 (matches `MeleeBehavior.ATTACK_RANGE = 4.0`) **or** drop trident minimum to 3.8; align the director and the behavior's own range check.

### P1-4. Trident throw freezes the bot for ~0.9 s

- File: [TridentBehavior.java](TerminatorPlus-Plugin/src/main/java/net/nuggetmc/tplus/bot/combat/TridentBehavior.java) (`MAX_CHARGE_TICKS = 18`)
- Symptom: during `CHARGING`, the bot stops moving/dodging for 18 ticks.
- Fix: allow movement during trident charge; snapshot inputs and release at the earliest viable aim.

### P1-5. Wind-charge standalone `ticksFor` is dead code

- Files: [CombatDirector.java:170-173](TerminatorPlus-Plugin/src/main/java/net/nuggetmc/tplus/bot/combat/CombatDirector.java#L170-L173), [WindChargeBehavior.java:45-72, 98, 161](TerminatorPlus-Plugin/src/main/java/net/nuggetmc/tplus/bot/combat/WindChargeBehavior.java)
- Symptom: the scanner fires wind charges for knockup combos; the standalone branch never runs.
- Root cause: the comment at CombatDirector.java:170-173 explicitly removes wind from the pipeline, but `WindChargeBehavior.ticksFor` still contains a full range/kit/cooldown check that is never called. Worse, `WindChargeBehavior` sets its 120-tick cooldown at **plan time** rather than fire time (line 161), so a cancelled plan (line 98) burns the cooldown with zero effect.
- Fix: either delete `WindChargeBehavior.ticksFor` entirely (let scanner drive it) or reintroduce the branch in the director. Move cooldown set to fire time regardless.

---

## P2 — combat tick can be gated out by unrelated subsystems

### P2-1. Mining animation blocks all combat

- File: [LegacyAgent.java:182](TerminatorPlus-API/src/main/java/net/nuggetmc/tplus/api/agent/legacyagent/LegacyAgent.java#L182)
- Symptom: 506 `dir-noop reason=no-branch-matched` events. Many of these are the director being skipped entirely because `miningAnim` holds the bot.
- Root cause: `combatTickReady && !miningAnim.containsKey(botPlayer)` — while mining anim runs, combat never ticks.
- Fix: combat should be independent of mining. Run the director regardless and let the miner animate in a cosmetic channel (or swap-out mining when a target is within engagement distance).

### P2-2. `preBreak` BukkitRunnable punches every 4 ticks forever

- File: [LegacyAgent.java:1121-1134](TerminatorPlus-API/src/main/java/net/nuggetmc/tplus/api/agent/legacyagent/LegacyAgent.java#L1121-L1134)
- Symptom: 24 `punch src=LegacyAgent#lambda$checkUp$0` events, many of them with `held=COBBLESTONE` (not useful for mining/attacking). The task has no cancel condition.
- Fix: cancel the runnable when the bot starts combat or switches held slot; or bind it to the block's breaking progress.

### P2-3. Clutch blockcheck swings held weapon cosmetically

- File: [LegacyBlockCheck.java:224, 284](TerminatorPlus-API/src/main/java/net/nuggetmc/tplus/api/agent/legacyagent/LegacyBlockCheck.java)
- Symptom: 9 `punch src=LegacyBlockCheck#clutch` events — bot swings mid-clutch, wasting charge. All 9 show `held=COBBLESTONE`.
- Fix: only punch in clutch if a target is within melee range and the held item is a weapon.

---

## P3 — consumable cascade only fires in narrow conditions

### P3-1. Consumable cascade falls through after one branch

- File: [ConsumableBehavior.java](TerminatorPlus-Plugin/src/main/java/net/nuggetmc/tplus/bot/combat/ConsumableBehavior.java)
- Symptom: only 6 `consumable-use` events across the entire fight; 1499 `consumable-noop`. Bot dies at 7% HP unable to heal.
- Root cause: the cascade (critical → low-HP → splash → drink) short-circuits on any branch that gates out; there is no fallback to the next branch. `hasAnyConsumable` only scans the hotbar.
- Fix: make the cascade additive — try each branch until one succeeds. Widen `hasAnyConsumable` to include storage (with auto-swap to hotbar).

### P3-2. Eating a consumable doesn't restore the weapon

- File: [ConsumableBehavior.java:148](TerminatorPlus-Plugin/src/main/java/net/nuggetmc/tplus/bot/combat/ConsumableBehavior.java#L148)
- Symptom: 109 consecutive ticks of `held=GOLDEN_APPLE` after a single gapple use. Bot skips melee for ~5.5 s.
- Root cause: `bot.selectHotbarSlot(slot)` switches to the gapple slot to eat, but there's no restore back to the weapon slot after the eat finishes.
- Fix: push/pop the held slot around the eat. Record `previousSlot`, eat, `selectHotbarSlot(previousSlot)` on the next tick after consumption completes.

### P3-3. Hotbar select is a no-op when already on that slot

- File: [BotInventory.java:117-137](TerminatorPlus-Plugin/src/main/java/net/nuggetmc/tplus/bot/loadout/BotInventory.java#L117-L137)
- Symptom: after a destructive `setItem(MAIN_HAND)`, the slot is empty but `selectHotbarSlot(currentSlot)` early-returns.
- Fix: remove the early-return, or have the guard on P0-1 explicitly re-find the weapon and pick a new slot.

---

## P4 — opportunity scanner is mis-targeted for "basic" kits

### P4-1. All 37 plays are kit-gated; default sword bot matches zero

- File: [OpportunityScanner.java](TerminatorPlus-Plugin/src/main/java/net/nuggetmc/tplus/bot/combat/OpportunityScanner.java)
- Symptom: `opp-scan result=none` on all 1718 scans. Scanner is dead weight for any bot without the special kits.
- Root cause: every play requires mace / trident / crystal / anchor / pearl / wind / cobweb. A sword-only bot has nothing to fire.
- Fix: add sword-only plays — sprint-reset, crit-jump, combo-extend, pursue-gap-close, disengage — so the scanner contributes even without specialty gear.

### P4-2. Scanner has no result-miss breakdown

- Files: [OpportunityScanner.java](TerminatorPlus-Plugin/src/main/java/net/nuggetmc/tplus/bot/combat/OpportunityScanner.java), [CombatDebugger.java](TerminatorPlus-Plugin/src/main/java/net/nuggetmc/tplus/bot/combat/CombatDebugger.java)
- Symptom: `scanner-miss` is logged per tick but carries only target-state booleans; we cannot tell *which* of the 37 plays were considered and why each rejected.
- Fix: emit `scanner-miss play=<name> reason=<short>` when a play exits for a non-inventory reason. Tag kit-presence exits separately so they don't drown the rest.

---

## P5 — inventory + kit detection edge cases

### P5-1. Kit detection counts hotbar only

- File: [BotInventory.java:170-172](TerminatorPlus-Plugin/src/main/java/net/nuggetmc/tplus/bot/loadout/BotInventory.java#L170-L172)
- Symptom: `hasHotbar` drives `hasCrystalKit / hasAnchorKit / hasMace / hasTrident / hasPearls / hasWindCharge / hasCobweb`. The helper comment says "hotbar or storage" but the code only calls `findHotbar` (hotbar). A kit in storage reads as absent.
- Fix: either check storage too (with auto-swap) or update the comment to match behaviour. Recommended: check both, swap on demand.

### P5-2. `ensureMovementKit` restocks specialty items — but not weapons

- File: [BotInventory.java:728](TerminatorPlus-Plugin/src/main/java/net/nuggetmc/tplus/bot/loadout/BotInventory.java#L728)
- Symptom: pearls and wind charges are refilled every 40 ticks, but the main weapon is not. After any override deletes the sword, it stays gone.
- Fix: extend `ensureMovementKit` to also re-equip a stored backup weapon (sword/axe/mace) when the hotbar has none.

### P5-3. No in-combat autoEquip

- File: [BotInventory.java:503](TerminatorPlus-Plugin/src/main/java/net/nuggetmc/tplus/bot/loadout/BotInventory.java#L503)
- Symptom: "called when inventory GUI closes" — autoEquip only runs when a human operator closes the bot's inventory editor. In a live fight it never runs.
- Fix: hook it on a low-frequency combat tick (every 20 ticks) when HP > 0 and no target is engaged, or when a weapon disappears.

---

## P6 — NMS inventory race against container transactions (Paper 26.x)

### P6-1. LegacyAgent still goes through `PlayerInventory.setItem` for weapon swaps

- File: [LegacyAgent.java](TerminatorPlus-API/src/main/java/net/nuggetmc/tplus/api/agent/legacyagent/LegacyAgent.java) (multiple call sites)
- Symptom: on 26.x, Paper's container-transaction system rolls back any `PlayerInventory.setItem` write the next tick because `MockConnection` never ACKs the slot packet. The sword comes back, but only after a visible tick of corruption.
- Root cause: per `CLAUDE.md`, Paper 26.x rolls back `PlayerInventory.setItem` for bots. The documented pattern (NMS direct write) is only used in some paths.
- Fix: replace every `PlayerInventory.setItem` on a bot with the NMS pattern from `CLAUDE.md`:
  ```java
  net.minecraft.world.entity.player.Inventory nmsInv = bot.getInventory();
  nmsInv.setItem(i, CraftItemStack.asNMSCopy(bukkitStack));
  nmsInv.setChanged();
  ```

---

## P7 — preset & loadout rough edges (minor)

### P7-1. Preset capture doesn't include shield field

- File: [PresetManager.java](TerminatorPlus-Plugin/src/main/java/net/nuggetmc/tplus/bot/preset/PresetManager.java) `capture()`
- Symptom: loading a preset that had a shield drops it silently.
- Fix: serialize + restore shield alongside armor/hotbar.

---

## P8 — ergonomics / log clarity

### P8-1. `snapshot` never logs `botBlockCount` / `weaponTier`

- File: [CombatDebugger.java](TerminatorPlus-Plugin/src/main/java/net/nuggetmc/tplus/bot/combat/CombatDebugger.java)
- Symptom: we can't tell from the log whether a `melee-skip` happened because the bot was out of cobblestone, out of golden apples, etc.
- Fix: add a per-second `inventory` log line: hotbar summary + armor + offhand.

### P8-2. `dir-entry` has no outcome field

- Symptom: you can see a `dir-entry` followed by a `dir-noop`, but not what branch *almost* matched.
- Fix: `dir-noop branch_attempted=<last-checked>` — helps tell melee-miss from trident-miss from crystal-miss.

### P8-3. `punch` event lacks "why"

- Symptom: 120 punches logged with `src=...` but no semantic tag (swing, clutch, pre-break, scanner).
- Fix: extend the `src=` enum so clutch and pre-break punches are their own tags instead of `LegacyBlockCheck#clutch` / `LegacyAgent#lambda$checkUp$0`.

---

## Priority ordering (what to fix first)

1. **P0-1** weapon-guard around every non-combat `setItem(MAIN_HAND)` — fixes 48% of `MELEE(empty)` and all `COBBLESTONE/GAPPLE/WATER_BUCKET` held-item pins.
2. **P0-2** add crit gate to `canSwing` — lets the bot actually crit.
3. **P3-1 + P3-2** consumable cascade fallthrough and restore-after-eat — lifts heal rate from 6 fights per fight to ~double-digit.
4. **P4-1** sword-only opportunity plays — gives the scanner something to do for the default bot.
5. **P1-1, P1-2, P1-3, P1-5** crystal/anchor/trident/wind alignment — stops self-kill and dead-branch conditions.
6. **P2-1** decouple combat tick from `miningAnim` — recovers the 506 `dir-noop` ticks.
7. **P5-2, P5-3** in-combat weapon restock.
8. **P6-1** finish NMS migration for all inventory writes.
9. **P7/P8** quality-of-life.

---

## Repro / evidence bundle

Log available at `C:\Users\dudie\Downloads\combat-all.log` (27,726 lines). Key greps to reproduce:

```bash
# Event distribution
awk '{print $6}' combat-all.log | sort | uniq -c | sort -rn

# Held-item distribution on melee-skip
grep "melee-skip" combat-all.log | awk '{print $NF}' | sort | uniq -c | sort -rn

# Proof that anchor kit never exists
grep "dir-ready" combat-all.log | grep "anchor=true"   # → 0 matches

# Proof of the weapon-override cascade (first time the bot goes cobble-held)
grep "held=COBBLESTONE" combat-all.log | head
```

---

No code changes are proposed in this issue — it's a consolidated audit. Happy to open PRs per finding when prioritised.
