# TerminatorPlus 6.2.9 - mc26.2

> **UNTESTED PRE-RELEASE — EXPECT BREAKAGE.** This build has not been tested on
> a live server. Keep a known-good build and a rollback plan available.

This pre-release fixes bots that stop respawning or keep falling out of the
world (issue 40).

## Fixed

- A bot whose respawn spot is blocked or missing is no longer deleted. It
  retries for about nine seconds, then respawns at the world spawn. Only if
  that also fails is it dropped, with a console message naming the bot and the
  spot.
- A bot that dies before it ever lands safely now respawns where it was
  created instead of silently never coming back.
- The nearby-spot search now looks for ground around the spawn's height
  instead of the top of the world, so bots no longer respawn on roofs or the
  Nether ceiling, and islands over the void are found.
- Bots that end up far below the world while still alive (for example on
  servers with void damage turned off) are killed after two seconds so they
  respawn instead of falling forever.

## Changed

- `/bot settings set-spawn` (`/bot setspawn`) now accepts mid-air spots. Bots
  respawn at exactly that point and drop to the ground without taking fall
  damage on that first landing.
- Set-spawn refuses spots with no ground below (void), spots inside blocks or
  hazards, and spots above lava, with a message saying why.
- The set-spawn point now sticks across deaths and also applies to bots that
  are dead and waiting to respawn. It still resets when the server restarts.

## Validation

- New unit tests cover spawn-point precedence, the floor search order, the
  retry policy and the set-spawn refusal messages.

Runtime verification on Purpur 26.2 is still needed (needs runtime test).
