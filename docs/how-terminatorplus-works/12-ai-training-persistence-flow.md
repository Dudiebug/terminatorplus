# 12. AI, Training, and Persistence Flow

This section explains the plugin's AI/training systems without pretending they
are all part of the normal player-facing runtime path.

## There are two AI families in the repo

Terminators currently support:

1. an older full-replacement neural-network path
2. a newer movement-controller neural-network path

The second one is more aligned with the current architecture. The first one is
still present, exposed in places, and should be treated as legacy/protected
rather than immediately removable.

## Full-replacement NN mode

The older path uses classes such as:

- `NeuralNetwork`
- `BotNode`
- `BotData`

The broad idea is that the network drives much more of the bot's movement and
control behavior directly.

Why it still matters:

- command surfaces still expose related modes
- `LegacyAgent` still contains mode branches for it
- deleting it would remove real behavior, not just dead docs

Why it is not the best model for the current direction:

- it is less aligned with a clean combat-vs-movement authority split
- it carries older broad experimentation assumptions
- it is not the clearest path toward a focused duel bot

## Movement-controller mode

This is the newer and more important NN path.

Its core idea is:

- let combat planning stay in `CombatDirector`
- let the network control locomotion only
- let observed `MovementState` feed back into combat execution

This avoids making the network the entire combat brain.

## Movement brain bank

`MovementBrainBank` stores the movement-controller brains used by the newer
system.

It keeps:

- one required fallback/general brain
- optional specialist brains keyed by branch-family IDs

The fallback requirement is important because it prevents the system from being
entirely dependent on every specialist family being present.

## Brain routing

`MovementBrainRouter` decides which brain to use for a given moment.

It can choose based on:

- explicit lock state
- branch family
- heuristic family selection
- fallback behavior

This gives the plugin a controlled kind of specialization instead of a single
monolithic movement model.

## Persistence

`MovementBrainPersistence` handles save/load/reset behavior for the movement
brain bank.

Runtime data paths include:

- `plugins/TerminatorPlus/ai/movement/manifest.json`
- `plugins/TerminatorPlus/ai/movement/brains/*.json`

Persistence logic is not just "dump one JSON file."

It also handles:

- manifest metadata
- schema/version compatibility
- fallback brain validity
- quarantine/reset behavior for bad data

That means movement persistence is a real subsystem with integrity concerns,
not an afterthought.

## Legacy brain import

The config and AI surfaces still reference an older import path:

- `plugins/TerminatorPlus/ai/brain.json`

This shows a bridge from older AI storage assumptions into the newer movement
bank model.

That import capability is another sign the repo is in transition rather than
already fully migrated.

## Training commands

`AICommand.java` is the main training/persistence/admin surface for AI.

Important command families include:

- reinforcement/training commands
- movement-brain management
- brain save/load/reset
- evaluation export
- stop/info helpers
- legacy/random/full-NN-related paths

These commands are active, but most of them are not part of the normal
"spawn a duel bot and fight it" path.

## Evaluation and reports

The movement-controller stack includes evaluation tooling and export/report
logic.

This is useful for:

- benchmarking movement behavior
- comparing brains or branch families
- producing training artifacts

But these are better classified as training/debug tooling than as core duel
runtime behavior.

## Schema stability matters

The newer movement-controller path depends heavily on stable schemas:

- movement input vector shape
- output interpretation shape
- branch-family IDs
- manifest metadata

Changing those casually can:

- break persistence
- invalidate existing brains
- create silent training/runtime mismatches

So even apparently small AI changes can have repo-wide migration costs.

## What appears current

Current and important:

- movement-controller runtime path
- movement brain bank
- movement brain persistence
- routing/fallback logic
- AI command support for movement-brain workflows

## What appears legacy/protected

Legacy/protected:

- full-replacement NN mode
- legacy training/random paths
- old import compatibility behavior
- fallback bridges back into legacy movement logic

## What appears training-only

Training-only or mostly training-oriented:

- reinforcement training commands
- evaluation export flows
- some route telemetry/reporting helpers
- mixed training scenarios and curriculum-oriented settings

## What should not be deleted yet

The correct current posture is:

- document the difference between current and legacy AI modes
- preserve both until proof exists
- prefer docs reclassification over code deletion

The movement-controller system is the architectural future, but it still lives
inside a codebase that preserves older AI machinery for real reasons.
