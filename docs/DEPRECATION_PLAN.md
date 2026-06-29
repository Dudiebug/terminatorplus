# Deprecation Plan

## Purpose

This file defines how TerminatorPlus distinguishes current strategy, legacy
runtime support, archived reference material, and deletion candidates.

Deprecation does not mean deletion. In this repo, many legacy systems remain
runtime-critical until the new 1v1 Duel Core is proven by build checks and
runtime duel tests.

## Current branch truth

- Primary active development target: `mc-26.1.2`.
- Do not start current 1v1 strategy work from `master`.
- Treat `mc-1.21.11` as an older compatibility branch unless a task explicitly
  scopes compatibility work.

## Terms

### Keep

The code or document is part of the current strategy and should remain visible.

### Keep but isolate

The code or document remains useful, but should not define the default 1v1
strategy.

### Legacy/protected

The area is old or broad, but still runtime-critical. Do not rewrite or delete
without explicit scope and tests.

### Archive from default strategy

The area should not guide current strategy work, but may remain as reference or
optional tooling.

### Candidate for replacement by Duel Core V2

The area contains policy or behavior that V2 should eventually own or wrap.

### Candidate for deletion only after tests

Deletion is allowed only after replacement exists, links are migrated, build
passes, and runtime tests prove no regression.

## Protected runtime areas

Do not delete or broadly rewrite these during planning or early V2 work:

- `LegacyAgent`
- `Bot`
- `BotInventory`
- MockConnection/NMS code
- movement-controller code
- movement brain bank and persistence
- full-replacement neural-network mode
- loadout and preset application paths
- existing combat behavior classes

## Default-strategy archive candidates

These should be moved out of the default strategy narrative, not deleted:

- broad wiki combat behavior pages
- broad loadout catalog pages
- broad command catalog pages
- full-replacement neural-network strategy pages
- advanced-tool-first combat guidance
- loadout mixes designed to exercise every behavior

## Immediate governance actions

1. Use `CODEX.md` as the canonical AI-agent playbook.
2. Keep `CLAUDE.md` only as a compatibility redirect.
3. Keep `docs/DUEL_CORE_V2.md` as the clean future architecture plan.
4. Keep `docs/CODEBASE_ARCHIVE_CANDIDATES.md` as the source-level
   archive/protection matrix.
5. Keep `docs/NEXT_CODEX_TASKS.md` as the staged task queue.
6. Rewrite wiki quick-start and combat behavior pages after the docs land.

## Deletion gate

Before deleting runtime code, all of these must be true:

- Replacement exists.
- `./gradlew build -q` passes.
- One-bot duel runtime test passes.
- Movement-controller bot still moves.
- Legacy mode still works or the task explicitly removes it.
- Loadouts still apply.
- Presets still save/load/apply.
- Bot spawn/render/removal still works.
- The rollback plan is documented.

## Runtime-proof rule

Any gameplay improvement claim without a duel test result must be marked:

`needs runtime test`
