# 15. Old HorseNuggets / Original-Plugin Legacy Map

This section identifies areas that likely come from the older broad
HorseNuggets/NuggetMC-style plugin direction, using careful wording.

This is not an authorship claim audit. It is a runtime and strategy mismatch
map.

## Legacy/original map

| Area | Why it appears legacy/original | Still needed? | Archive strategy |
|---|---|---|---|
| `LegacyAgent` | Large mixed-responsibility class for targeting, movement, survival, and fallback melee; style matches older broad-plugin behavior | Yes, runtime-critical | Document as protected legacy core; do not delete yet |
| `LegacyBlockCheck` | Heuristic clutch/block-place helper integrated into survival flow | Yes | Keep, but treat as compatibility behavior |
| `LegacyMats` | Large hand-maintained material taxonomies and mutable solid override set | Yes | Keep as compatibility adapter/reference layer |
| Broad target-goal system | Supports many target types beyond duel-focused player-vs-player | Yes, but strategy mismatch | Archive from default docs first |
| `/bot multi` and gather-style surfaces | Reflect multi-bot sandbox behavior more than focused duel training | Partly | Reclassify as broad/legacy/admin in docs |
| `/botenvironment` | Custom material and custom mob-list runtime mutation is broad-environment tooling, not narrow duel-core | Yes for compatibility | Move out of default-user docs path |
| Full-replacement NN mode | Older all-in AI experimentation style | Yes, but not current strategy | Mark as legacy/protected in docs |
| Wide loadout variety and loadout mixes | Fits older experimentation and broad feature surface | Partly | Keep code, archive from main strategy docs |
| Old wiki strategy tone | Talks like a wide feature plugin rather than a narrow duel bot | No, as primary truth | Archive/relabel first |
| Some advanced tactical overlays | Broad combat toolbox rather than strict fundamentals-first baseline | Yes today | Keep runtime, later classify as optional or modular |

## How to use this map correctly

The right interpretation is not:

- "legacy means remove it now"

The right interpretation is:

- some subsystems clearly reflect an older broad-plugin design
- many of them are still on the runtime path
- the first safe step is to archive them from the default strategy/docs, not
  from code

## Strong examples of current strategy mismatch

The clearest mismatch areas are:

- broad non-player target goals
- multi-bot/admin sandbox command surfaces
- loadout-mix and variety-heavy behavior
- broad wiki pages that read as if all these features are equally central

Those are the cleanest first archive candidates because they can be
reclassified in docs without risking runtime breakage.

## Runtime-critical legacy is still real

The biggest mistake a future maintainer could make is assuming that because a
class is named `Legacy*`, it is already dormant.

That is false here.

Examples:

- `LegacyAgent` is still the live top-level orchestrator
- `LegacyBlockCheck` still protects survival/clutch behavior
- `LegacyMats` still influences non-legacy code paths too

So the plugin currently contains legacy-shaped code that is also runtime-core.

## The practical archive lesson

The safest archive approach is:

- archive old strategy presentation first
- isolate broad/admin/training features in docs
- keep runtime legacy systems visible as protected dependencies

That gives the repo a more honest narrative without pretending migration is
already finished.
