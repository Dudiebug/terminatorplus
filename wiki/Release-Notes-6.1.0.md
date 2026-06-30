# TerminatorPlus 6.1.0 - Legal Actions, Live Metrics, Less Fake PvP

TerminatorPlus 6.1.0 is a focused follow-up to the 6.0.0 architecture release.
Version 6.0.0 built the foundation: richer combat intent, MovementInput v3,
baseline movement, NN residual mixing, timed core consumables, and the
`PlayerLikeActionController`.

Version 6.1.0 starts making that foundation prove itself in real fights.

The headline is simple: the bot should still be strong, fast, and annoying to
fight, but it should stop looking like it is winning through impossible
shortcuts. This release moves more behavior through action states, records the
remaining shortcuts honestly, and adds live duel metrics so future tuning can be
based on what actually happened in combat.

## Highlights

- Splash self-heal no longer double-dips by throwing a potion and applying an
  immediate extra heal.
- Splash throws, cobweb placement, ender pearls, and trident releases now reserve
  short player-like action windows before their result happens.
- Movement slows or holds during active use, drink, projectile, pearl, placement,
  and mining actions without changing MovementInput v3.
- Live duel metrics now track damage, spacing, retreat behavior, movement
  fallback, route thrash, heal completion/cancel counts, and action-legality
  counters.
- Legacy obstacle mining now has a lightweight block/tool speed model instead of
  a fixed crack timer for everything.
- The build version is now `6.1.0-BETA-mc26.1.2`.

## Why This Release Matters

Before this release, several advanced actions were measured as shortcuts but
still happened too abruptly. That made the bot hard to evaluate: did it win
because it had better timing, spacing, and decision-making, or because it
snapped a result into the world faster than a player could?

6.1.0 does not pretend every advanced action is fully vanilla-perfect yet. It
does something more useful for this stage: it adds action phases, prevents the
worst impossible stacking, and records the parts that are still direct. That
means future balancing can punish fake behavior instead of accidentally training
around it.

## Player-Like Action Improvements

### Splash Potions

Splash self-heal was the biggest practical fix in this release.

Previously, the bot could throw a splash healing potion and also apply healing
immediately as a reliability shortcut. That created fake-looking behavior and
could effectively reward the bot twice for one action.

Now:

- splash self-heal reserves a short throw action;
- the potion is released from a completion callback;
- the duplicate immediate heal is gone;
- the item is decremented once;
- the previous weapon is restored after release;
- remaining direct projectile release is counted as shortcut telemetry.

Splash offense follows the same short action-release pattern.

### Cobweb Placement

Utility cobweb placement and scanner-driven cobweb plays now reserve a placement
action before the block is set.

The placement still uses the existing direct `setType` release internally, but it
is no longer an invisible same-frame result. The bot now has to claim a
`PLACING_BLOCK` action window, and the release is recorded.

### Ender Pearls

Pearls now behave more like an actual committed action:

- select pearl;
- reserve `USING_PEARL`;
- face the target;
- release after a short delay;
- decrement once;
- restore the previous weapon.

The projectile spawn is still direct at release time, and it is reported that
way. This is a measured migration step, not a full packet-perfect pearl rewrite.

### Tridents

Trident charge now starts an action-controller state, and release is tied to that
state instead of being only a direct projectile spawn.

The existing momentum throw behavior remains intact, but release is now recorded
as an action-state-bound projectile shortcut. This should make same-tick melee
stacking and trident release timing easier to inspect in combat logs.

## Movement During Actions

MovementInput stays at schema version 3. No brain reset, no schema bump, no v4
surprise.

Instead, 6.1.0 constrains movement output after the existing mixer:

- eating and drinking sharply reduce movement and sprinting;
- projectile, pearl, placement, and mining actions damp movement and suppress
  jumps;
- debug logs record before/after movement output when an action constraint is
  applied.

This keeps the movement system combat-informed but not combat-authoritative.
Movement still does not select items, place blocks, attack, throw projectiles, or
call combat behavior internals.

## Live Duel Metrics

6.1.0 adds `LiveDuelMetricsRecorder` and `LiveDuelMetricsSnapshot`.

Runtime fights can now accumulate:

- damage dealt;
- damage taken;
- damage delta and ratio;
- ticks in desired range;
- too-close and too-far ticks;
- movement fallback ticks;
- movement held ticks;
- route thrash count;
- retreat success/failure ticks;
- fake/direct shortcut count;
- instant consume shortcut count;
- illegal same-tick action count;
- action interruption count;
- heal completion count;
- heal cancel count.

Report-only `/ai evaluate` exports are still honest: live metrics remain `null`
when no live duel ran. The evaluation report schema is now version 3 so tooling
can distinguish report-only route probes from live runtime metrics.

## Reward and Training Signals

Movement reward scoring now includes action-legality pressure:

- fake/direct shortcuts are penalized;
- instant consumes are penalized more heavily;
- illegal same-tick primary actions are penalized;
- interruptions are tracked as a smaller penalty;
- completed legal heals get a small positive signal;
- canceled heals get a small negative signal.

The goal is not to make the bot passive. The goal is to reward pressure that
comes from real spacing, timing, damage, and recovery instead of fake action
stacking.

## Mining and Fall-Clutch Cleanup

Legacy mining remains protected, but the old fixed crack loop has been replaced
with a small tool-aware progress model for common obstacle blocks.

This means obvious cases like pickaxe-on-stone, shovel-on-dirt, axe-on-wood, and
slow hand mining now differ instead of every block progressing through the same
fixed timer.

Fall/clutch behavior now records environment fall blocking as `FALL_CLUTCH`
telemetry. The release does not add broad mace-in-hotbar fall immunity, and the
release notes intentionally do not claim fall behavior is fully solved without
runtime proof.

## For Server Owners

Use this release if you want the 6.0.0 architecture with better action timing,
better measurement, and fewer obviously fake advanced-tool shortcuts.

Suggested smoke test:

```text
/bot create DuelBot
/bot loadout sword DuelBot
/bot combatdebug DuelBot on
/ai movement 1 DuelBot
```

Then test targeted loadouts:

```text
/bot loadout pot DuelBot
/bot loadout trident DuelBot
/bot loadout vanilla DuelBot
/bot loadout hybrid DuelBot
```

Watch for:

- splash self-heal consumes once and does not double-heal;
- pearl restores the previous weapon after release;
- trident charge/release does not allow free same-tick melee;
- cobweb placement is delayed/action-bound;
- live debug output includes action and metric signals.

## For Developers and Trainers

Important files added or changed:

- `PlayerLikeActionController` now tracks heal completions and cancels.
- `LiveDuelMetricsRecorder` accumulates runtime fight metrics.
- `LiveDuelMetricsSnapshot` exposes the live payload to training/report code.
- `MovementRewardProfile` includes action-legality and heal completion signals.
- `MovementEvaluationHarness` uses report schema version 3.
- `MovementOutputApplier` constrains locomotion during active actions.

The important architecture boundary is preserved: movement may consume combat
intent and report physical state, but combat-owned code still owns item use,
projectiles, placement, attacks, and action timing.

## Known Limitations

These are intentionally not hidden:

- Runtime duel validation is still required before claiming gameplay improvement.
- Wind charge, crystal, and anchor action sequences still use direct shortcut
  telemetry.
- Bow, crossbow, firework, and full elytra legality rewrites are not included.
- Full Duel Core V2 runtime replacement is not included.
- Cobweb, pearl, trident, and splash still use direct Bukkit/Paper release
  primitives after their action phase.
- The release improves measurement and legality scaffolding; it does not make
  every advanced action packet-perfect.

## Validation

- Build command: `./gradlew build -q`
- Build result: passed on June 30, 2026
- Java: bundled Java 25 workspace JDK
- Artifact: `TerminatorPlus-6.1.0-BETA-mc26.1.2.jar`
- Runtime duel validation: needs runtime test

## Rollback Notes

The 6.1.0 changes are concentrated in:

- action behavior files;
- movement output application;
- live metric snapshot/recorder classes;
- reward scoring;
- the legacy mining crack loop;
- release documentation.

If runtime testing finds action timing regressions, revert the 6.1.0 commit as a
single unit rather than partially removing the metric or action-controller
pieces. The code is intentionally grouped so rollback is straightforward.
