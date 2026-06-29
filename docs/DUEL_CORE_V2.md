# Duel Core V2

## Goal

Duel Core V2 is a new clean 1v1 PvP brain for TerminatorPlus.

It should make one bot better at fighting skilled human players through:

- movement and spacing
- vanilla hit timing
- sword/axe/shield fundamentals
- defensive recovery
- punish logic
- controlled advanced tools

Behavior-improvement claims remain `needs runtime test` until proven by recorded
runtime duel tests.

## Package

New V2 code should use:

`dev.dudiebug.terminatorplus.duel`

Do not mass-rename existing `net.nuggetmc.tplus` code during early V2 work.

## Principles

1. Movement is combat-informed, not combat-authoritative.
2. Combat policy decides actions.
3. Movement never attacks, uses items, selects hotbar slots, places blocks, or
   throws projectiles.
4. Paper/NMS risk stays behind old runtime adapters.
5. V2 migration must be staged.
6. Build success is not gameplay success.
7. All untested behavior claims are `needs runtime test`.

## Proposed package tree

```text
TerminatorPlus-Plugin/src/main/java/dev/dudiebug/terminatorplus/duel/
  DuelController.java
  DuelContext.java
  DuelSnapshot.java
  DuelDecision.java
  DuelTuning.java
  DuelMetrics.java
  BotActionExecutor.java
  ActionResult.java
  ActionType.java
  MovementIntent.java
  CombatPlan.java
  RecoveryPlan.java
  PunishPlan.java

  adapter/
    LegacyBotActionExecutor.java
    CombatIntentMapper.java

  debug/
    DuelDebugSink.java
```

## Class responsibilities

### DuelController

Owns one V2 decision tick.

It reads the context, builds a snapshot, chooses plans, emits a decision, and
invokes the executor.

It must not directly touch Paper/NMS, packet code, inventory internals, or
command parsing.

### DuelContext

Owns runtime references needed for one decision:

- bot
- target
- tick
- tuning
- metrics
- executor

It must not become a global mutable state bag.

### DuelSnapshot

Owns immutable observed facts:

- bot health
- target health
- distance
- target state
- bot movement state
- cooldown facts
- available equipment facts
- line-of-sight or reach facts

It must not execute behavior.

### DuelDecision

Owns the chosen output of a V2 tick:

- movement intent
- combat plan
- recovery plan
- punish plan
- selected action type
- reason/debug labels

It must not mutate the world.

### DuelTuning

Owns constants and thresholds.

It must not contain runtime counters or direct bot references.

### DuelMetrics

Owns measurements:

- time in desired range
- hit attempts
- hit success
- damage dealt
- damage taken
- missed swings
- successful retreats
- failed retreats
- punish windows
- special attempts
- special successes

It must not choose actions by itself.

### BotActionExecutor

Owns the safe action boundary.

Only executor implementations may call old runtime methods that attack, select
inventory slots, use items, place blocks, throw projectiles, or invoke legacy
behavior classes.

It must not decide strategy.

### ActionResult

Owns action outcome:

- success
- skipped
- failed
- reason
- cooldown or delay
- `needs runtime test` marker when applicable

### ActionType

Owns typed action identity.

Initial candidates:

- `NONE`
- `MELEE_ATTACK`
- `AXE_SHIELD_PUNISH`
- `RECOVER_HEAL`
- `RECOVER_PEARL`
- `PUNISH_EAT`
- `PUNISH_BOW`
- `CHASE_LOW_HP`
- `TRIDENT_THROW`
- `MACE_COMMIT`
- `COBWEB`
- `ADVANCED_CRYSTAL`
- `ADVANCED_ANCHOR`

This replaces string-based planned action routing over time.

### MovementIntent

Owns locomotion-only guidance:

- desired range
- minimum safe range
- maximum useful range
- range urgency
- retreat desire
- hold position
- strafe preference
- wants crit setup
- wants sprint hit
- committed action state

It must not contain executable combat actions.

### CombatPlan

Owns sword/axe/shield timing and contact choices.

It must not directly move or execute.

### RecoveryPlan

Owns low-HP and defensive reset choices.

It must not directly consume items; execution goes through `BotActionExecutor`.

### PunishPlan

Owns tactical punish choices against vulnerable target states.

It must not execute.

## Runtime integration stages

### Stage 0: docs only

Add this design and the archive/deprecation plan.

No behavior changes.

### Stage 1: compile-only skeleton

Add V2 classes and records.

No runtime hook.

Run:

```bash
./gradlew build -q
```

### Stage 2: adapter mapping

Add `CombatIntentMapper` so V2 `MovementIntent` can map into the existing
movement contract.

No behavior change unless explicitly scoped.

### Stage 3: debug-only shadow decision

Build V2 snapshots and decisions in debug mode, but keep existing
`CombatDirector` behavior authoritative.

Runtime result: `needs runtime test`.

### Stage 4: opt-in basic duel mode

Enable V2 only for basic sword/axe/shield behavior.

No crystals, anchors, mace, trident, cobweb, elytra, or scanner plays by
default.

### Stage 5: recovery and punish

Add controlled healing, retreat, pearl-away, re-entry, shield punish, eating
punish, bow punish, and low-HP chase.

### Stage 6: controlled advanced tools

Add advanced tools only after fundamentals pass runtime tests.

## Non-goals

- Do not rewrite `LegacyAgent` broadly.
- Do not rewrite `Bot`.
- Do not rewrite `BotInventory`.
- Do not touch MockConnection/NMS unless explicitly required.
- Do not mass-rename existing packages.
- Do not delete neural-network training code.
- Do not treat advanced tools as proof of good PvP behavior.

## Acceptance rule

A V2 behavior change is acceptable only when:

- build passes;
- runtime duel test is recorded;
- movement-only contract remains intact;
- no unrelated legacy behavior regresses;
- test result is documented.
