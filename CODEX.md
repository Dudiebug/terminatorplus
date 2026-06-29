# TerminatorPlus - Codex Playbook

## Read this first

This is the canonical AI-agent playbook for TerminatorPlus.

Read this file before making repository changes. `CLAUDE.md` is only a
compatibility redirect for tooling or older sessions that still look for it.

This repository is currently focused on one strong 1v1 PvP bot versus one
skilled human PvPer. Broad bot features, old wiki strategy pages, and older
branch-flow guidance are reference material unless a task explicitly scopes
compatibility or archival work.

## Current source of truth

Source code defines runtime truth. Current strategy docs define product
direction. Wiki pages are legacy/reference material unless they explicitly say
they have been rewritten for the current 1v1 strategy.

Use this precedence when files disagree:

1. Current source code.
2. `CODEX.md`.
3. Current docs under `docs/`.
4. Current-strategy wiki pages.
5. Legacy wiki/reference docs.

If an older file conflicts with this playbook, do not follow the stale process.
Update or flag the stale doc in a scoped docs task.

## Active branch target

- Primary active target: `mc-26.1.2`.
- Current 1v1 PvP strategy work starts from `mc-26.1.2`.
- `master` is the default/display branch only, not the current development
  source of truth.
- `mc-1.21.11` is older compatibility/reference unless a task explicitly scopes
  that branch.

Do not route current-strategy work through the old
`mc-1.21.11 -> mc-26.1.2 -> master` flow.

## Build rules

Docs-only work does not require a build.

If a build is required, run only:

```bash
./gradlew build -q
```

The root `build` task assembles the plugin jar in `build/libs/`. This project
does not use ShadowJar for the final plugin artifact.

If Gradle reports Windows/buildSrc Kotlin DSL snapshot corruption such as
`class-attributes.tab` or generated accessor `.class` files that are not regular
files, delete only:

```bash
rm -rf buildSrc/build
```

Then rerun:

```bash
./gradlew build -q
```

Do not run `./gradlew clean` as a reflex. It is slower and does not fix the
known buildSrc corruption pattern.

## Commands that are forbidden unless explicitly scoped

Do not run these unless the user explicitly scopes a task that requires them:

```bash
./gradlew clean
./gradlew shadowJar
./gradlew reobfJar
```

Do not create branches, commits, pushes, releases, or PRs unless the user
explicitly asks.

## Branch rules

- Work on the branch named by the task. For current 1v1 strategy, that branch is
  `mc-26.1.2`.
- Do not treat `master` as the development source of truth.
- Do not treat `mc-1.21.11` as primary for current strategy work.
- If compatibility work is explicitly scoped, verify the relevant branch before
  editing and keep the change narrow.
- Do not cherry-pick from `master` as a shortcut for current strategy work.

## Runtime safety rules

Runtime code deletion is not allowed without tests.

Do not delete, broadly rewrite, or simplify protected runtime systems during
planning, docs work, or early Duel Core V2 work. Build success is not gameplay
success. A compile-only result does not prove the bot fights better.

Gameplay behavior claims without recorded runtime proof must be marked:

```text
needs runtime test
```

Examples that need runtime proof include claims that the bot spaces better,
punishes eating, recovers correctly, lands better hits, uses advanced tools
better, or survives longer.

## Paper/NMS safety notes

Keep these version-specific guardrails intact:

- `SynchedEntityData` extraction uses a fallback chain:
  `packAll()` method, then `Int2ObjectMap` field, then array field. Do not
  simplify this fallback chain.
- Do not hardcode the player skin-customization entity data slot. Use
  `net.minecraft.world.entity.player.Player.DATA_PLAYER_MODE_CUSTOMISATION`.
- Paper 26.x can roll back bot inventory writes that go through Bukkit container
  transactions when `MockConnection` never ACKs slot packets. For bot main
  inventory writes, preserve the known NMS direct-write pattern:

  ```java
  net.minecraft.world.entity.player.Inventory nmsInv = bot.getInventory();
  nmsInv.setItem(i, CraftItemStack.asNMSCopy(bukkitStack));
  nmsInv.setChanged();
  ```

- Armor and offhand paths may use the existing equipment-packet path where the
  current source already does so.
- `MockConnection` has Paper 26.x assumptions around packet listener fields and
  a sentinel loopback address. Do not rewrite it unless the task is explicitly
  scoped to a Paper runtime compatibility issue.

## Movement/combat authority contract

Movement is combat-informed, not combat-authoritative.

Movement may consume intent and observations such as desired range, urgency,
branch family, crit setup request, sprint-hit request, hold-position request,
committed phase state, weapon range, velocities, obstruction, and reachability.

Movement may report physical state such as sprinting, falling, retreating,
strafing/circling, approach speed, facing, and just-jumped state.

Movement code must not directly:

- attack or punch
- block or use items
- select hotbar slots
- apply loadouts
- place or detonate crystals
- place or detonate anchors
- throw pearls or other projectiles
- place cobweb or lava
- call weapon behavior internals

Combat policy and execution stay behind combat-owned code and, later, explicit
executor/adaptor boundaries.

## Current 1v1 PvP strategy

TerminatorPlus is focused on one strong 1v1 PvP bot versus one skilled human
PvPer on the `mc-26.1.2` target branch.

The current priority is duel quality:

- movement and spacing
- vanilla hit timing
- sword, axe, and shield fundamentals
- defensive recovery
- punish logic
- controlled advanced tools

Advanced tools such as mace, trident, pearls, cobwebs, crystals, anchors,
elytra, fireworks, and wind charges are tactical options only after fundamentals
work. They must not be used as proof of good PvP behavior without runtime duel
tests.

## Protected legacy systems

Do not delete or broadly rewrite these systems without explicit scope and
runtime tests:

- `LegacyAgent`
- `Bot`
- `BotInventory`
- `MockConnection`
- NMS access paths
- movement-controller code
- movement brain bank and persistence
- full-replacement neural-network mode
- loadout and preset application paths
- existing combat behavior classes

Route around protected systems through narrow adapters when possible. Preserve
working compatibility until replacement behavior is proven.

## Safe first tasks

Good early tasks are small, reviewable, and easy to validate:

- docs governance updates
- read-only replacement maps
- compile-only Duel Core V2 skeleton classes with no runtime hook
- duel test plans and metrics docs
- narrow debug labels or constants, only when scoped

Do not start with broad rewrites, package renames, NMS cleanup, command rewrites,
loadout rewrites, or runtime deletion.

## Required output from agents

For implementation work, report:

- files changed
- behavior changed, or confirmation that no behavior changed
- build result, or why no build was needed
- runtime test result, or `needs runtime test`
- risks
- follow-up tasks

For docs-only work, explicitly state whether source/build files were unchanged
and whether a build was not run.

## Acceptance criteria

A change is acceptable only when:

- it serves the 1v1 PvP strategy or repo governance
- it is scoped to the requested files and behavior
- protected legacy systems remain protected
- movement-only authority remains intact
- Paper/NMS safety notes remain intact
- build validation is run when source/build files change
- runtime gameplay claims are backed by recorded duel tests or marked
  `needs runtime test`

## Things not to do

- Do not use `master` as current source of truth.
- Do not treat `mc-1.21.11` as primary for current strategy work.
- Do not follow old branch-flow guidance that starts current work on
  `mc-1.21.11`.
- Do not run `shadowJar`, `reobfJar`, or `gradlew clean`.
- Do not edit Java, Gradle, commands, loadouts, NMS, `BotInventory`,
  `LegacyAgent`, `CombatDirector`, movement code, or neural-network code during
  docs-only governance tasks.
- Do not claim runtime gameplay improvement from documentation changes.
- Do not recommend deleting protected runtime systems without replacement,
  build proof, runtime duel proof, and a rollback plan.
