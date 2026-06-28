# TerminatorPlus Vision

## Mission

TerminatorPlus is being redirected from a general server-side bot plugin into a focused 1v1 PvP bot project.

The goal is not to spawn many weak bots. The goal is to make one bot that can pressure, survive, punish, and adapt against a skilled human PvPer.

Primary target branch: `mc-26.1.2`.

## Old model vs new model

### Old model

- Many bots against one player.
- Broad feature coverage.
- Loadout variety as a selling point.
- Impressive-looking mechanics.
- General-purpose bot behavior.
- Wiki pages acting as the main strategy.
- Advanced tools sometimes hiding weak fundamentals.

### New model

- One bot versus one skilled human.
- Duel quality over feature quantity.
- Movement, spacing, timing, and survival first.
- Advanced tools only after fundamentals work.
- Combat-aware movement with strict authority boundaries.
- Small reviewable changes.
- Runtime-tested behavior.

## Primary gameplay target

```text
one TerminatorPlus bot
versus
one skilled human PvPer
in a controlled duel arena
```

A change is valuable only if it improves actual duel behavior or protects the repo from regressions.

## Non-goals

- Do not optimize for large bot swarms first.
- Do not add flashy mechanics before fundamentals are reliable.
- Do not rewrite NMS/Paper internals unless there is a version-specific runtime reason.
- Do not rewrite `LegacyAgent` broadly.
- Do not break neural-network training.
- Do not make movement code directly execute combat actions.
- Do not treat `master` as the main development branch.
- Do not turn docs cleanup into gameplay rewrite work.

## Combat design principles

1. Win fights through timing, spacing, pressure, and survival.
2. Vanilla hit timing matters.
3. The bot should understand weapon commitment.
4. The bot should not spam low-value attacks.
5. The bot should punish obvious human mistakes:
   - eating in range
   - bowing too close
   - shielding predictably
   - running at low HP
   - missing a swing
   - overcommitting
6. The bot should disengage when low, heal when legal, and re-enter safely.
7. Mace, crystal, anchor, trident, elytra, cobweb, and pearl behavior must not compensate for weak melee fundamentals.

## Movement design principles

Movement is combat-informed but not combat-authoritative.

Movement may consume combat intent:

- desired range
- urgency
- branch family
- crit setup request
- sprint-hit request
- hold-position request
- committed phase state
- weapon range
- target velocity and bot velocity
- obstruction/reachability data

Movement may report movement state:

- sprinting
- falling
- retreating
- strafing/circling
- approach speed
- facing
- just jumped

Movement must not directly:

- attack
- punch
- block
- use items
- select hotbar slots
- apply loadouts
- throw pearls
- fire projectiles
- place or detonate crystals
- place or detonate anchors
- place cobweb/lava
- call combat behavior internals

## Code design principles

- Preserve known working behavior before tuning.
- Prefer constants and narrow helper methods over broad rewrites.
- Keep behavior changes reviewable.
- Keep debug output useful but not noisy.
- Keep source-of-truth clear:
  - source code defines runtime truth
  - docs define intent and workflow
  - wiki pages are reference unless verified current
- Build with `./gradlew build -q`.
- Do not use `shadowJar`, `reobfJar`, or `gradlew clean`.

## Priority order

1. Build stability
2. Correct bot lifecycle
3. 1v1 movement and spacing
4. Vanilla hit timing
5. Sword, axe, and shield fundamentals
6. Defensive recovery
7. Punish logic
8. Controlled advanced tools
9. Docs and release polish

## Good bot behavior

A good bot:

- Acquires the correct target.
- Holds dangerous melee range without face-hugging.
- Strafes and adjusts position.
- Backs up when too close.
- Waits for useful attack charge.
- Uses axe pressure against shields.
- Does not freeze while trying to crit.
- Heals or retreats when low.
- Chases a low-HP player.
- Punishes eating, bowing, and predictable shielding.
- Uses trident, mace, pearl, cobweb, crystal, and anchor only when the situation justifies it.
- Keeps fighting after a failed special move.

## Bad bot behavior

A bad bot:

- Runs straight into the player.
- Spams weak attacks.
- Freezes while waiting for perfect crit conditions.
- Lets the player eat for free.
- Uses advanced tools instead of basic positioning.
- Swaps items constantly.
- Gets stuck in committed phases.
- Dies while trying to execute flashy logic.
- Breaks neural-network mode or training behavior.
