# TerminatorPlus 6.2.3 - Paper 26.2 Prerelease

TerminatorPlus 6.2.3 keeps the organized `/bot` command surface and replaces
the bot inventory flow with an explicit, exact-target editor.

## Highlights

- Adds previous/next bot-page controls with clamped page state and empty-list
  feedback in the management UI.
- Rejects ambiguous name-based inventory commands and keeps the management-menu
  inventory action tied to the selected bot UUID.
- Makes inventory editing transactional: changes are local until **Save
  changes**, while Discard, Close, disconnect, removal, permission loss, and
  shutdown discard unsaved changes.
- Blocks inventory shortcuts and transfer paths that bypass slot validation,
  validates armor placement, and adds a one-editor-per-bot lock.
- Makes auto-equip an explicit opt-in at save time and reports dispatch status
  honestly in the management UI.

## Compatibility and scope

- Existing command aliases remain available.
- This is a prerelease for Paper 26.2 and Java 25.
- Live Paper acceptance testing is still required before a production rollout.

## Artifact

`TerminatorPlus-6.2.3-BETA-mc26.2.jar`
