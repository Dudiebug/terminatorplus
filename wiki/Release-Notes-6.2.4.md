# TerminatorPlus 6.2.4 - Minecraft 26.2

This prerelease enables Movement V2 automatically.

- New installations start with Movement V2 enabled.
- Existing installations receive a one-time migration that enables Movement V2.
- After migration, `/bot settings movement-v2 off` remains persistent across restarts.
- Movement V2 can be inspected with `/bot settings movement-v2 status` and
  `/bot debug movement [bot-name]`.
- `/bot move scatter [radius]` no longer has a command-defined maximum radius;
  destination validity is still constrained by the world border.

Artifact: `TerminatorPlus-6.2.4-BETA-mc26.2.jar`

This is a Paper 26.2 prerelease. Full live-server arena acceptance remains
environment-dependent.
