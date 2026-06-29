# 14. Active vs Legacy vs Unused Matrix

This matrix classifies major systems using the following buckets:

- Active/current 1v1 path
- Active but legacy/protected
- Optional/debug/admin
- Training-only
- Technical reference/support
- Archive candidate from default strategy
- Possibly unused/unreachable
- Needs deeper proof
- Do not touch without runtime tests

## Matrix

| Area/File/Class | Bucket | Evidence | What it does | Archive/delete recommendation |
|---|---|---|---|---|
| `TerminatorPlus.java` | Active/current 1v1 path | Plugin bootstrap and shutdown entrypoint | Creates managers, commands, bridge, cleanup | Keep and document |
| `BotManagerImpl.java` | Active/current 1v1 path | Live manager used by commands and plugin lifecycle | Owns bot collection, spawn flow, join rerender, manager settings | Keep and protect |
| `Bot.java` | Do not touch without runtime tests | Central fake-player entity implementation | Spawn, tick, combat hooks, render, remove, equipment | Do not delete yet |
| `MockConnection.java` | Do not touch without runtime tests | Required in bot construction path | Fake network connection for NMS player stack | Do not delete yet |
| `MockChannel.java` | Do not touch without runtime tests | Used by mock connection stack | Fake channel scaffolding | Do not delete yet |
| `NMSUtils.java` | Do not touch without runtime tests | Used for entity-data fallback access | Reflection/version compatibility helper | Do not delete yet |
| `LegacyAgent.java` | Active but legacy/protected | Constructed by `BotManagerImpl`; owns live outer tick loop | Targeting, survival, movement mode choice, fallback melee | Do not delete yet |
| `LegacyBlockCheck.java` | Active but legacy/protected | Called from `LegacyAgent` survival flow | Pre-MLG and clutch logic | Keep protected |
| `LegacyMats.java` | Technical reference/support | Shared from both legacy and non-legacy code | Block/material taxonomy and solid overrides | Keep; later wrap if needed |
| `CombatDirector.java` | Active/current 1v1 path | Central combat owner used by bot combat hooks | Plan/execute/timing integration | Keep and protect |
| `CombatIntent.java` | Active/current 1v1 path | Live movement-controller combat contract | Carries movement-relevant combat plan | Keep |
| `MovementState.java` | Active/current 1v1 path | Read after movement apply by combat flow | Observed post-movement state | Keep |
| `CombatSnapshot.java` | Active/current 1v1 path | Used in combat planning/execution | Combat context aggregation | Keep |
| `BotCombatTiming.java` | Do not touch without runtime tests | Central timing gates used by melee/mace/trident | Charge, crit, i-frame, sprint timing | Protect |
| `MeleeBehavior.java` | Active/current 1v1 path | Standard melee branch in combat pipeline | Core melee logic | Keep |
| `MaceBehavior.java` | Active/current 1v1 path | Live combat branch with committed phases | Mace timing/launch/impact behavior | Keep; runtime-test sensitive |
| `TridentBehavior.java` | Active/current 1v1 path | Live combat branch | Trident charge/release behavior | Keep; runtime-test sensitive |
| `OpportunityScanner.java` | Active/current 1v1 path | Used in planning and execution | Advanced tactical opportunity selection | Keep, but likely wrap later |
| Advanced combat behavior classes | Active/current 1v1 path | Instantiated in `CombatDirector` | Crystals, anchors, pearls, wind, consumables, elytra, utility | Keep; later split by docs/strategy |
| `MovementInput.java` | Active/current 1v1 path | Used by movement-controller runtime | Builds stable network input vector | Keep |
| `MovementOutput.java` | Active/current 1v1 path | Used after network evaluation | Interprets raw output semantics | Keep |
| `MovementOutputApplier.java` | Active/current 1v1 path | Live apply layer for controller mode | Applies locomotion and writes `MovementState` | Keep and protect |
| `MovementBrainRouter.java` | Active/current 1v1 path | Chooses movement brain family | Routing/fallback of movement brains | Keep |
| `MovementBrainBank.java` | Active/current 1v1 path | Used by runtime and persistence | Stores fallback + specialist brains | Keep |
| `MovementBrainPersistence.java` | Active/current 1v1 path | Save/load/reset movement brain bank | Persistence and schema/quarantine logic | Keep |
| `MovementNetwork.java` | Active/current 1v1 path | Runtime evaluation of movement brain | Evaluates movement-controller network | Keep |
| `MovementNetworkShape.java` | Active/current 1v1 path | Defines model shape assumptions | Input/output schema shape | Keep |
| Full-replacement NN mode | Active but legacy/protected | Command and agent mode branches still preserve it | Older all-in movement/control model | Do not delete yet |
| `BotInventory.java` | Do not touch without runtime tests | NMS direct-write path for fake-player inventory | Inventory state, equip logic, hotbar state | Protect |
| `PresetManager.java` | Active/current 1v1 path | `/bot preset` persistence and apply | Saves and applies presets | Keep |
| `BotInventoryGUI.java` | Active/current 1v1 path | Opened by command and synced through listener | Inventory editing GUI | Keep |
| `BotInventoryListener.java` | Active/current 1v1 path | Syncs GUI edits back to bots | GUI close/apply propagation | Keep |
| `BotCommand.java` | Active/current 1v1 path + archive candidate from default strategy | Registered command root with wide surface | Main player/admin bot command set | Reclassify docs first |
| `AICommand.java` | Training-only | Exposes movement brain, reinforcement, evaluation flows | AI/training/admin surface | Keep; document as training |
| `BotEnvironmentCommand.java` | Active but legacy/protected | Registered and mutates `LegacyMats`/custom mob list | Environment override/admin tooling | Keep but archive from default user path |
| `MainCommand.java` | Optional/debug/admin | Provides plugin info and debug upload path | Shell + debug info upload | Keep |
| `MovementEvaluationHarness.java` | Training-only | Used for evaluation/export flows | Evaluation/report tooling | Keep as tooling |
| `BotAgent` | Possibly unused/unreachable | `BotManagerImpl` constructs `LegacyAgent` instead | Alternate agent concept | Verify before any cleanup |
| Old loadout/loadoutmix surfaces | Archive candidate from default strategy | Active commands but broad sandbox flavor | Equipment variety/training surface | Archive in docs first, keep code |
| Old wiki docs | Archive candidate from default strategy | Broader than current duel-focused direction | User-facing legacy reference | Reclassify/archive first |

## How to read this matrix

Important interpretation rules:

- "legacy" does not mean safe to remove
- "current" does not mean low-risk
- "archive candidate" here usually means docs/strategy archive first, not code
  deletion
- "possibly unused" means the reference evidence was incomplete or indirect

## Most important high-level conclusion

The plugin's runtime is still built around protected legacy scaffolding plus
newer duel-aligned subsystems.

So the safest next action is still:

- improve docs clarity
- narrow default strategy presentation
- preserve runtime systems until tests and deeper mapping justify change
