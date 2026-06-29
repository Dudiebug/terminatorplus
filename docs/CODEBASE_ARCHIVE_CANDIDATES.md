# Codebase Archive Candidates

## Purpose

This file separates current strategy, legacy/protected runtime code, optional
reference systems, and future Duel Core V2 replacement candidates.

Archive from default strategy does not mean delete.

## Matrix

| Area/File | Bucket | Reason | Current action |
|---|---|---|---|
| `CombatIntent` | Keep | Existing combat-to-movement contract. | Preserve. |
| `MovementState` | Keep | Existing movement-to-combat physical feedback contract. | Preserve. |
| `MovementOutputApplier` | Keep | Applies locomotion-only outputs. | Preserve. |
| `MovementBrainRouter` | Keep | Routes movement-only brain by combat intent. | Preserve. |
| `MovementBrainBank` | Keep but isolate | Useful movement brain infrastructure, not product strategy by itself. | Preserve as technical reference. |
| `CombatDirector` | Candidate for replacement by Duel Core V2 | Useful central authority, but broad policy is hard to tune. | Map and wrap before replacing. |
| `OpportunityScanner` | Archive from default strategy | Advanced play catalog can distract from melee fundamentals. | Keep as optional advanced toolbox. |
| `MeleeBehavior` | Keep | Core sword/axe/mace/trident contact behavior. | Tune only with runtime tests. |
| `ConsumableBehavior` | Keep but isolate | Recovery needed; policy should move into V2 recovery plan. | Wrap later. |
| `EnderPearlBehavior` | Keep but isolate | Useful recovery/chase tool; must be gated. | Wrap later. |
| Advanced behavior classes | Archive from default strategy | Controlled advanced tools, not baseline proof. | Preserve. |
| `LegacyAgent` | Legacy/protected | Runtime tick, target, movement, pathing, fallback behavior. | Route around; do not rewrite broadly. |
| Full-replacement NN mode | Legacy/protected | Compatibility/training path. | Preserve until explicitly removed. |
| `Bot` | Legacy/protected | NMS-backed bot runtime. | Wrap only. |
| `BotInventory` | Legacy/protected | High-risk inventory bridge. | Do not rewrite. |
| MockConnection/NMS code | Legacy/protected | Paper compatibility boundary. | Do not rewrite. |
| `PresetManager` | Keep | Useful preset persistence. | Preserve. |
| `BotCommand` | Keep but isolate | Broad command surface; current docs should show duel path first. | Preserve; refocus docs. |
| Old loadouts/loadout mixes | Archive from default strategy | Useful tests/reference, not default strategy. | Preserve. |
| Old wiki pages | Candidate for deletion only after replacement | Broad strategy docs can mislead agents. | Rewrite/mark legacy first. |

## Protected deletion list

Do not delete without explicit scope and runtime tests:

- `LegacyAgent`
- `Bot`
- `BotInventory`
- MockConnection/NMS code
- movement brain bank
- full-replacement neural-network mode
- advanced combat behavior classes
- loadout and preset code

## Required proof before runtime-code deletion

- `./gradlew build -q`
- bot spawn/render/removal test
- one-bot duel test
- loadout apply test
- preset apply test
- movement-controller movement test
- legacy-mode smoke test or explicit removal scope
- rollback plan

## Current recommendation

Do not delete runtime code now.

First complete documentation governance, then add a compile-only Duel Core V2
skeleton, then produce a replacement map.
