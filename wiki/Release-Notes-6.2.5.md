# TerminatorPlus 6.2.5 - Minecraft 26.2

This prerelease improves Movement V2 locomotion and autorespawn inventory restoration.

- Movement V2 aligns bot head and body yaw with each validated route vector.
- Validated routes sprint at full route speed, including normal, step-up, parkour,
  and movement-controller traversal; route holds stop sprinting.
- Autorespawn restores the last deliberately saved inventory instead of the
  depleted inventory at death, whether or not a loadout lock is active.
- Give, armor, loadout, preset, and inventory-editor saves update the respawn snapshot.

Artifact: `TerminatorPlus-6.2.5-BETA-mc26.2.jar`

This is a Paper 26.2 prerelease. Full live-server arena acceptance remains
environment-dependent.
