# How TerminatorPlus Works

This folder is a source-grounded architecture guide for the `mc-26.1.2`
direction of TerminatorPlus.

It is meant to let a human or another AI learn the plugin by reading these
files in order, without needing to immediately dig through the entire source
tree.

Scope rules for this guide:

- Source of truth is the `mc-26.1.2` branch direction.
- `master` is not treated here as the primary development branch.
- Old wiki pages under `wiki/` were left in place on purpose.
- This guide does not treat legacy wiki claims as runtime truth unless they
  are backed by source.
- This guide is descriptive, not a deletion plan.

Recommended reading order:

1. [01-scope-and-safety.md](./01-scope-and-safety.md)
2. [02-mental-model.md](./02-mental-model.md)
3. [03-repository-module-map.md](./03-repository-module-map.md)
4. [04-runtime-lifecycle.md](./04-runtime-lifecycle.md)
5. [05-bot-creation-flow.md](./05-bot-creation-flow.md)
6. [06-main-runtime-tick-flow.md](./06-main-runtime-tick-flow.md)
7. [07-targeting-flow.md](./07-targeting-flow.md)
8. [08-combat-flow.md](./08-combat-flow.md)
9. [09-movement-flow.md](./09-movement-flow.md)
10. [10-inventory-loadout-preset-gui-flow.md](./10-inventory-loadout-preset-gui-flow.md)
11. [11-command-map.md](./11-command-map.md)
12. [12-ai-training-persistence-flow.md](./12-ai-training-persistence-flow.md)
13. [13-runtime-config-data-files.md](./13-runtime-config-data-files.md)
14. [14-active-vs-legacy-vs-unused-matrix.md](./14-active-vs-legacy-vs-unused-matrix.md)
15. [15-old-horsenuggets-legacy-map.md](./15-old-horsenuggets-legacy-map.md)
16. [16-present-but-maybe-not-doing-anything.md](./16-present-but-maybe-not-doing-anything.md)

What this guide is optimized for:

- Understanding the runtime from plugin enable to bot removal.
- Understanding which systems are current, legacy, protected, optional, or
  suspicious.
- Understanding where the plugin is NMS-sensitive and where runtime tests are
  required before changes.
- Understanding the current narrow 1v1 direction without pretending the legacy
  stack has already been removed.

What this guide deliberately does not do:

- It does not recommend deleting runtime code now.
- It does not assume build success means gameplay success.
- It does not assume old docs are correct when source disagrees.
