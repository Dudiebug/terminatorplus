# TerminatorPlus 6.2.6 - mc26.2

This prerelease makes Movement V2 deterministic by default while retaining an
explicit neural-movement option.

## Changes

- Added `ai.movement.neural-enabled`, defaulting to `false`.
- Movement V2 route planning remains active when neural movement is disabled.
- Ordinary movement uses the deterministic baseline when neural movement is disabled.
- Neural residual output can no longer cancel a baseline sprint decision.

## Validation

- Added regression coverage for the neural feature gate and sprint floor.
- Full Gradle build and automated tests pass.

Live duel behavior still needs a runtime Paper server test.
