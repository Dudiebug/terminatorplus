# Next Codex Tasks

## Task 1 - Migrate `CLAUDE.md` to `CODEX.md`

### Goal

Make `CODEX.md` the canonical AI-agent playbook and preserve `CLAUDE.md` as a
compatibility redirect.

### Allowed files

- `CODEX.md`
- `CLAUDE.md`
- `README.md`
- `docs/WORKFLOW.md`
- `docs/SUBAGENTS.md`
- `docs/IMPLEMENTATION_PROMPTS.md`
- `docs/DEPRECATION_PLAN.md`
- `docs/DUEL_CORE_V2.md`
- `docs/CODEBASE_ARCHIVE_CANDIDATES.md`
- `docs/NEXT_CODEX_TASKS.md`
- optionally `wiki/README.md`
- optionally `wiki/Legacy-Status.md`

### Forbidden files

- Java source files
- Gradle files
- plugin resources
- buildSrc
- generated files

### Do not change

- runtime behavior
- commands
- loadouts
- movement schemas
- NMS/MockConnection code
- package names

### Build command

No build required for docs-only work.

If a build is run, only use:

```bash
./gradlew build -q
```

### Test plan

- Confirm `CODEX.md` exists.
- Confirm `CLAUDE.md` is only a compatibility redirect.
- Confirm current strategy target is `mc-26.1.2`.
- Confirm old `mc-1.21.11` branch-flow guidance is removed or marked
  compatibility-only.
- Confirm no source/build files changed.
- Confirm all unverified gameplay claims are marked `needs runtime test`.

### Required output

- Files changed.
- Summary of docs changed.
- Exact branch-governance correction.
- Checks run.
- Follow-up tasks.

### Acceptance criteria

- `CODEX.md` is canonical.
- `CLAUDE.md` points to `CODEX.md`.
- No Java/build/runtime files changed.
- Current work is clearly scoped to `mc-26.1.2`.

### Rejection criteria

- Any source behavior change.
- Any Gradle change.
- Any deletion recommendation for protected runtime systems without tests.
- Any instruction to use `master` as current source of truth.
- Any instruction to use `mc-1.21.11` as primary for current strategy work.

---

## Task 2 - Add compile-only Duel Core V2 skeleton

### Goal

Add non-wired V2 skeleton classes under `dev.dudiebug.terminatorplus.duel`.

### Allowed files

- `TerminatorPlus-Plugin/src/main/java/dev/dudiebug/terminatorplus/duel/**`

### Forbidden files

- `net/nuggetmc/tplus/**`
- `plugin.yml`
- Gradle files
- docs, unless only adding Javadocs in new files
- commands
- NMS/MockConnection
- `Bot`
- `BotInventory`
- `LegacyAgent`
- `CombatDirector`

### Do not change

- runtime behavior
- command behavior
- loadouts
- movement schema
- package names outside new V2 package

### Build command

```bash
./gradlew build -q
```

### Test plan

- Compile only.
- Confirm no runtime hook exists.
- Confirm new classes do not import NMS.
- Confirm movement intent contains no executable combat action.

### Required output

- New class list.
- Build result.
- Confirmation that no runtime behavior changed.

### Acceptance criteria

- Build passes.
- New package compiles.
- No old source modified.
- No runtime registration or command hook added.
- `BotActionExecutor` is an interface or inert abstraction only.

### Rejection criteria

- Any behavior change.
- Any edit to `CombatDirector`, `LegacyAgent`, `Bot`, `BotInventory`,
  MockConnection, movement brain code, or commands.
- Any mass rename.
- Any direct NMS dependency from V2 policy classes.

---

## Task 3 - Read-only `CombatDirector`/`LegacyAgent` replacement map

### Goal

Create a mapping document showing what Duel Core V2 will eventually own, wrap,
or avoid copying from `CombatDirector` and `LegacyAgent`.

### Allowed files

- `docs/DUEL_CORE_V2_REPLACEMENT_MAP.md`
- optionally update `docs/DUEL_CORE_V2.md` with a link

### Forbidden files

- Java source files
- Gradle files
- plugin resources
- commands
- wiki pages unless explicitly scoped

### Do not change

- runtime behavior
- loadouts
- movement schema
- NMS/MockConnection
- `CombatDirector`
- `LegacyAgent`

### Build command

No build required for docs-only work.

If a build is run, only use:

```bash
./gradlew build -q
```

### Test plan

- Verify every mapped source claim against current source.
- Mark behavior assumptions `needs runtime test`.
- Confirm no source files changed.

### Required output

- Ownership map.
- Wrap/replace/avoid-copy table.
- Migration sequence.
- Runtime test requirements.

### Acceptance criteria

- Clear map from old classes to V2 classes.
- No behavior changes.
- Protected systems remain protected.
- Advanced systems are not recommended for deletion.

### Rejection criteria

- Any code edit.
- Any broad rewrite recommendation as next step.
- Any deletion recommendation without runtime tests.
