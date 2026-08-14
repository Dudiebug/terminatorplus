# TerminatorPlus 6.1.1 - Paper 26.2 Compatibility

TerminatorPlus 6.1.1 is a focused compatibility release for Paper 26.2.
It does not rewrite combat, movement, training, or persisted brain formats.

## Highlights

- Updated the active target branch to `mc-26.2`.
- Updated the build version to `6.1.1-BETA-mc26.2`.
- Updated the plugin runtime version gate to require Minecraft/Paper `26.2`.
- Updated Paper API dependency from `paper-api:26.1.2.build.+` to
  `paper-api:26.2.build.+`.
- Updated Paperweight Userdev dev bundle from `26.1.2.build.+` to
  `26.2.build.+`.
- Updated current docs, wiki install notes, release helper metadata, and CI
  comments to describe `mc-26.2` as the active target.

## Compatibility Fixes

- No Java/NMS signature changes were required by the compiler for this update.
- The existing Paper 26.x NMS safety paths were preserved, including
  `SynchedEntityData` fallback extraction, Mojang constant-based player skin
  customization data, and direct NMS inventory writes for bot main inventory
  slots.
- `BotInventory` wording now refers to the broader Paper 26.x inventory rollback
  behavior instead of a single 26.1.2 point release.

## Validation

- Build command: `./gradlew build -q`
- Build result: passed on July 3, 2026
- Paper API/dev bundle resolved from Paper's 26.2 build stream.
- Artifact: `build/libs/TerminatorPlus-6.1.1-BETA-mc26.2.jar`
- Runtime duel validation: needs runtime test

## Known Runtime Risks

- Paper 26.2 runtime smoke testing has not been run in a live server in this
  session.
- Fake-player spawn, packet listener assumptions, inventory synchronization,
  entity data packing, movement, and combat timing remain the highest-risk
  runtime areas after any Paper target bump.
- Paper's current 26.2 Maven stream is still marked alpha in upstream metadata,
  so server-side behavior may move under the same `26.2.build.+` dependency
  selector.

## Server Owner Notes

Use this release on Paper 26.2 with Java 25. Spigot and CraftBukkit are not
supported.

Suggested smoke test:

```text
/bot create DuelBot
/bot loadout sword DuelBot
/bot combatdebug DuelBot on
/ai movement 1 DuelBot
```

Watch especially for bot spawn, skin rendering, inventory persistence after
loadout changes, movement output, and normal melee hit timing.
