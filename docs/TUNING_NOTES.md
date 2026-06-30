# Tuning Notes

## Purpose

This file is the running log for balance and behavior tuning. Do not rely on memory. Every tuning change should have a reason and a test result.

## Entry format

```markdown
## YYYY-MM-DD - short title

Branch:
Files:
Problem:
Expected behavior:
Change:
Runtime test:
Result:
Decision: keep/revert/follow-up
Notes:
```

## Movement tuning

Track:

- ideal melee distance
- minimum distance
- retreat distance
- strafe strength
- facing adjustment
- hold-position behavior
- approach urgency

## Melee timing tuning

Track:

- attack charge threshold
- crit wait behavior
- sprint-hit rules
- missed swing behavior
- shield pressure

## Defensive recovery tuning

Track:

- low HP threshold
- critical HP threshold
- healing HP threshold
- pearl-away threshold
- retreat duration
- re-entry timing

## Punish logic tuning

Track:

- punish eating
- punish bowing
- punish shielding
- punish low HP running
- punish missed swings

## Trident tuning

Track:

- min distance
- max distance
- charge timing
- line-of-sight checks
- throw cooldown
- fallback to melee

## Mace tuning

Track:

- engage range
- launch conditions
- airborne tracking
- cancel conditions
- self-damage risk
- cooldown

## Crystal/anchor tuning

Track:

- safe distance
- placement conditions
- detonation timing
- self-damage
- dimension restrictions
- fallback behavior

## Inventory/loadout tuning

Track:

- hotbar slot assumptions
- offhand behavior
- shield/totem swaps
- gapple slot
- loadout lock state

## Initial hypotheses

These are starting guesses, not final values.

| Constant | Initial hypothesis |
|---|---:|
| ideal melee distance | 2.7-3.2 blocks |
| minimum distance | 1.6-2.0 blocks |
| retreat distance | 1.8 blocks |
| strafe strength | moderate, not constant max |
| low HP threshold | 40% |
| critical HP threshold | 25% |
| healing HP threshold | 35-45% depending range |
| pearl-away threshold | low HP + unsafe melee range |
| chase-low-target threshold | target below 35% |
| shield-break range | melee reach only |
| trident min distance | 5 blocks |
| trident max distance | 28 blocks |
| mace engage range | close range only with setup |

## Change log table

| Date | Branch | File | Value changed | Old value | New value | Reason | Test result | Keep/revert |
|---|---|---|---|---:|---:|---|---|---|
