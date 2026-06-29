# 16. Present but Maybe Not Doing Anything

This section is intentionally conservative.

The goal is not to label things "dead" aggressively. The goal is to identify
items that appear inactive, underused, partially stale, or mismatched with the
current source-guided understanding.

## Conservative interpretation rules

Terms used here:

- `possibly unused` means reference evidence was weak or no clear live call
  path was found
- `appears inactive` means source suggests a code path is not part of the
  ordinary runtime
- `safe action` means docs note, reference search, or proof task, not deletion

## Findings

| Item | Evidence | Why it may be inactive | What would prove it | Safe action |
|---|---|---|---|---|
| `BotAgent` | `BotManagerImpl` constructs `LegacyAgent`, not `BotAgent` | No live construction path was observed in the main runtime | Full repo reference search and runtime instantiation audit | Mark as possibly unused; do not delete yet |
| `AICommand.ensureMovementBrain(...)` | No meaningful active call path was found during source review | Looks like convenience/helper logic that current flows bypass | Exhaustive reference search and command-path tracing | Leave in place and tag for later cleanup audit |
| `MovementBrainPersistence` convenience wrappers (`save/load/reset`) | Primary callers appear to prefer explicit bank-oriented methods | Older or convenience API surface may no longer be needed | Full reference search across repo | Leave in place |
| `CombatDirector.plan(..., previousIntent)` parameter | The `previousIntent` parameter is present but not read | API shape may be leftover from an earlier iteration | Read all overloads/callers and compare commit history if needed | Low-priority cleanup note only |
| `CombatIntent.lockTicksRemaining(...)` | No active call sites were found in the audit | Accessor/field may be stale or reserved for future behavior | Full reference search and runtime tracing of lock state | Leave in place |
| `MovementState.currentFacing()` | No active call sites were found | Looks like a convenience accessor with no current consumer | Full reference search | Leave in place |
| `WindChargeBehavior.ticksFor(...)` | No live call site was found; active flow uses movement-boost handling | Older offensive helper path may have been superseded | Full reference search plus targeted runtime exercise if revived | Mark as possibly unused |
| `ComboBehavior` broader enum surface | Only one combo variant clearly appears active | Enum/API shape is wider than current effective usage | Search all combo type references and force combo scenarios in runtime | Leave in place |
| `/bot info` partial branch | Source contains "coming soon"/partial UX shape | Command exists but does not look fully finished | Run the command on a test server | Reclassify docs, not code |
| `plugin.yml` or command-text implication of reload/admin breadth | Current registered commands do not match every broad expectation old docs imply | Docs/metadata drift rather than live feature proof | Compare source command registration against wiki/help pages | Fix docs later |
| Old wiki claims around broad behavior | Source and current docs direction do not fully match some old wiki pages | Documentation drift from prior strategy | Page-by-page wiki/source audit | Archive/relabel docs first |
| Mixed-mode training strings about fallback update behavior | Some user-facing text appears behind current persistence behavior | String/docs drift rather than necessarily dead code | Run training flow and inspect saved bank outputs | Correct docs and messages later |

## What not to do with this list

Do not:

- mass-delete these items
- assume they are harmless just because they appear unused
- assume they are safe to remove without runtime/build/reference proof

In this repo, a surprising amount of behavior is mode-gated, command-gated, or
admin-gated rather than obviously dead.

## Best next use of this list

The right follow-up for this list is:

1. docs reclassification
2. explicit reference search tickets
3. runtime proof tasks for suspicious command or behavior surfaces
4. only then possible cleanup work

This keeps the repo honest without turning uncertainty into accidental
deletion.
