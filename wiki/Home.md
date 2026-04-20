# TerminatorPlus Wiki

TerminatorPlus is a Paper plugin that spawns server-side player bots with combat AI, a full editable inventory, and a preset system. Unlike basic NPC plugins, each bot is an NMS `ServerPlayer` subclass — it takes and deals real damage, uses real items, and reacts to its surroundings.

## Pages

- [Installation](Installation)
- [Quick Start](Quick-Start)
- [Commands](Commands)
- [Loadouts](Loadouts)
- [Combat Behaviors](Combat-Behaviors)
- [Presets](Presets)
- [Inventory GUI](Inventory-GUI)
- [Neural Network Mode](Neural-Network-Mode)
- [API](API)
- [Troubleshooting](Troubleshooting)
- [Changelog](Changelog)

## What's New

- **Weapon-aware combat AI** — bots use swords, maces, tridents with momentum, wind charges, ender pearls, end crystals, respawn anchors, cobwebs, totems, and elytra gliding with firework boosts.
- **Full inventory editor** — `/bot inventory <bot-name>` opens a 54-slot chest you can edit like a real inventory.
- **Presets** — save a bot's loadout and behavior settings to YAML, then re-apply to any bot.
- **Neural-network training mode is preserved** — the AI pipeline only kicks in for non-training bots, so fitness scoring stays deterministic.
