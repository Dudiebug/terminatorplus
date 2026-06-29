# Implementation Prompts

## 1. Read-only audit prompt

```text
You are auditing Dudiebug/terminatorplus for the new 1v1 PvP strategy.

Read CODEX.md, VISION.md, WORKFLOW.md, REVIEW_CHECKLIST.md, README.md, wiki/, and the relevant source files.

Do not modify files.
Do not commit.
Do not open PRs.
Do not run destructive commands.
If you build, only run ./gradlew build -q.

Output:
- files inspected
- stale docs found
- high-risk files
- safe cleanup candidates
- behavior assumptions requiring runtime test
- recommended next small slice
```

## 2. DuelTuning prompt

```text
Create a narrow centralized tuning constants file for duel behavior.

Problem:
Combat tuning values are scattered.

Expected behavior:
No behavior should intentionally change in this PR unless required to compile.

Allowed files:
- combat tuning/constants files
- narrow imports where constants are used
- docs explaining constants

Forbidden files:
- NMS internals
- build/release files
- neural-network persistence
- broad LegacyAgent changes

Build:
./gradlew build -q

Required output:
- files changed
- constants moved
- behavior changed or confirmed unchanged
- build result
- runtime test plan
```

## 3. 1v1 movement/spacing prompt

```text
Improve 1v1 melee spacing.

Expected behavior:
The bot approaches into threat range, strafes, avoids face-hugging, and backs up when too close.

Movement may consume CombatIntent and report MovementState.
Movement must not attack, use items, select hotbar slots, or call combat behavior internals.

Allowed files:
- movement-controller files
- spacing constants
- debug metrics
- docs/tests

Forbidden files:
- inventory/NMS internals
- neural-network persistence
- broad LegacyAgent rewrite
- release/build flow

Build:
./gradlew build -q
```

## 4. Sword/axe/shield tuning prompt

```text
Improve basic sword/axe/shield fundamentals before advanced tools.

Expected behavior:
The bot waits for useful attack charge, pressures shield with axe when appropriate, does not freeze waiting for crit, and does not spam weak hits.

Allowed files:
- CombatDirector
- MeleeBehavior
- BotCombatTiming
- narrow inventory selection helpers only if required
- debug docs/tests

Forbidden:
- movement authority violations
- trident/mace/crystal compensation
- broad rewrites

Build:
./gradlew build -q
```

## 5. Defensive recovery prompt

```text
Improve low-HP recovery.

Expected behavior:
At low HP the bot creates space, heals when legal, uses pearl defensively only when appropriate, and re-enters combat after recovery.

Allowed:
- recovery behavior
- healing thresholds
- pearl defensive logic
- debug metrics
- duel test docs

Forbidden:
- endless fleeing
- inventory serialization rewrites
- NMS changes unless runtime-specific

Build:
./gradlew build -q
```

## 6. Punish eating/bowing/shielding prompt

```text
Add or tune punish logic for human vulnerability windows.

Expected behavior:
The bot pressures a player who eats, bows too close, shields predictably, or runs at low HP.

Allowed:
- CombatSnapshot
- CombatDirector
- narrow behavior helpers
- debug labels
- duel tests

Forbidden:
- advanced tool spam
- movement code directly attacking
- broad LegacyAgent rewrite

Build:
./gradlew build -q
```

## 7. Trident tuning prompt

```text
Tune trident behavior for tactical use.

Expected behavior:
The bot uses trident at appropriate range with line of sight and falls back to melee when range is bad.

Allowed:
- trident behavior
- CombatDirector trident branch
- tuning constants
- debug metrics

Forbidden:
- changing unrelated weapon behavior
- inventory rewrite
- movement authority violation

Build:
./gradlew build -q
```

## 8. Mace tuning prompt

```text
Tune mace behavior for tactical conversion, not spectacle.

Expected behavior:
The bot commits to mace only when setup is plausible, tracks target while airborne, avoids excessive self-damage, and returns to melee after failed attempts.

Allowed:
- mace behavior
- CombatDirector mace branch
- BotCombatTiming mace helpers
- debug metrics

Forbidden:
- using mace to hide bad melee
- broad movement rewrite
- neural-network persistence changes

Build:
./gradlew build -q
```

## 9. Debug metrics prompt

```text
Add focused duel debug metrics.

Expected behavior:
Debug output should help decide if the bot is improving in 1v1 PvP.

Metrics:
- desired range
- actual distance
- attack charge
- movement state
- branch family
- planned action
- target HP delta
- bot HP
- punish window detected
- special success/failure

Do not create noisy always-on logs.
Build:
./gradlew build -q
```

## 10. Final review prompt

```text
Review the proposed change against the new strategy.

Reject if:
- it changes unrelated files
- it rewrites LegacyAgent broadly without reason
- it touches NMS/Paper internals without runtime-specific reason
- it makes mace/crystal logic compensate for bad movement
- it breaks neural-network deterministic behavior
- it cannot explain how to test behavior
- it uses forbidden build commands

Output:
- accept/reject
- reasons
- risks
- required runtime tests
- follow-up items
```
