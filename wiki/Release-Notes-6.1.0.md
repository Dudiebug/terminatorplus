# TerminatorPlus 6.1.0 - Legal Action Migration + Live Duel Proof

TerminatorPlus 6.1.0 is a focused follow-up to the 6.0.0 architecture release. It does not redo the movement-input v3, baseline movement, residual mixer, or player-like action controller foundation. It uses those pieces to make remaining direct shortcuts more visible and more player-like.

## What 6.1.0 Fixes Over 6.0.0

- Splash self-heal no longer both spawns a potion and applies an immediate duplicate heal.
- Splash offense, splash self-heal, cobweb placement, ender pearl throws, and trident releases now reserve action-controller phases before direct release.
- Movement output is constrained while timed consumable, drink, projectile, pearl, placement, and mining actions are active, without changing the movement input schema.
- Legacy mining progress now uses a small tool/block speed model for common obstacle blocks instead of a fixed crack loop.
- Environment fall-block/clutch behavior is recorded as fall-clutch telemetry; mace-in-hotbar fall immunity is not used.
- Runtime fights now accumulate `LiveDuelMetricsSnapshot` data for damage, spacing, fallback/held movement, route thrash, retreat, and action-legality counters.
- Movement rewards now penalize fake/direct action shortcuts, instant consumes, illegal same-tick actions, and interruptions while rewarding completed legal heals.

## Action Legality Improvements

- `PlayerLikeActionController` now tracks heal completion and cancel counts.
- Splash actions use a short throw window and release callback.
- Cobweb placement uses a short placement window before the remaining direct `setType` release.
- Pearl throws select the pearl, reserve `USING_PEARL`, release after a short delay, decrement once, and restore the previous weapon.
- Trident charge starts `THROWING_TRIDENT`; direct projectile spawning is counted honestly at release.

## Live Metric Improvements

- `LiveDuelMetricsRecorder` records runtime damage dealt/taken, desired-range ticks, too-close/too-far ticks, movement fallback/held ticks, route thrash, retreat success/failure, and action-legality counters.
- Report-only `/ai evaluate` exports remain honest: live metrics are still null when no live duel ran.
- Movement evaluation report schema is now version 3 so consumers can distinguish report-only data from live metric payloads.

## Known Limitations

- Runtime duel validation is still required for all gameplay claims in this release.
- Wind charge, crystal, and anchor action sequences still use direct shortcut telemetry.
- Bow, crossbow, firework, and full elytra legality rewrites are not included.
- Full Duel Core V2 runtime replacement is not included.
- Cobweb, pearl, trident, and splash still rely on direct Bukkit/Paper release primitives after their action phase; they are measured, not fully vanilla packet-perfect.

## Validation

- Build: `./gradlew build -q` passed on June 30, 2026 using Java 25 from the bundled workspace JDK and produced `TerminatorPlus-6.1.0-BETA-mc26.1.2.jar`.
- Runtime smoke tests: needs runtime test.

## Rollback Notes

The changes are concentrated in action behavior files, movement output application, live metric snapshot/recorder classes, the legacy mining crack loop, and release docs. Revert those files together if runtime testing finds action timing regressions.
