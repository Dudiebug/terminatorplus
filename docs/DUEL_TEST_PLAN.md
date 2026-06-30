# Duel Test Plan

## Test goals

Determine whether a bot is actually improving in 1v1 PvP, not merely compiling or showing more features.

## Server assumptions

- Paper-compatible server for target branch.
- Plugin jar built with `./gradlew build -q`.
- Controlled flat duel arena.
- One human tester.
- One bot unless a test explicitly says otherwise.
- Combat debug available.

## Arena setup

Recommended:

- flat surface
- no mobs
- no terrain clutter for baseline tests
- clear boundaries
- reset point
- optional obstacles only for later movement tests

## Standard loadouts

### Basic sword/axe/shield kit

- sword
- axe
- shield
- armor
- limited gapples

### Recovery kit

- sword
- shield
- gapples
- pearl
- armor

### Trident kit

- trident
- sword fallback
- armor

### Mace kit

- mace
- sword fallback
- armor

### Advanced kit later

- sword
- axe
- shield/totem
- pearls
- gapples
- cobweb
- crystal/anchor only when explicitly testing advanced tools

## Standard command pattern

Adjust exact names to the current command implementation.

```text
/bot create DuelBot
/bot loadout sword DuelBot
/bot combatdebug DuelBot on
/ai movement 1 DuelBot
```

If command syntax differs, update this file after verifying runtime behavior.

## Manual test cases

1. Bot spawns.
2. Bot targets player.
3. Bot holds melee range.
4. Bot strafes.
5. Bot backs up when too close.
6. Bot uses axe against shield.
7. Bot waits for attack charge.
8. Bot does not freeze waiting for crit.
9. Bot heals/resets at low HP.
10. Bot chases low-HP player.
11. Bot punishes eating.
12. Bot punishes bowing.
13. Bot uses pearl defensively.
14. Bot uses trident only when appropriate.
15. Bot uses mace only when appropriate.

## Metrics

Record:

- time alive
- damage dealt
- damage taken
- hits landed
- missed swings
- shield breaks
- successful retreats
- successful heals
- stuck events
- special move attempts
- special move successes
- special move failures
- time in desired range
- freeze events
- branch-family fallback rate when movement-controller is active

## Pass/fail criteria

A change passes only if it improves the targeted behavior without obvious regression.

Examples:

- spacing change passes if bot spends more time in useful melee range and does not face-hug more.
- timing change passes if weak spam swings decrease and real hits do not disappear.
- recovery change passes if bot survives low-HP states more often without endlessly fleeing.

## Debug checklist

Enable and record relevant debug:

- branch/action selected
- target distance
- desired range
- attack charge
- target HP delta
- bot HP
- movement state
- stuck/fallback state
- special commit state

## Regression checklist

Before accepting a change:

- bot still spawns
- bot still targets
- `/bot reset` works
- loadouts still apply
- movement-controller bot still moves
- legacy mode still works or is not touched
- neural-network training commands still parse
- build still passes
