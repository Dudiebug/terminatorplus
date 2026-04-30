# Movement Network

The movement-controller network handles footwork only. It never selects weapons,
changes hotbar slots, attacks, blocks, uses items, places crystals or anchors, or
executes utility combat actions. `CombatDirector` owns combat; movement brains
own locomotion.

For the full bank architecture, persistence layout, training mixes, evaluation
commands, and regression guard, see [Movement Brain Bank](Movement-Brain-Bank).

## Tick Flow

1. `CombatDirector.plan(...)` creates a typed `CombatIntent`.
2. `MovementBrainRouter` selects a bank brain by committed lock, then
   `CombatIntent.branchFamily`, then range/role fallback, then
   `general_fallback`.
3. `MovementInput.from(...)` creates the normalized observation vector.
4. `MovementNetwork.evaluate(...)` produces movement preferences.
5. `MovementOutputApplier.tryApply(...)` applies walking, jumping, sprinting,
   retreating, strafing, and facing adjustment only.
6. `MovementState` is reported back to `CombatDirector.execute(...)`.

## CombatIntent Fields

| Field | Purpose |
| --- | --- |
| `branchFamily` | Semantic `MovementBranchFamily` selected by CombatDirector |
| `playId` | Optional scanner/play telemetry |
| `desiredRange` / `desiredRangeBand` | Desired spacing and coarse range band |
| `rangeUrgency` | How urgent the spacing correction is |
| `lockFamily`, `lockReason`, `lockUntilTick`, `interruptible` | Commit/latch ownership |
| `plannedAction` | Debug/action label consumed by the stored plan |
| `wantsCritSetup`, `wantsSprintHit`, `wantsHoldPosition` | Movement hints for legal combat timing |
| `isCommitted`, `commitProgress`, `weaponRange` | Current phase and range metadata |

## Input Schema

The current observation schema has 37 values:

- relative position and distance
- bot and target velocity
- facing dot/cross
- grounded/falling/sprinting/recent-jump flags
- reachability/obstruction flags
- desired range and urgency
- crit/sprint/hold intent flags
- commit state and weapon range
- one-hot `branchFamily.*` fields for every `MovementBranchFamily`

The observation schema hash is exported in the brain manifest and evaluation
reports. Changing fields requires a schema/hash change and retraining or reset.

## Output Schema

The network outputs 8 movement values:

| Output | Effect |
| --- | --- |
| `forwardPressure` | Move toward or away from target |
| `strafePressure` | Left/right pressure |
| `jumpDesire` | Jump when over threshold and grounded |
| `sprintDesire` | Sprint when over threshold and not retreating |
| `retreatDesire` | Prefer backing out |
| `facingAdjustment` | Small yaw adjustment |
| `urgency` | Movement speed multiplier |
| `holdPosition` | Cooperate with Director hold requests |

## Fallbacks

Missing, incompatible, or invalid specialist brains fall back to
`general_fallback`. Missing or corrupt files are reported by `/ai brain status`
and by `/ai evaluate` exports. Non-network bots spawned through `/bot create`
continue using legacy movement.

