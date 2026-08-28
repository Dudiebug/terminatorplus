# TerminatorPlus 6.2.7 - mc26.2

This prerelease fixes LuckPerms/Vault main-thread lookup warnings for bots.

## Fixed

- Bot UUIDs are generated as version-2 NPC UUIDs so LuckPerms routes Vault
  permission and metadata checks through its configured NPC group instead of
  attempting an offline-player database lookup on the server thread.
- Restored the UUID regression test that was present on the 6.2.0 release
  branch but was accidentally omitted when later releases resumed from
  `master`.

## Validation

- The UUID regression test verifies 512 generated bot IDs are unique,
  version-2 NPC UUIDs with the expected player-skin parity.
- Full Gradle build and automated tests pass.

Runtime verification with LuckPerms, Vault, and the reporter's larger plugin
stack is still recommended.
