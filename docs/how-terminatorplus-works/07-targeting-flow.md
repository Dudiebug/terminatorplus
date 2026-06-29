# 7. Targeting Flow

This section explains how a bot decides who or what it is trying to fight.

## Targeting is still owned by `LegacyAgent`

Target acquisition is not owned by `CombatDirector`.

It is still implemented in `LegacyAgent.locateTarget(...)`, which means the
legacy layer remains responsible for one of the most strategically important
runtime decisions in the plugin.

This is a major reason `LegacyAgent` cannot be treated as dead weight yet.

## Core targeting model

The plugin uses an `EnumTargetGoal`-driven target-goal model.

Depending on manager/bot configuration, the bot may search for:

- nearest player
- nearest vulnerable player
- specific player
- nearest hostile
- raider
- mob
- another bot
- bot with different name prefix/identity semantics
- custom mob-list target

This is much broader than the current narrow 1v1 strategy, but it is still
real runtime behavior.

## Target lookup flow

The effective targeting flow inside `LegacyAgent` is:

1. Determine the current target goal mode.
2. Build or select the relevant candidate pool.
3. Filter candidates by type and validity rules.
4. Apply distance or weighted distance logic.
5. Respect per-bot or manager-level target locks/settings when present.
6. Fire `TerminatorLocateTargetEvent`.
7. Use the event-adjusted target or honor cancellation.

## Region weighting and goal behavior

`LegacyAgent` does not only use raw nearest-distance logic.

It can also apply region-weighted distance through helper logic such as
`getWeightedRegionDist(...)`.

That means targeting can be influenced by:

- proximity to a configured region
- manager state changed through `/bot settings`
- preset application that modifies target-related manager settings

For the current duel-focused direction, this is more capability than the plugin
ultimately needs, but it is still part of the live targeting path.

## Event-based target override

After `LegacyAgent` picks a target candidate, the plugin fires
`TerminatorLocateTargetEvent`.

This event allows:

- canceling targeting for that tick
- replacing the chosen target

Architecturally, this is the cleanest target override extension point in the
repo.

That matters because a future duel-focused target policy could be layered in
through a clearer interface, but right now this event is one of the only
official seams.

## Interaction with bot-specific target state

Some command paths and bot state can influence targeting directly, including:

- `/bot settings setgoal ...`
- region-related settings
- per-bot target UUID/name-based behavior
- custom mob-list configuration through `/botenvironment`

That means target selection is not purely local to the agent. It is shaped by
runtime state spread across commands, manager flags, bot state, and event
hooks.

## What this means for the current 1v1 strategy

The current strategic direction wants a focused duel bot, but the targeting
system still reflects broad-plugin history.

So the right interpretation is:

- the targeting stack appears old broad-plugin behavior
- it is still runtime-critical
- it should be documented and possibly wrapped later
- it should not be deleted yet

## Likely mismatch areas

Several targeting features look more like old general-purpose sandbox behavior
than current duel-core behavior:

- broad non-player target modes
- custom mob-list mechanics
- region weighting
- gather/multi-bot oriented assumptions

That does not make them fake or unused. It makes them strategy-mismatched.

## Runtime-test-sensitive parts

The following targeting conclusions should be treated carefully:

- exact behavior when target-specific settings interact with presets
- gameplay effect of region weighting in active duels
- external plugin usage of `TerminatorLocateTargetEvent`

Those are better marked `needs runtime test` than asserted from source alone.
