# Wiki Reorganization Plan

## Problem

The `wiki/` folder contains useful technical documentation, but it largely reflects the old broad-feature strategy. That makes it dangerous as a strategy source for Codex or subagents.

## Decision

Do not delete `wiki/` immediately.

Reclassify it:

```text
wiki/ = legacy/reference docs unless explicitly rewritten for the new 1v1 strategy
```

## New rule

Source code defines runtime truth.
Current strategy docs define product direction.
Wiki pages are reference until verified and rewritten.

## Recommended wiki structure

```text
wiki/
  README.md
  Current-Strategy.md
  Combat-Movement-Contract.md
  Duel-Testing.md
  Legacy-Status.md
  legacy/
    Commands.md
    Loadouts.md
    Combat-Behaviors.md
    Movement-Network.md
    Movement-Brain-Bank.md
    AI-Training.md
    Brain-Persistence.md
    Configuration.md
```

Alternative if preserving GitHub Wiki page links matters:

```text
wiki/
  README.md
  Current-Strategy.md
  Combat-Movement-Contract.md
  Duel-Testing.md
  Legacy-Status.md
  Commands.md
  Loadouts.md
  Combat-Behaviors.md
  Movement-Network.md
  Movement-Brain-Bank.md
  AI-Training.md
  Brain-Persistence.md
  Configuration.md
```

In that version, add a banner to old pages instead of moving them.

## Pages to keep as technical reference

- `Movement-Network.md`
- `Movement-Brain-Bank.md`
- `AI-Training.md`
- `Brain-Persistence.md`
- `Configuration.md`

These contain useful implementation knowledge.

## Pages to rewrite first

- `Combat-Behaviors.md`
- `Loadouts.md`
- `Commands.md`

These are most likely to pull agents back toward the old broad-feature strategy.

## Required banner for legacy pages

```markdown
> Legacy/reference notice:
> This page may describe the old general TerminatorPlus strategy.
> Current strategy is 1v1 PvP bot quality on `mc-26.1.2`.
> Use this page for technical reference only until it is verified against source code and runtime behavior.
```

## Required current-strategy summary

```markdown
Current strategy:
- one bot versus one skilled human PvPer
- movement/spacing first
- vanilla hit timing
- sword/axe/shield fundamentals
- defensive recovery
- punish logic
- advanced tools only after fundamentals are proven
- movement is combat-informed, not combat-authoritative
```

## Cleanup order

1. Add `wiki/README.md`.
2. Add `wiki/Legacy-Status.md`.
3. Add `wiki/Current-Strategy.md`.
4. Add `wiki/Combat-Movement-Contract.md`.
5. Add `wiki/Duel-Testing.md`.
6. Add banners to old pages.
7. Rewrite `Combat-Behaviors.md`.
8. Rewrite `Loadouts.md`.
9. Rewrite `Commands.md`.
10. Verify remaining technical pages against source code.
