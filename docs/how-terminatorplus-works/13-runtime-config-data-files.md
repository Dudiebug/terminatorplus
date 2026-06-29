# 13. Config, Data, and Files Written at Runtime

This section explains which files and config areas matter at runtime and what
they are used for.

## Why runtime file mapping matters

A subsystem that reads or writes data in the plugin folder is usually more
real than a subsystem that only appears in old docs.

Runtime file mapping helps answer:

- which systems are actually persistent
- which systems are training-only but still active
- which systems are debug/admin surfaces
- which docs can be archived first without affecting runtime state

## Runtime config and data table

| Path/key | Written/read by | Purpose | Current/legacy status |
|---|---|---|---|
| `plugins/TerminatorPlus/config.yml` | `TerminatorPlus.saveDefaultConfig()` and runtime config reads | Default plugin settings | Active/current |
| `config: ai.movement.enabled` and related keys | movement-controller runtime and AI command surfaces | Enable/configure movement-controller behavior | Active/current |
| `config: ai.movement.bank.*` | movement brain persistence | Configure manifest/brain directories, fallback behavior, autosave, quarantine | Active/current |
| `config: ai.legacy-brain-path` style keying | legacy AI import path handling | Bridge older brain storage into current repo behavior | Active but legacy/protected |
| `config: training.*` | AI training/evaluation flows | Max round time, curriculum, loadout mix defaults | Active/current training support |
| `plugins/TerminatorPlus/presets/*.yml` | `PresetManager` | Persist bot inventory/loadout/goal presets | Active/current |
| `plugins/TerminatorPlus/ai/movement/manifest.json` | `MovementBrainPersistence` | Movement bank manifest and schema metadata | Active/current |
| `plugins/TerminatorPlus/ai/movement/brains/*.json` | `MovementBrainPersistence` | Serialized fallback and specialist movement brains | Active/current |
| `plugins/TerminatorPlus/ai/brain.json` | legacy import code and docs/config references | Older brain import source | Active but legacy/protected |
| `plugins/TerminatorPlus/ai/movement/evaluations/` | evaluation/export tooling | Store evaluation outputs and reports | Training-only |
| `plugins/TerminatorPlus/debug/combat-all.log` | `CombatDebugger` | Aggregate combat debug trace | Optional/debug/admin |
| `plugins/TerminatorPlus/debug/<bot>.log` | `CombatDebugger` | Per-bot combat debug trace | Optional/debug/admin |
| external `https://api.mclo.gs/1/log` | `MainCommand.debugInfo()` through `MCLogs` | Upload debug text externally | Optional/debug/admin |

## `config.yml`

The default config is important because it reveals what the plugin believes is
runtime-configurable today.

Major config themes include:

- movement-controller enablement and shape
- movement bank persistence paths
- fallback and autosave policy
- legacy brain import path
- debug logging toggles
- training/evaluation defaults

This makes the config file a useful architecture summary in its own right.

## Preset files

Preset YAML files are one of the clearest examples of a feature that is both
active and easy to underestimate.

They persist:

- equipment/loadout state
- hotbar state
- manager-affecting settings

So a preset file is not just a cosmetic shortcut. It can alter live bot
behavior when applied.

## Movement brain persistence files

The movement bank files are the clearest evidence that the newer
movement-controller system is not just an experiment bolted onto runtime.

It has:

- stable storage paths
- schema handling
- manifest metadata
- multiple brain files
- fallback rules

That is the shape of a maintained subsystem.

## Evaluation outputs

Evaluation outputs should be treated differently from presets or core config.

They are:

- real
- maintained enough to be exposed by commands
- useful for AI/training workflows

But they are not part of the normal duel-bot runtime path.

So they belong in "training/debug support" docs, not in the primary
"how to fight a bot" narrative.

## Debug logs

`CombatDebugger` writes files into the plugin debug folder only when enabled.

That means:

- the logging subsystem is active
- it is optional
- its file format/path stability may matter to existing debugging workflows

So debug log paths are not a deletion target just because they are not part of
the normal player path.

## Documentation implication

A clean docs split should treat runtime file areas like this:

- core runtime config
- persistent user/runtime data
- AI/training persistence
- debug/admin outputs
- legacy import compatibility

That is much clearer than treating all plugin-folder files as one flat feature
set.
