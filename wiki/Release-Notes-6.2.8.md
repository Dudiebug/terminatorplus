# TerminatorPlus 6.2.8 - mc26.2

> **UNTESTED PRE-RELEASE — EXPECT BREAKAGE.** This build has not been tested on
> a live server. We do not know what will break. Keep a known-good build and a
> rollback plan available.

This pre-release reduces the Movement V2 planning spikes reported in issue 39.

## Changed

- Fresh plan and replan attempts are spread across ten stable per-bot tick
  phases instead of allowing every bot to plan in the same server tick.
- Each planning context reads the loaded state of any of its nine chunks only
  once, instead of repeating the same Bukkit chunk check for every block query.
- `ai.movement.v2.replan-stagger-ticks` controls the spread. Set it to `1` to
  restore the old behavior; the default of `10` can delay a fresh plan by at
  most nine ticks.

The NPC UUID behavior introduced in 6.2.7 is unchanged.

## Validation still required

- Run the same approximately 100-bot scenario against 6.2.7 and this build.
- Capture matched 60-second Spark profiles after warmup.
- Compare Movement V2 planning time, chunk-loaded checks, tick percentiles,
  replan reasons, fallback count, and navigation correctness.
