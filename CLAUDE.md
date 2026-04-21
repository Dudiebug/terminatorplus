# TerminatorPlus — Build & Release Playbook

**Read this before you touch the build.** Every trap below has burned at
least one previous session. Follow the happy path and you won't fight
Gradle.

---

## The happy path

```bash
# From repo root, on whichever branch you're working on (mc-26.1.2 / mc-1.21.11 / master).
./gradlew build -q
ls build/libs/
# → TerminatorPlus-5.0.0-BETA-mc1.21.11.jar
# → TerminatorPlus-5.0.0-BETA-mc26.1.2.jar
```

`./gradlew build` is the **only** build command you need. It produces the
final, ready-to-drop-in-plugins jars in the root-level `build/libs/`
directory. Don't run anything else until this works.

---

## Build errors you *will* hit if you go off-script

Each of these cost a previous Claude real time. They are **not** Java
compile errors — every Java change so far has compiled cleanly on first
try. They are Gradle-pipeline errors. Match the error text exactly.

### 1. `Task 'shadowJar' not found in root project 'TerminatorPlus'`

You ran `./gradlew shadowJar` or `./gradlew :TerminatorPlus-Plugin:shadowJar`.
**This project doesn't use the Shadow plugin.** The `build` task already
produces the fat-ish plugin jars at the root `build/libs/` — via a custom
task defined in `buildSrc/`, not via ShadowJar.

→ **Fix:** use `./gradlew build`. Never `shadowJar`.

### 2. `Failed to calculate the value of task ':TerminatorPlus-Plugin:reobfJar' property 'mappingsFile'. The current dev bundle does not provide reobf mappings.`

You ran `./gradlew reobfJar`. The paperweight dev bundle in use
(`1.21.11-R0.1-SNAPSHOT` / `26.1.2-R0.1-SNAPSHOT`) does **not** ship
Mojang→Spigot reobf mappings because Paper stopped shipping them —
modern Paper runs on Mojang-mapped internals at runtime, so the plugin
does not need reobfuscation.

→ **Fix:** do **not** call `reobfJar`. `./gradlew build` produces a
Mojang-mapped jar that Paper 1.21.x+ loads directly.

### 3. `Cannot snapshot C:\...\buildSrc\build\...\class-attributes.tab: not a regular file`

### 3b. `Cannot snapshot C:\...\buildSrc\build\classes\kotlin\main\gradle\kotlin\dsl\accessors\_0cb39c16b209519d61ee18b0fceac003\Accessors<hash>Kt.class: not a regular file`

Both are the same underlying bug: the **Windows + buildSrc + Kotlin DSL
accessors** combo leaves corrupted sentinel files in `buildSrc/build/`
that Gradle 9 can't snapshot. It reliably happens after a branch switch,
a `gradlew clean`, or a crashed build.

→ **Fix (don't overthink it):**

```bash
rm -rf buildSrc/build
./gradlew build -q
```

That's it. Don't run `./gradlew clean`. Don't try to delete specific
subdirs. Just wipe `buildSrc/build` entirely. `buildSrc` recompiles in
~5 seconds so the cost is trivial.

### 4. "BUILD FAILED in 1s" with **no What-went-wrong block**

That's a ghost from a prior failure that Gradle cached. Same fix: nuke
`buildSrc/build` and rerun. If the real build genuinely failed, the
error text is under `* What went wrong:` — if you don't see that block,
it's a stale failure cache.

---

## Release flow (once the jars are built)

GitHub releases already exist as tags — do **not** create new ones:

- `v5.0.0-mc26.1.2`
- `v5.0.0-mc1.21.11`

Upload flow per branch:

```bash
# mc-26.1.2
git checkout mc-26.1.2
./gradlew build -q
gh release upload v5.0.0-mc26.1.2 \
  "build/libs/TerminatorPlus-5.0.0-BETA-mc26.1.2.jar" --clobber

# mc-1.21.11
git checkout mc-1.21.11
./gradlew build -q
gh release upload v5.0.0-mc1.21.11 \
  "build/libs/TerminatorPlus-5.0.0-BETA-mc1.21.11.jar" --clobber
```

`--clobber` overwrites the existing asset. Always include the full path
(with quotes — the repo lives under a path with spaces).

---

## Branch topology — **read this before cherry-picking**

There are three long-lived branches, and changes flow **down** a chain:

```
mc-1.21.11  ←  primary dev branch, commits usually land here first
   │
   │  cherry-pick
   ▼
mc-26.1.2   ←  same code, compiled against Paper 26.1.2 dev bundle
   │
   │  cherry-pick
   ▼
master      ←  display branch for GitHub; mirrors the latest fixes
```

### The `cherry-pick master` trap

If you cherry-pick from `master` while on a feature branch, **master is
almost always behind** — the cherry-pick will silently produce an empty
commit and you'll think the fix landed when it didn't. Previous
sessions burned an hour on this exact thing.

→ **Always cherry-pick the concrete SHA** from the branch where the
commit was authored (usually `mc-1.21.11`):

```bash
# Bad:  git cherry-pick master
# Good:
SHA=$(git log mc-1.21.11 --format=%H -n1)
git checkout mc-26.1.2  && git cherry-pick $SHA
git checkout master     && git cherry-pick $SHA
```

Push all three when done. `gh release upload --clobber` only needs the
two tagged branches (`mc-26.1.2`, `mc-1.21.11`); `master` is for
display.

---

## Paper-version specifics that bite

### Paper 26.x (`mc-26.1.2` branch)

These are runtime traps, not compile traps, but they cost real time:

- **`SynchedEntityData.packAll()` reflection.** Paper 26.x removed the
  old `Int2ObjectMap<DataItem<?>>` field. `NMSUtils` now tries three
  strategies in order: `packAll()` method → `Int2ObjectMap` field →
  array field. Keep that fallback chain intact; don't simplify it or
  you'll break the older branch.
- **Entity data slot index for `DATA_PLAYER_MODE_CUSTOMISATION`.**
  Between 1.21.1 and 26.x, Mojang inserted new `LivingEntity` data
  slots, shifting the skin-customisation BYTE's index. **Never
  hardcode the index.** Use
  `net.minecraft.world.entity.player.Player.DATA_PLAYER_MODE_CUSTOMISATION`
  (Bot.java:173) — that constant always resolves to the correct index
  for whatever Paper build the dev bundle points at.
- **Container-transaction rollback for bots.** `PlayerInventory.setItem`
  goes through the Bukkit container-transaction system, which Paper
  26.x rolls back on the next tick when the `MockConnection` never ACKs
  the slot packet. For bot inventory writes, **bypass Bukkit** and
  write straight to NMS:

  ```java
  net.minecraft.world.entity.player.Inventory nmsInv = bot.getInventory();
  nmsInv.setItem(i, CraftItemStack.asNMSCopy(bukkitStack));
  nmsInv.setChanged();
  ```

  Armor and offhand are fine via `bot.setItem(stack, slot)` because
  that path emits `ClientboundSetEquipmentPacket` directly.

### Paper 1.21.11 (`mc-1.21.11` branch)

- Uses the older `Int2ObjectMap` field in `SynchedEntityData` — the
  fallback strategy above handles it automatically, just leave the code
  alone.
- Container rollback bug is NOT present here, but the NMS direct-write
  pattern still works and is used for consistency across branches.

---

## Sanity-check checklist before declaring a fix "done"

1. `./gradlew build -q` succeeds **on both branches** (`mc-26.1.2` and
   `mc-1.21.11`).
2. `ls -la build/libs/*.jar` shows fresh timestamps for *both* jars. If
   one is stale, you forgot to rebuild after checking out its branch.
3. Both branches are pushed: `git push origin mc-26.1.2 mc-1.21.11 master`.
4. Both releases have the new jar: `gh release view v5.0.0-mc26.1.2
   --json assets -q '.assets[].name'` and likewise for `v5.0.0-mc1.21.11`.

If any of those four are missing, the user's test server is running old
code even though you "finished."

---

## Things I spent time on that didn't matter — skip these

- **Don't** try to reproduce the `class-attributes.tab` failure — just
  delete `buildSrc/build` the moment you see it.
- **Don't** chase the "BUILD FAILED in 1s with no error" message
  thinking it's a real Gradle bug. It's the same stale cache.
- **Don't** run `./gradlew clean` as a reflex — it doesn't fix the
  buildSrc corruption and it wastes ~30s of dependency re-resolution.
- **Don't** add `publishing {}` tasks or a GitHub Actions workflow for
  releases unless explicitly asked. The manual `gh release upload
  --clobber` flow is the current standard and the user prefers it.
