# TerminatorPlus 6.3.0 - mc26.3

> **UNTESTED PRE-RELEASE — EXPECT BREAKAGE.** This build has not been tested on
> a live server, and Paper 26.3 itself is still on Paper's beta channel. Keep a
> known-good build and a rollback plan available.

This pre-release moves TerminatorPlus to Paper 26.3 (issue 41). It includes
every fix from 6.2.9, including the respawn fixes for issue 40.

## Changed

- Built against the Paper 26.3 dev bundle and API, with Java 25.
- Built against Authlib 10, which Paper 26.3 ships.
- The version check now accepts 26.3 hotfixes such as 26.3.1, so a server
  hotfix no longer disables every command.
- Bot punches use the arm-swing call vanilla now uses for players. The swing
  animation follows the held item. (needs runtime test)

## Compatibility

- Requires Paper 26.3 or a 26.3.x hotfix. Servers on Paper or Purpur 26.2 should
  stay on 6.2.9 from the `mc-26.2` line.
- Spigot and CraftBukkit are not supported.
