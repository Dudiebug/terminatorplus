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

## Interpretation

`legacy` is a history/strategy label, not a deletion signal: `LegacyAgent`,
`LegacyBlockCheck`, and `LegacyMats` remain runtime-relevant. Use this table
with the [status matrix](./14-active-vs-legacy-vs-unused-matrix.md) and archive
docs before considering code changes.
