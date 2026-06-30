# TerminatorPlus Roadmap

## Phase 0 — Repo safety and audit

Goal: establish source-of-truth and prevent agent drift.

Why it matters: the repo has old strategy docs and high-risk internals.

Allowed scope:
- docs
- audit
- workflow
- wiki status
- no behavior changes

Forbidden scope:
- gameplay rewrites
- release changes
- NMS changes

Done criteria:
- current strategy docs exist
- wiki is marked reference/legacy
- build command is documented
- branch target is clear

Prompt:
```text
Perform a read-only repo audit and identify stale strategy docs. Do not modify behavior.
```

Testing:
- docs review only
- no invented runtime results

## Phase 1 — Baseline duel kit and test arena

Goal: make testing repeatable.

Allowed scope:
- duel docs
- commands
- baseline loadout docs
- debug checklist

Forbidden scope:
- advanced tool tuning

Done criteria:
- reproducible duel setup
- pass/fail checklist
- metrics captured

## Phase 2 — 1v1 movement/spacing

Goal: make the bot hold useful range.

Allowed scope:
- movement constants
- movement-controller behavior
- debug metrics

Forbidden scope:
- attacks from movement code
- broad LegacyAgent rewrite

Done criteria:
- bot approaches
- bot strafes
- bot backs up when too close
- bot does not freeze

## Phase 3 — sword/axe/shield fundamentals

Goal: make basic PvP threatening.

Allowed scope:
- melee timing
- shield pressure
- axe selection
- charge checks

Forbidden scope:
- crystal/mace/trident compensation

Done criteria:
- fewer weak swings
- shield punish works
- bot keeps pressure

## Phase 4 — defensive recovery

Goal: make bot survive and reset.

Allowed scope:
- healing thresholds
- pearl escape
- re-entry
- low HP behavior

Forbidden scope:
- endless fleeing
- inventory rewrites

Done criteria:
- bot heals when legal
- retreats from unsafe range
- re-engages after recovery

## Phase 5 — punish logic

Goal: punish bad human states.

Allowed scope:
- eating/bowing/shielding/low-HP chase rules
- debug labels
- narrow behavior changes

Forbidden scope:
- unrelated advanced mechanics

Done criteria:
- bot closes on eating
- bot interrupts bowing
- bot pressures shields
- bot chases low HP

## Phase 6 — trident/mace tuning

Goal: make special weapons tactical.

Allowed scope:
- trident ranges
- mace commit/cancel
- cooldown tuning

Forbidden scope:
- using specials as basic melee replacement

Done criteria:
- specials trigger less often but convert better
- failed special does not break fight

## Phase 7 — crystal/anchor/elytra/cobweb advanced kit

Goal: add advanced tools after fundamentals work.

Allowed scope:
- advanced behavior gating
- self-damage safety
- fail-safe logic

Forbidden scope:
- bypassing movement/melee fundamentals

Done criteria:
- advanced tools are situational
- self-KO decreases
- melee remains functional

## Phase 8 — config/tuning externalization

Goal: move stable tuning constants into config where useful.

Allowed scope:
- selected proven constants
- docs
- config migration

Forbidden scope:
- exposing unstable internals too early

Done criteria:
- stable constants documented
- defaults match tested behavior

## Phase 9 — docs/release polish

Goal: make the repo clean and understandable.

Allowed scope:
- README
- wiki/current docs
- release notes
- examples

Forbidden scope:
- gameplay changes

Done criteria:
- docs match code
- old strategy is archived
- users understand current direction
