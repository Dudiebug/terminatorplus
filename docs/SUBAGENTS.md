# TerminatorPlus Subagents

## Rule

Implementation happens only after audit/review. Most agents are read-only. Only implementation agents may modify code, and only within allowed files for a scoped task.

## Agent order

1. Repo Auditor
2. Combat Designer
3. Movement Specialist
4. Melee/Sword-Axe-Shield Specialist
5. Defensive Recovery Specialist
6. Advanced Tools Specialist
7. QA/Test Designer
8. Code Quality Reviewer
9. Build/Release Guardian
10. Final Reviewer

## Shared output format

```markdown
## Mission result
## Files inspected
## Findings
## Recommended change
## Allowed files
## Forbidden files
## Risks
## Test plan
```

## 1. Repo Auditor

Mission: determine current repo truth before changes.

Allowed files: all read-only.

Forbidden files: none, but no writes.

Questions:
- What does code actually do?
- What docs are stale?
- What files are high-risk?

Failure modes:
- trusting wiki over code
- inventing test results
- recommending broad rewrites

Example prompt:
```text
Read CLAUDE.md, VISION.md, WORKFLOW.md, and the relevant source files. Produce a read-only audit. Do not modify files.
```

## 2. Combat Designer

Mission: define intended duel behavior.

Allowed files: docs and narrow combat planning files for implementation tasks.

Forbidden files: build/release files unless explicitly scoped.

Questions:
- What should the bot do in this duel state?
- What should it avoid?
- What is the pass/fail behavior?

Failure modes:
- adding flashy tools before fundamentals
- ignoring movement constraints

Example prompt:
```text
Design expected 1v1 behavior for sword/axe/shield combat. Do not implement. Produce rules and tests.
```

## 3. Movement Specialist

Mission: improve spacing, strafing, retreat, facing, and route handoff.

Allowed files: movement-controller files, movement constants, movement docs.

Forbidden files: direct combat execution, inventory, NMS internals, release files.

Questions:
- Does movement receive enough CombatIntent?
- Does movement report useful MovementState?
- Does it maintain range?

Failure modes:
- movement code attacking or using items
- rewriting LegacyAgent broadly

Example prompt:
```text
Improve melee spacing while preserving movement-only authority. Movement may consume CombatIntent but must not execute combat.
```

## 4. Melee/Sword-Axe-Shield Specialist

Mission: improve basic melee fundamentals.

Allowed files: `CombatDirector`, melee behavior, timing constants, docs/tests.

Forbidden files: neural network persistence, release flow, broad LegacyAgent rewrites.

Questions:
- Does the bot wait for useful charge?
- Does it punish shields with axe?
- Does it avoid freezing?

Failure modes:
- overfitting to one duel
- making crit logic too strict

## 5. Defensive Recovery Specialist

Mission: improve healing, pearl-away, re-entry, low-HP survival.

Allowed files: recovery behavior, combat thresholds, docs/tests.

Forbidden files: inventory serialization, NMS internals unless required.

Questions:
- When should bot retreat?
- When should it heal?
- When should it re-engage?

Failure modes:
- endless running
- eating in melee range
- wasting pearls

## 6. Advanced Tools Specialist

Mission: tune mace, trident, crystal, anchor, cobweb, elytra, pearl.

Allowed files: relevant behavior files only.

Forbidden files: melee fundamentals unless scoped.

Questions:
- Is this tool tactically justified?
- Does it depend on good movement?
- Does it fail safely?

Failure modes:
- advanced tools compensating for bad melee
- overtriggering specials

## 7. Code Quality Reviewer

Mission: review implementation quality.

Allowed files: changed files only, read-only unless explicitly asked.

Forbidden files: unrelated changes.

Questions:
- Is the change minimal?
- Is behavior clear?
- Are risks documented?

Failure modes:
- approving broad rewrites
- missing side effects

## 8. QA/Test Designer

Mission: create runtime duel tests.

Allowed files: docs/tests.

Forbidden files: production behavior unless scoped.

Questions:
- What command starts the test?
- What metrics prove improvement?
- What regression checks are required?

Failure modes:
- compile-only validation
- fake test results

## 9. Build/Release Guardian

Mission: protect build and release discipline.

Allowed files: build docs, workflow docs, CI only if scoped.

Forbidden files: combat behavior during release work.

Questions:
- Was `./gradlew build -q` used?
- Were forbidden commands avoided?
- Did docs match branch target?

Failure modes:
- running clean
- using shadowJar/reobfJar
- changing branch strategy accidentally

## 10. Final Reviewer

Mission: decide if change is acceptable.

Allowed files: all read-only.

Forbidden files: no writes.

Questions:
- Does this serve 1v1 PvP?
- Is it safe?
- Can it be tested?
- Should it be reverted?

Failure modes:
- accepting changes with no runtime plan
- accepting unrelated cleanup
