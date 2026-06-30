# Risk Register

| Risk | Severity | Likelihood | Affected files/areas | Mitigation | Test required |
|---|---|---:|---|---|---|
| Paper/NMS internals break | High | Medium | Bot, BotInventory, version-specific code | Avoid unless runtime-specific | spawn/loadout/inventory test |
| MockConnection behavior changes | High | Medium | bot creation, inventory, packet sync | preserve existing patterns | spawn and interact test |
| Inventory rollback | High | Medium | BotInventory, loadouts | use existing NMS-backed writes | apply/loadout/close GUI test |
| Packet sync issues | Medium | Medium | equipment, held slot, inventory | avoid unnecessary slot writes | visual held-item test |
| Branch cherry-pick mistakes | High | Medium | all | primary target is mc-26.1.2 | branch review |
| buildSrc Gradle corruption | Medium | Medium | buildSrc | delete only buildSrc/build | build test |
| Neural-network mode regression | High | Medium | movement/training/persistence | preserve schemas and commands | /ai commands + build |
| Movement-only contract violation | High | Medium | movement files | keep contract check | ./gradlew build -q |
| Combat logic too complex | Medium | High | CombatDirector, OpportunityScanner | small helpers, debug labels | duel behavior test |
| Opportunity scanner overfitting | Medium | Medium | OpportunityScanner | fundamentals first | special-trigger logs |
| Special moves override fundamentals | High | High | mace/trident/crystal/anchor | gate advanced tools | duel baseline test |
| Debug logging too noisy | Low | Medium | debug systems | opt-in logs | console review |
| Runtime-only bugs | High | High | all gameplay code | manual duel tests | live server test |
| 1.21.11 vs 26.1.2 differences | High | Medium | Paper/NMS APIs | do current work on mc-26.1.2 | branch/version test |
