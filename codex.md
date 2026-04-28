# Codex Implementation Log

Append-only engineering log for TerminatorPlus hardening sprint (mc-26.1.2).

## 2026-04-23 - Command Permission + Threading Foundations
- Bug found: `@Require` metadata existed but was never enforced in command execution (`CommandInstance.execute` only checked `terminatorplus.manage`).
- Change made: enforced method-level permission gate in `CommandInstance.execute` using `CommandMethod.permission`.
- Validation: compile validation pending full build.

- Bug found: admin-only commands were documented but not gated in runtime.
- Change made: added `@Require("terminatorplus.admin")` on `/bot reset`, `/bot debug`, `/bot combatdebug`; added explicit admin check in `/bot preset delete` branch.
- Validation: runtime permission validation pending manual server checks.

- Bug found: `/bot settings mobtarget` and `addplayerlist` required lowercase literal booleans.
- Change made: added case-insensitive boolean parser helper and switched both paths to strict parse (`true|false`, case-insensitive).
- Validation: compile validation pending full build.

- Bug found: `/bot info` and `/ai info` read Bukkit entities from async scheduler tasks.
- Change made: moved both command info flows to main-thread synchronous execution.
- Validation: runtime thread-safety verification pending manual command loop test.

- Bug found: `/bot create` and `/bot multi` blocked server tick waiting on Mojang skin HTTP.
- Change made: added `MojangAPI.getSkinAsync(...)` with in-flight dedupe and background executor; added `BotManagerImpl.createBotsAsync(...)`; routed `/bot create` and `/bot multi` through async path while preserving sync APIs.
- Validation: full behavior validation pending build + in-server spawn tests.

## 2026-04-23 - Full Combat + Movement Debug Overhaul
- User request: expand `/bot combatdebug` from branch-only combat logs into full trace logging, including movement timing and punch cadence, and write logs to plugin debug files instead of `latest.log`.
- Change made: rewired `CombatDebugger` sink from Bukkit logger to async file writer under `plugins/TerminatorPlus/debug/` (combined `combat-all.log` + per-bot `combat-<bot>-<uuid>.log`). Added resilient fallback to console only if file write fails.
- Validation: code compiled and build succeeded (`.\gradlew.bat build -q`) after this change set.

- Change made: added punch-cadence tracing in `CombatDebugger.punch(...)` with ticks-since-last-punch, held item, attack-charge sample, and inferred caller source.
- Validation: compile validation passed in successful build.

- Change made: upgraded swing gate instrumentation in `BotCombatTiming` to emit full allow/block decisions (`charge`, threshold, iframes, reason), not just block events.
- Validation: compile validation passed in successful build.

- Change made: expanded per-tick movement trace in `Bot` (`move-tick`, `move-ground`, `move-step`, `move-jump`, `move-walk`, `move-face`, `move-attack-charge`) while keeping output gated behind combat debug enablement.
- Validation: compile validation passed in successful build.

- Change made: expanded behavior/opportunity trace coverage in combat pipeline (`CombatDirector`, `OpportunityScanner`, `TridentBehavior`, `WindChargeBehavior`, `EnderPearlBehavior`, `UtilityBehavior`, `ConsumableBehavior`, `CrystalBehavior`, `AnchorBombBehavior`) to record attempts, skips, scanner outcomes, and action execution context.
- Validation: compile validation passed in successful build.

- Bug found during validation: `BotCommand.java` and `BotInventoryGUI.java` contained UTF-8 BOM markers (`\ufeff`) causing javac "illegal character" failures at line 1.
- Change made: rewrote both files as UTF-8 without BOM.
- Validation: rebuild succeeded immediately afterward.

- Change made: `/bot combatdebug` now advertises full trace semantics, accepts alias typo `/bot comatdebug`, and reports debug output directory path in command feedback.
- Validation: compile validation passed in successful build.

## 2026-04-23 - Build/Artifact Validation
- Validation run: `.\gradlew.bat build -q` from repo path containing `&` succeeded.
- Artifact observed: `build/libs/TerminatorPlus-5.0.3-BETA-mc26.1.2.jar` (fresh timestamp).

## 2026-04-23 - Release Version Bump to 5.0.5 (mc-26.1.2)
- User request: build release as `5.0.5`.
- Change made: bumped project version in `buildSrc/src/main/kotlin/net.nuggetmc.java-conventions.gradle.kts` from `5.0.3-BETA-mc26.1.2` to `5.0.5-BETA-mc26.1.2`.
- Validation: build succeeded via `.\gradlew.bat build -q`.
- Artifact observed: `build/libs/TerminatorPlus-5.0.5-BETA-mc26.1.2.jar` (fresh timestamp).

## 2026-04-23 - GitHub Tag
- Change made: created annotated Git tag `v5.0.5-mc26.1.2` and pushed to `origin`.
- Validation: `git push origin v5.0.5-mc26.1.2` succeeded.
- Note: tag references current committed `HEAD` on branch `mc-26.1.2`.

## 2026-04-23 - GitHub Release Publication
- Validation issue found: tag `v5.0.5-mc26.1.2` existed but no GitHub Release object was present (`gh release view` returned "release not found").
- Change made: created GitHub prerelease `v5.0.5-mc26.1.2` and uploaded asset `TerminatorPlus-5.0.5-BETA-mc26.1.2.jar`.
- Validation: release URL is live and asset is uploaded.
