# 9. Movement Flow

This section explains all three movement styles present in the plugin and the
boundary between movement and combat authority.

## The plugin has three movement modes

Terminators do not all move through one system.

The repo currently contains:

1. legacy heuristic movement
2. full-replacement neural-network movement
3. movement-controller neural-network movement

Those modes coexist because the repo is in transition, not because the old
systems are gone.

## Legacy movement

Legacy movement lives mainly in `LegacyAgent.java`.

It is a reactive, heuristic, block-aware movement model rather than a modern
navigation engine.

Typical behaviors include:

- walk, jump, and face corrections
- obstacle checks
- fence/gate logic
- side/up/down block breaking
- vertical tower-up behavior
- mining down through standing blocks
- swimming and hazard responses
- cobblestone and MLG-style clutch support

This style is very "old broad-plugin behavior," but it is still live and
runtime-critical.

## Full-replacement NN movement

The old neural-network path uses classes such as:

- `NeuralNetwork`
- `BotNode`
- `BotData`

In this mode, the neural network drives a broader set of bot movement/control
behavior directly. It is closer to an "older experiment replaces the whole
movement brain" model than the newer movement-controller design.

This path still matters because:

- command surfaces still reference it
- fallback/legacy modes still preserve it
- deleting it prematurely would remove runtime behavior without enough proof

## Movement-controller NN movement

This is the newer movement design and the one most aligned with the current
duel-focused strategy.

The movement-controller path uses:

- `CombatIntent`
- `MovementInput`
- `MovementBrainRouter`
- `MovementNetwork`
- `MovementOutput`
- `MovementOutputApplier`
- `MovementState`

## Step-by-step movement-controller flow

### 1. Combat planning creates movement-relevant intent

`CombatDirector.plan(...)` creates a `CombatIntent`.

The intent expresses what combat wants from movement without directly choosing
or executing attacks from movement code.

Examples include:

- preferred pressure/range
- branch family
- tactical commitment state
- locomotion-relevant posture or lock assumptions

### 2. Movement input is assembled

`MovementInput.from(...)` builds the fixed-size input vector used by the
movement brain.

This pulls together:

- bot state
- target state
- world-relative movement context
- combat intent

The fixed vector shape matters because persistence, training, and schema
stability depend on it.

### 3. Brain routing chooses which movement brain to use

`MovementBrainRouter` decides which movement brain should evaluate the current
situation.

Its priority logic includes:

- explicit lock/family hints
- branch-family routing
- heuristic family selection
- fallback-brain selection

This is where the plugin supports specialization without losing a mandatory
fallback brain.

### 4. Network evaluation produces raw output

`MovementNetwork.evaluate(...)` produces raw numeric outputs from the selected
brain.

That output is not used directly by the game entity. It is interpreted by the
next layer.

### 5. Raw output becomes movement semantics

`MovementOutput.fromRaw(...)` translates raw values into semantic movement
decisions such as:

- forward/back pressure
- strafe
- sprint
- jump
- aim/rotation intent

This interpretation layer is important because it keeps the network output
schema stable and makes runtime application more explicit.

### 6. Output is applied to the bot

`MovementOutputApplier.apply(...)` converts movement semantics into actual bot
locomotion and writes back observed `MovementState`.

This is where the NN output becomes:

- movement inputs
- sprint/jump state
- observed locomotion state
- facing/look state

### 7. Combat reads observed movement state later

After movement is applied, `CombatDirector.execute(...)` can read
`MovementState` as observed context.

That is the key boundary:

- movement may influence combat context
- movement should not directly own attack legality or weapon choice

## How the movement/combat contract is protected

The contract is protected in two ways:

1. design intent in the runtime flow
2. build enforcement through `checkMovementOnlyContract` in the root build

The build task scans movement-layer sources for banned combat-authority calls.

That is unusually strong for a gameplay plugin and it reflects an important
repo-level rule:

- movement can inform combat
- movement cannot become the combat brain

## Where future changes could break the contract

Accidental contract breaks would likely happen if someone:

- lets movement code choose specific attacks directly
- lets movement code mutate inventory/loadout decisions
- bypasses `CombatDirector` and encodes combat branching inside router/network
  logic
- changes input/output/schema ordering casually
- removes fallback-to-legacy movement when controller apply fails

`LegacyAgent.move(...)` currently preserves a fallback path from controller
movement back to `moveLegacy(...)` when needed. That fallback is protective,
not dead code.

## Why this architecture is important for the future

The movement-controller design is one of the clearest expressions of the
current long-term direction.

It allows:

- learned locomotion specialization
- combat-aware spacing and pressure
- continued centralized combat legality

That is much closer to a future duel-core architecture than the old
full-replacement NN path, but it still depends on the legacy shell around it.
