# Review Checklist

## Vision alignment

- [ ] Change improves 1v1 PvP quality or repo governance.
- [ ] Change does not optimize for bot swarms first.
- [ ] Change does not prioritize flashy mechanics over fundamentals.
- [ ] Change keeps advanced tools subordinate to movement/melee basics.

## Scope

- [ ] One problem only.
- [ ] No unrelated files.
- [ ] No drive-by formatting.
- [ ] No broad package moves.
- [ ] No release-flow edits during combat work.

## Build

- [ ] Built with `./gradlew build -q`.
- [ ] Did not use `shadowJar`.
- [ ] Did not use `reobfJar`.
- [ ] Did not use `gradlew clean`.
- [ ] If cache issue occurred, only `buildSrc/build` was deleted.

## Runtime

- [ ] Bot spawns.
- [ ] Bot targets player.
- [ ] Bot moves.
- [ ] Bot fights.
- [ ] Bot can be reset.
- [ ] Loadout applies correctly.
- [ ] Debug output is usable.

## 1v1 behavior

- [ ] Holds range.
- [ ] Strafes.
- [ ] Backs up when too close.
- [ ] Waits for useful attack charge.
- [ ] Does not freeze for crit.
- [ ] Punishes eating/bowing/shielding.
- [ ] Recovers at low HP.
- [ ] Advanced tools are situational.

## Neural-network preservation

- [ ] Movement-only contract preserved.
- [ ] Brain schema not changed unless intentional.
- [ ] Legacy mode not broken.
- [ ] Movement-controller mode still works.
- [ ] Training commands still parse.

## NMS/Paper safety

- [ ] No risky simplification.
- [ ] Inventory writes use known safe path.
- [ ] MockConnection assumptions preserved.
- [ ] Version-specific code not generalized casually.

## Debug/logging

- [ ] Debug is opt-in or low-noise.
- [ ] Logs include useful duel information.
- [ ] Logs do not spam every tick without reason.

## Documentation

- [ ] Docs updated if behavior changed.
- [ ] Wiki not treated as current strategy unless rewritten.
- [ ] Any stale docs are marked stale.

## Revert criteria

Reject or revert if:

- it changes unrelated files
- it rewrites LegacyAgent broadly without reason
- it touches NMS fallback code without a runtime-specific reason
- it makes mace/crystal logic compensate for bad movement
- it breaks neural-network behavior
- it cannot explain how to test the behavior
- it only compiles but has no runtime test plan
- it follows the old wiki strategy instead of the 1v1 strategy
