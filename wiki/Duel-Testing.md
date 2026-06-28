# Duel Testing

Use duel tests to decide whether the bot is improving in 1v1 PvP, not merely
whether it compiles or shows more features.

## Baseline Setup

- Paper-compatible server for `mc-26.1.2`
- plugin jar built with `./gradlew build -q`
- controlled flat duel arena
- one human tester
- one bot unless a test explicitly says otherwise
- combat debug enabled when measuring behavior

## Suggested Command Pattern

Adjust exact syntax after verifying runtime behavior:

```text
/bot create DuelBot
/bot loadout sword DuelBot
/bot combatdebug DuelBot on
/ai movement 1 DuelBot
```

## Manual Checks

- bot spawns
- bot targets the player
- bot holds melee range
- bot strafes
- bot backs up when too close
- bot uses axe against shield
- bot waits for attack charge
- bot does not freeze waiting for crit
- bot heals or resets at low HP
- bot chases a low-HP player
- bot punishes eating and bowing
- bot uses advanced tools only when appropriate

## Metrics

Record time alive, damage dealt, damage taken, hits landed, missed swings,
shield breaks, successful retreats, successful heals, stuck events, special move
attempts and failures, time in desired range, freeze events, and movement-family
fallback rate when the movement controller is active.
