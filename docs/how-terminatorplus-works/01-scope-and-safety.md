# 1. Scope and Safety

This guide documents how TerminatorPlus currently works, using the
`mc-26.1.2` branch direction as the intended source of truth.

## What this documentation is for

This folder exists to answer four practical questions:

1. What the plugin is made of.
2. What actually happens at runtime.
3. Which systems are active and important.
4. Which systems look legacy, optional, or suspicious but still cannot be
   deleted casually.

The audience is:

- a repo owner trying to decide what to archive first
- a future Codex/AI session that needs fast architectural context
- a human maintainer who wants a reliable entry point before making changes

## Branch and evidence rules

This guide is written against the `mc-26.1.2` line of development.

That means:

- `master` is not treated as the authoritative current implementation branch
  for architecture decisions
- `mc-1.21.11` is not treated as the current primary branch except for
  compatibility concerns
- old wiki pages are treated as reference material, not as source of truth

When source and docs disagree, source wins.

Primary evidence came from these areas:

- root governance and planning docs such as `CODEX.md`, `README.md`, and
  `docs/*.md`
- build structure in `settings.gradle.kts`, root `build.gradle.kts`,
  `TerminatorPlus-Plugin/build.gradle.kts`, and
  `TerminatorPlus-API/build.gradle.kts`
- plugin metadata in `src/main/resources/plugin.yml`
- runtime classes such as `TerminatorPlus.java`, `Bot.java`,
  `BotManagerImpl.java`, `LegacyAgent.java`, `CombatDirector.java`,
  `MovementOutputApplier.java`, `BotInventory.java`, `PresetManager.java`,
  `AICommand.java`, and `BotCommand.java`

## Important safety assumptions

The plugin is still a hybrid system.

Newer systems exist:

- movement-controller neural-network flow
- `CombatDirector` and combat behavior classes
- movement brain bank and persistence
- richer inventory/loadout control

Older systems still remain in the runtime hot path:

- `LegacyAgent`
- `LegacyBlockCheck`
- `LegacyMats`
- full-replacement NN mode
- broad `/bot` command surface
- several admin/debug/training subsystems

Because of that, this codebase cannot be safely reasoned about as if it were
already a clean Duel Core V2 implementation.

## What must be treated carefully

The following areas are high-risk even if they look messy or outdated:

- NMS fake-player creation and removal in `Bot.java`
- connection shims in `MockConnection.java` and `MockChannel.java`
- entity-data reflection fallback in `NMSUtils.java`
- target acquisition and survival logic in `LegacyAgent.java`
- direct NMS inventory writes in `BotInventory.java`
- movement/combat boundary enforcement across `CombatDirector`,
  `CombatIntent`, `MovementInput`, `MovementOutputApplier`, and
  `MovementState`
- preset application and inventory GUI sync

These are not protected because they are elegant. They are protected because
they sit on live runtime paths and can break in ways that compile cleanly.

## How to use this folder safely

If you are planning docs cleanup:

- use this folder as your reference set
- treat `wiki/` as material to reclassify, not as runtime evidence

If you are planning code changes:

- map the relevant runtime path first
- assume Paper 26.x and fake-player behavior can be fragile
- mark uncertain gameplay conclusions as `needs runtime test`

If you are planning archive/deprecation work:

- archive docs first
- split current vs legacy/admin/training surfaces second
- only discuss runtime deletion after proof from source mapping and runtime
  tests
