# 3. Repository and Module Map

This section explains what each major repo area is for and how the pieces fit
together.

## Top-level layout

At a high level the repo is composed of:

- a root Gradle project that assembles the final plugin jar
- a `TerminatorPlus-Plugin` module containing the Paper runtime
- a `TerminatorPlus-API` module containing public/internal API types, events,
  and the legacy agent layer
- a `buildSrc` folder containing custom Gradle convention/build logic
- `docs/` for current planning and architecture work
- `wiki/` for older and user-facing reference material

## Root project

Main files:

- `settings.gradle.kts`
- `build.gradle.kts`
- `src/main/resources/plugin.yml`

The root project is not just a wrapper. It owns several important behaviors:

- final packaging of the distributable jar
- plugin metadata expansion
- dependency bundling
- movement-only contract checking

### Why the root build matters

The repo's build path does not follow the most obvious "plugin module builds
the final jar directly" expectation.

Instead, the root `build.gradle.kts` is where several repo-specific decisions
live, including the `checkMovementOnlyContract` task that scans movement-layer
sources for banned combat-authority calls. That means the build is part of the
architecture, not just a packaging convenience.

## `buildSrc/`

This contains custom build logic and conventions used by the root/modules.

Practical meaning:

- the repo does not rely only on stock Gradle setup
- a broken `buildSrc/build/` cache can break the whole build even when Java
  code is fine
- versioning conventions are centralized here

This is why the repo's documented build workflow is strict and why
`buildSrc/build` corruption matters more than in many ordinary Java projects.

## `TerminatorPlus-Plugin/`

This is the live runtime module.

It contains:

- main plugin bootstrap in `TerminatorPlus.java`
- bot implementation in `bot/`
- combat logic in `bot/combat/`
- movement-controller logic in `bot/movement/`
- inventory/loadout/preset/gui systems
- command surfaces under `command/`
- NMS integration under `nms/`
- utility/runtime support classes

If something directly affects the behavior of a spawned bot on a server, it is
usually here.

### Important package groups inside the plugin module

`net.nuggetmc.tplus.bot`

- `Bot.java`
- `BotManagerImpl.java`
- bot runtime state and core fake-player behavior

`net.nuggetmc.tplus.bot.combat`

- `CombatDirector.java`
- combat intent, timing, snapshot, debugger, and behavior classes

`net.nuggetmc.tplus.bot.movement`

- movement-controller neural-network path
- movement input/output/state/router/apply layers

`net.nuggetmc.tplus.bot.loadout`

- item/loadout representation used by commands and inventory flows

`net.nuggetmc.tplus.bot.preset`

- preset persistence and apply logic

`net.nuggetmc.tplus.bot.gui`

- inventory editing GUI and listener sync logic

`net.nuggetmc.tplus.command`

- command registration plus concrete command classes

`net.nuggetmc.tplus.nms`

- low-level server integration helpers

## `TerminatorPlus-API/`

This module is more than a tiny public API jar.

It contains:

- public-facing bridge types such as `BotManager`
- internal bridge plumbing
- events
- agent abstractions
- the still-live `legacyagent` package
- legacy/full NN support types
- movement NN support types shared with runtime logic

### Why the API module is architecturally important

The name "API" can mislead readers into assuming it is mostly passive.
That is not true here.

The API module contains some of the most important runtime behavior:

- `Agent.java`
- `LegacyAgent.java`
- `LegacyBlockCheck.java`
- `LegacyMats.java`
- target goal and movement-related support classes

So this module is both interface layer and runtime behavior layer.

## Resources and metadata

The plugin metadata lives at the root resource path used by the root build:

- `src/main/resources/plugin.yml`

Important points from `plugin.yml`:

- plugin main class is `net.nuggetmc.tplus.TerminatorPlus`
- command roots are `bot`, `terminatorplus`, `ai`, and `botenvironment`
- permission roots are `terminatorplus.*`, `terminatorplus.manage`, and
  `terminatorplus.admin`

The default config used by `saveDefaultConfig()` defines movement bank paths,
training defaults, legacy brain import path, evaluation export location, and
movement-controller persistence behavior.

## `docs/` vs `wiki/`

`docs/` is the current internal planning and architecture area.

It contains repo-owner-facing material such as:

- `VISION.md`
- `WORKFLOW.md`
- `ROADMAP.md`
- `AUDIT.md`
- `DEPRECATION_PLAN.md`
- `CODEBASE_ARCHIVE_CANDIDATES.md`

`wiki/` is broader and more user-facing, but it reflects older eras of the
plugin too. It should be treated as a reference layer that needs
reclassification, not as a guaranteed accurate description of current runtime
truth.

## Runtime data locations

Documented runtime write locations include:

- `plugins/TerminatorPlus/presets/*.yml`
- `plugins/TerminatorPlus/ai/movement/manifest.json`
- `plugins/TerminatorPlus/ai/movement/brains/*.json`
- `plugins/TerminatorPlus/ai/movement/evaluations/`
- `plugins/TerminatorPlus/debug/*.log`

These paths matter because they show which systems are not just theoretical.
If a feature persists data or logs into the plugin data folder, it is usually a
real maintained subsystem, even if it is not part of the primary current
strategy.
