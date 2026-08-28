# TerminatorPlus 6.2.1 - Paper 26.2

TerminatorPlus 6.2.1 adds complete in-game bot management while keeping
existing commands available for automation and compatibility.

## Highlights

- Adds `/bot scatter [radius]` with deterministic circular placement, safe
  unique destinations, direct teleporting, and all-or-nothing failure handling.
- Includes the current disabled-by-default Movement V2 candidate and adds the
  live `/bot movementv2 on|off|status` control with immediate legacy fallback.
- Makes `/bot` open the management UI for bot creation, status, teleport,
  respawn, movement, AI, combat, loadout, preset, environment, help, and admin
  actions. `/bot info` and every existing command remain available.
- Restores opt-in `/bot respawn true|false` using each bot's first safe grounded
  location as its permanent anchor, with blocked-anchor fallback and a small
  respawn poof.
- Refreshes open management screens in place every five server ticks and
  cleans up refresh/input state when menus close, players leave, bots vanish,
  or the plugin disables.

## Compatibility and scope

- Movement V2 remains off on a fresh installation.
- Legacy movement remains the fallback when Movement V2 is off or cannot own a
  route.
- `/bot gather` and unrelated command behavior are unchanged.
- The superseded PR #12 terrain-navigation implementation and Issue #21 changes
  are not included.

## Artifact

`TerminatorPlus-6.2.1-BETA-mc26.2.jar`

Use this prerelease with Paper 26.2 and Java 25. Movement V2 traversal and the
full in-game workflow still require live Paper acceptance testing before a
production rollout.
