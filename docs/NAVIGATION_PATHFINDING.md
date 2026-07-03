# Terrain Navigation Baseline

This branch replaces the old direct-vector legacy chase fallback with a terrain-aware navigator.

## Problem

The previous non-NN baseline moved directly toward the target. It did not know whether the target was behind a wall, over a hill, behind a door, or inside a box. Local obstruction checks then reacted to the block in front of the bot and started mining, which produced bad behavior:

- running into small walls before deciding what to do;
- trying to match the target's Y level instead of finding a climbable route;
- mining through hills that could be scaled;
- breaking doors/trapdoors/gates instead of using them;
- mining as a first reaction rather than as a planned last resort.

## New model

Navigation is now route-selected instead of obstruction-selected.

```text
movement request
  -> no-break A* search
       walk / diagonal / ascend / descend / safe drop / open doors / open trapdoors / open gates
  -> if no no-break path exists, optional break-enabled A* search
       same movement primitives + planned breach blocks
  -> path follower emits bot.walk(...), bot.jump(...), faceLocation(...)
```

The important rule is structural: mining is not present in the first search pass. A wall or hill will only be mined if the no-break route cannot reach a line-of-sight combat goal near the target.

## Integration

`LegacyMovementStrategy` now routes movement as follows:

1. Full-replacement NN keeps its previous behavior.
2. Movement-controller NN gets the first movement attempt.
3. If the movement-controller cannot move, the terrain navigator replaces the old legacy direct-vector fallback.
4. Plain legacy mode uses the terrain navigator first.
5. The old legacy movement path remains as an emergency fallback when navigation is disabled or explicitly configured to allow fallback.

## Goal semantics

The search does not path to the target's exact moving block. It predicts target motion for a small number of ticks and searches for a standable node near the target that has line of sight. This prevents the boxed-target bug where standing outside the wall is incorrectly considered “close enough.”

## Movement primitives

The first pass supports:

- cardinal walking;
- diagonal walking with corner-clipping prevention;
- one-block ascent for hills/stairs-like terrain;
- descent and bounded safe drops;
- door opening;
- trapdoor opening;
- fence-gate opening;
- hazard avoidance.

The second pass adds:

- planned block breach for feet/head clearance;
- high cost for hard materials;
- practical forbids for bedrock, barriers, command blocks, structure blocks, and portal frames.

## Config

```yaml
ai:
  navigation:
    enabled: true
    apply-to-movement-controller-fallback: true
    suppress-legacy-fallback: true
    allow-break-fallback: true
    open-doors: true
    open-trapdoors: true
    open-gates: true
    avoid-hazards: true
    max-nodes: 1800
    replan-interval-ticks: 8
    target-predict-ticks: 6
    max-drop-blocks: 3
    max-search-distance: 48.0
    goal-radius: 3.3
    line-of-sight-goal-radius: 5.5
```

`allow-break-fallback` controls whether mining can ever appear in the route. `suppress-legacy-fallback` prevents the old local obstruction-mining code from taking over when no route is found.

## Expected behavior

| Scenario | Expected behavior |
| --- | --- |
| 3x3 wall between bot and target | Route around the wall if reachable. |
| Target above a hillside | Ascend the terrain instead of mining through the hill. |
| Closed wooden door | Open and continue. |
| Closed trapdoor/gate | Open and continue if the clearance path needs it. |
| Target inside a sealed box | No-break path fails; break-enabled path selects the cheapest breach. |
| Lava/fire/magma/powder snow in route | Avoid during search when hazard avoidance is enabled. |

## Follow-up work

This PR establishes the baseline. Future improvements should focus on:

- non-instant planned mining animation using the existing crack packet pipeline;
- jump-gap and parkour timing primitives;
- smarter stair/slab collision modeling;
- telemetry for selected path mode and breached blocks;
- arena tests that assert no-break routes are preferred over mining.
