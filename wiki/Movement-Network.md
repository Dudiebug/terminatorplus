# Movement Network

The movement-controller neural network handles **footwork only**. It controls how bots position themselves in combat — strafing, spacing, sprint/jump timing, approach/retreat, and facing. It does **not** select weapons, trigger attacks, or make combat decisions. The [CombatDirector](Combat-Behaviors) retains full authority over combat.

## What the NN controls vs. what the Director controls

| Movement NN | CombatDirector |
| --- | --- |
| Strafing and circling | Weapon selection |
| Spacing and range-seeking | Attack timing and charge gates |
| Sprint / jump timing | Mace, trident, and all combat actions |
| Approach / retreat | Opportunity evaluation |
| Facing adjustment | `bot.attack()` execution |
| Movement urgency | Consumables and passive behaviors |
| Hold-position cooperation | Combat state commits |

## How it works

Each tick (when movement-controller mode is active):

1. **CombatDirector.plan()** evaluates the combat situation and produces a **CombatIntent**.
2. **MovementInput.from()** builds a 30-value input vector from bot/target state and the CombatIntent.
3. **MovementNetwork.evaluate()** runs a forward pass through the feed-forward network.
4. **MovementOutputApplier.tryApply()** converts the 8-value output into actual bot movement (velocity, sprint, jump, facing).
5. **MovementState** is updated and fed back to the Director.
6. **CombatDirector.execute()** reads the MovementState to validate timing windows before executing attacks.

## CombatIntent (Director -> NN)

The CombatDirector tells the NN what it wants via CombatIntent:

| Field | Range | Purpose |
| --- | --- | --- |
| `desiredRange` | 0--64 | Target distance the Director wants |
| `rangeUrgency` | 0--1 | How urgently to close/open range |
| `wantsCritSetup` | bool | Request crit fall setup (jump then fall) |
| `wantsSprintHit` | bool | Request sprint-hit setup |
| `wantsHoldPosition` | bool | Request the bot to stay put |
| `isCommitted` | bool | Bot is in an active combat phase |
| `commitProgress` | 0--1 | How far through the commit phase |
| `weaponRange` | float | Effective range of the active weapon |
| `branchName` | string | Current weapon branch (melee, mace, trident, etc.) |

CombatIntent is a **hint** — the NN may choose different movement if its learned weights disagree.

## MovementState (NN -> Director)

After the NN moves the bot, MovementOutputApplier updates MovementState:

| Field | Purpose |
| --- | --- |
| `isSprinting` | Whether the bot is currently sprinting |
| `justJumped` | Whether the bot jumped this tick |
| `isFalling` | Whether the bot is falling |
| `isRetreating` | Whether the bot is moving away from target |
| `isCircling` | Whether the bot is moving laterally |
| `approachSpeed` | Forward velocity toward target |
| `currentFacing` | Normalized direction vector |

The Director uses this to validate timing windows. For example, it won't trigger a crit swing unless `isFalling` is true and descent velocity is sufficient.

## Network shape

- **Input:** 30 values (all normalized to [-1, 1])
- **Hidden layers:** [32, 24] by default (configurable via `ai.movement-network.hidden-layers`)
- **Output:** 8 values
- **Activation:** tanh with value clamping

### Input vector (30 values)

| # | Values | Source |
| --- | --- | --- |
| 1--3 | relX, relY, relZ | Relative position to target |
| 4--5 | distance, horizontalDistance | 3D and 2D distance |
| 6--9 | botVelX/Y/Z, botHorizontalSpeed | Bot velocity |
| 10--13 | targetVelX/Y/Z, targetHorizontalSpeed | Target velocity |
| 14--15 | facingDot, facingCross | Bot facing vs target direction |
| 16--19 | grounded, falling, sprinting, recentlyJumped | Bot state flags |
| 20--21 | reachable, obstructed | Line-of-sight checks |
| 22--23 | desiredRange, rangeUrgency | From CombatIntent |
| 24--26 | wantsCritSetup, wantsSprintHit, wantsHoldPosition | From CombatIntent |
| 27--28 | isCommitted, commitProgress | Combat phase state |
| 29--30 | weaponRange, branchCode | Weapon info (melee=0.2, mace=0.4, trident=0.6, pearl=0.8, other=1.0) |

### Output vector (8 values)

| # | Output | Range | Effect |
| --- | --- | --- | --- |
| 1 | forwardPressure | -1 to 1 | Movement toward (positive) or away from (negative) target |
| 2 | strafePressure | -1 to 1 | Left/right movement |
| 3 | jumpDesire | 0 to 1 | Desire to jump (threshold: 0.65) |
| 4 | sprintDesire | 0 to 1 | Desire to sprint (threshold: 0.55) |
| 5 | retreatDesire | 0 to 1 | Desire to retreat |
| 6 | facingAdjustment | -1 to 1 | Yaw rotation (scaled to pi/3 radians) |
| 7 | urgency | 0 to 1 | Movement speed multiplier (minimum 0.25) |
| 8 | holdPosition | 0 to 1 | Hold position compliance (threshold: 0.65) |

## Movement speeds

| State | Speed |
| --- | --- |
| Walking | 0.32 * urgency |
| Sprinting | 0.42 * urgency |
| Minimum | base * 0.25 (when urgency is very low) |

## Fallback behavior

The NN falls back to legacy movement when:

- `ai.movement-network.enabled` is `false` (default for normal bots)
- The brain file is missing or corrupt
- The network shape doesn't match the expected input/output dimensions
- Input validation fails (NaN or infinite values)

Fallback reasons are reported in the `ApplyResult`: `missing-bot`, `invalid-input`, `invalid-shape`, `disabled`.

When `ai.movement-network.fallback-to-legacy-movement` is `true` (default), a fallback bot seamlessly continues using legacy movement. When `false`, it stops moving.

## Modes

| Mode | Set by | Behavior |
| --- | --- | --- |
| **Legacy** | `/bot create` (always) | Classic movement logic, no NN involved |
| **Movement Controller** | `/ai movement` (deploy) or `/ai reinforcement` (train) | NN handles movement, CombatDirector handles combat |
| **Full Replacement** | `/ai reinforcement ... legacy` | Legacy NN replaces both movement and combat (original training pipeline) |

## Deploying a trained brain

`/bot create` bots **never use the movement NN** — they spawn with no network attached and always run legacy movement. The `ai.movement-network.enabled` config flag does not change this; it only controls fallback behavior for bots that already have a network attached.

To deploy a trained brain on production fighting bots, use **`/ai movement`**:

```
/ai reinforcement 120 TrainBot      # 1. train (autosaves best brain)
/ai stop                             # 2. end training
/ai movement 5 Soldier               # 3. spawn fighting bots that use the trained brain
```

Bots spawned via `/ai movement` are not training bots. They fight normally — the CombatDirector picks weapons and times attacks, and the trained NN handles their footwork. The plugin auto-loads `brain.json` from disk on startup, so you don't need to run `/ai brain load` after a server restart.

Note: `/ai brain load` only affects bots spawned **after** the load. Existing bots keep whatever network they had when they were spawned. To swap a brain on live bots, despawn and respawn them.

## Configuration

Key config values in `config.yml`:

```yaml
ai:
  movement-network:
    enabled: false                    # disabled for normal bots by default
    mode: movement-controller
    hidden-layers: [32, 24]
    tick-rate: 1                      # evaluate every N ticks
    fallback-to-legacy-movement: true
    movement-output-scaling: 1.0      # scale all movement outputs
    hold-position-behavior: true      # respect hold-position hints
    debug: false                      # log movement decisions
```

See [Configuration](Configuration) for the full reference.
