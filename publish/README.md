# TerminatorPlus — Multi-version ports

Four branches + jars are staged locally. GitHub Credential Manager on this box uses browser-based auth, which can't complete unattended, so the push/release step was deferred for you to run.

## To publish (≈1 minute)

1. Create a Personal Access Token at https://github.com/settings/tokens with `repo` scope.
2. In a bash shell:

   ```bash
   cd C:/Users/dudie/Documents/terminatorplus
   export GITHUB_TOKEN=ghp_...
   bash publish/publish-releases.sh
   ```

The script pushes all four branches and creates four pre-release releases on `Dudiebug/terminatorplus`, each with the matching plugin + API jars attached.

## Branches

| Branch         | Paper target                  | Jar                                              |
|----------------|-------------------------------|--------------------------------------------------|
| `mc-1.21.11`   | `paper-api:1.21.11-R0.1-SNAPSHOT` (Spigot-reobf, Java 21) | `TerminatorPlus-4.5.2-BETA-mc1.21.11.jar`        |
| `mc-26.1`      | `paper-api:26.1.1.build.+` (Mojang-mapped, Java 25)        | `TerminatorPlus-4.5.2-BETA-mc26.1.jar`           |
| `mc-26.1.1`    | `paper-api:26.1.1.build.+` (Mojang-mapped, Java 25)        | `TerminatorPlus-4.5.2-BETA-mc26.1.1.jar`         |
| `mc-26.1.2`    | `paper-api:26.1.2.build.+` (Mojang-mapped, Java 25)        | `TerminatorPlus-4.5.2-BETA-mc26.1.2.jar`         |

`mc-26.1` targets the 26.1.1 dev bundle because Paper never published a base `26.1.build.X` dev bundle — they jumped straight to 26.1.1.

## What changed across the port

- **paperweight-userdev** bumped 1.7.5 → 2.0.0-beta.21 (required for 26.x unobfuscated support).
- **Gradle wrapper** bumped 8.11.1 → 9.0.0 (required by paperweight 2.0).
- **26.x reobf removed.** Paper 26.1+ runs Mojang-mapped; `reobfJar`/`reobfArtifactConfiguration` deleted on the 26.x branches. Top-level `implementation(project(":TerminatorPlus-Plugin", "reobf"))` → `implementation(project(":TerminatorPlus-Plugin"))`.
- **Java 25** on 26.x branches; Java 21 retained on mc-1.21.11.
- **Paper API changes fixed:**
  - `Material.CHAIN` split into `IRON_CHAIN` + `COPPER_CHAIN` (1.21.11 Paper change).
  - `EntityType.BOAT` gone; now per-wood type (using `OAK_BOAT`).
  - `Entity.hurt(DamageSource, float)` is final; override `hurtServer(ServerLevel, DamageSource, float)` instead.
  - `ServerPlayer.server` is private; use `((CraftServer) Bukkit.getServer()).getServer()`.
  - `detectEquipmentUpdatesPublic()` folded back into public `detectEquipmentUpdates()`.
  - `Connection.send(Packet, PacketSendListener, ...)` → `ChannelFutureListener`.
  - `ChunkPos` became a record; `.x` / `.z` → `.x()` / `.z()`.
  - `org.apache.commons.lang.StringUtils` no longer shipped; replaced with `String.join`.
  - `com.googlecode.json-simple:json-simple` dropped from transitive paper-api deps on 1.21.11+; declared explicitly.
- **MockConnection** reflection rewritten to resolve `packetListener` / `disconnectListener` by field type + declaration order, so it works on both Spigot-reobf'd (1.21.x) and Mojang-mapped (26.x) runtimes without hardcoded obf letters.

## Not tested

These are compile-only verified. No Paper server was run. If bot spawn / combat / tracking misbehaves, the likeliest suspects are:
- `this.entityData.set(new EntityDataAccessor<>(17, EntityDataSerializers.BYTE), (byte) 0x7F)` — hardcoded data-watcher index 17 for the skin-parts bitmask. If Mojang shifted that slot, skins render as default. Can be fixed by resolving the accessor via `Player.DATA_PLAYER_MODE_CUSTOMISATION` (1.21.x) once confirmed.
- `LevelChunk.loaded` direct field access (Bot.java `loadChunks`) — if the field was removed, chunks won't be forced loaded. Easy to swap to `world.getChunk(i, j, ChunkStatus.FULL)` or similar.
