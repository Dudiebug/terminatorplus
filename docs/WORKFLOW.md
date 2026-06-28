# TerminatorPlus Workflow

## Standard development loop

1. Audit the current behavior.
2. Pick one problem.
3. Write expected behavior.
4. Limit scope.
5. Implement the smallest safe change.
6. Build with `./gradlew build -q`.
7. Runtime test in a duel.
8. Record result.
9. Review changed files.
10. Only then continue.

## Branch rules

- Primary active target: `mc-26.1.2`.
- Do not start new current-strategy work from `master`.
- Treat older branch docs as compatibility references.
- If a file says `mc-1.21.11` is primary, update docs or flag it as stale before using it as process guidance.

## Build rules

Allowed:

```bash
./gradlew build -q
```

Forbidden unless explicitly justified later:

```bash
./gradlew shadowJar
./gradlew reobfJar
./gradlew clean
```

If buildSrc corruption appears, delete only:

```bash
rm -rf buildSrc/build
```

Then retry:

```bash
./gradlew build -q
```

## Commit/PR rules

- One problem per PR.
- No drive-by formatting.
- No broad package moves during combat tuning.
- No release-flow edits during PvP behavior work.
- No unrelated docs churn.
- A PR must explain behavior changed, files changed, build result, test checklist, risks, and follow-up items.

## Codex/subagent rules

Every agent must read:

1. `CLAUDE.md`
2. `VISION.md`
3. `WORKFLOW.md`
4. `REVIEW_CHECKLIST.md`
5. task-specific docs

Agents must not follow old wiki strategy blindly. The wiki is reference unless verified current.

## Scope-control template

```markdown
## Scope

Problem:
Expected behavior:
Allowed files:
Forbidden files:
Do not change:
Build command:
Runtime test:
Acceptance criteria:
Rollback plan:
```

## Required agent output

```markdown
## Files changed

## Behavior changed

## Build result

## Runtime test checklist

## Risks

## Follow-up items
```

## Forbidden behavior

- broad rewrites
- drive-by formatting
- touching release flow during combat work
- editing unrelated systems
- simplifying NMS fallback code
- changing neural-network behavior accidentally
- changing movement-only contract
- modifying advanced mechanics to hide weak fundamentals
- reorganizing packages and changing behavior in the same PR

## Handling failures

If build fails:

1. Capture the exact error.
2. Do not clean the repo.
3. Do not rewrite unrelated files.
4. Fix the smallest direct cause.
5. If buildSrc corruption is suspected, delete only `buildSrc/build`.

If runtime behavior fails:

1. Record the expected behavior.
2. Record what happened.
3. Capture debug output.
4. Revert or narrow the change.
5. Do not add advanced tools as a workaround.

## Acceptance rule

Accept a change only if:

- it matches the vision
- it is scoped
- it builds
- it has a runtime test plan
- it does not break neural-network mode
- it does not weaken Paper/NMS safety
- it improves 1v1 behavior or repo governance
