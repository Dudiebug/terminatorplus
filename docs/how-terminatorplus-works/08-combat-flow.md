# 8. Combat Flow

This section explains how a bot decides what combat action to take and how that
results in real attack behavior.

## `CombatDirector` owns combat authority

The most important design fact in the combat stack is that
`CombatDirector.java` is the central combat owner.

It is responsible for:

- planning combat intent
- selecting legal or appropriate actions
- preserving committed multi-tick combat phases
- executing passive utility/survival combat actions
- coordinating melee and advanced tools

This is a real architectural improvement over letting movement or command code
choose attacks directly.

## Main combat entrypoints

There are three important entrypoints:

- `Bot.combatTick(target)` -> `CombatDirector.tick(...)`
- `Bot.tickCommittedCombat(target)` -> `CombatDirector.tickCommitted(...)`
- `Bot.executePlannedCombat(target)` -> `CombatDirector.execute(...)`

These exist because the plugin supports both:

- older immediate-per-tick combat assumptions
- newer movement-controller plan/move/execute sequencing

## Planning phase

`CombatDirector.plan(...)` produces a `CombatIntent`.

The intent is deliberately narrower than "do everything combat wants." It is a
contract for movement-aware combat planning.

Examples of what intent can represent:

- desired range
- branch family
- planned action style
- movement pressure expectations
- lock/commit state that affects movement routing

The important design boundary is:

- planning may inform movement
- planning should not let movement become combat-authoritative

## Execution phase

`CombatDirector.execute(...)` consumes current runtime state and decides what
to actually do this tick.

The effective priority structure is:

1. passive survival/utility updates
2. double-execution and duplicate-action safety guards
3. planned action execution
4. committed multi-tick phase continuation
5. combo/scanner-driven opportunities
6. standard fallback branch ordering

The standard fallback branch ordering includes:

- shield/axe response
- crystal behavior
- anchor behavior
- melee or mace behavior
- trident behavior
- pearl behavior
- cobweb/special tactical behavior

This is why the combat stack feels both focused and broad at the same time.
Basic melee is protected, but advanced systems still have a lot of presence.

## Vanilla timing and hit legality

`BotCombatTiming.java` is the timing spine.

It centralizes:

- attack-strength charge checks
- critical-hit timing windows
- mace timing thresholds
- sprint-reset logic
- i-frame and invalid-hit rejection

This matters because without it, the plugin could easily devolve into fake
"spam hits" that compile fine but do not resemble real PvP timing.

So if a future maintainer wants "stronger combat," one of the first questions
should be whether they are strengthening decision quality or merely weakening
timing gates.

## `MeleeBehavior` and fundamentals

`MeleeBehavior.java` is one of the cleanest parts of the combat stack.

It represents the close-range fundamentals the current strategy actually cares
about:

- basic melee swings
- timing-aware attack release
- crit-window cooperation
- shield/weapon context through the surrounding director logic

If the repo keeps only one part of the combat system as a conceptual center of
gravity, it should be the melee/timing spine rather than the broad tactical
catalog.

## Advanced behavior overlay

The combat stack also includes:

- `MaceBehavior`
- `TridentBehavior`
- `CrystalBehavior`
- `AnchorBombBehavior`
- `EnderPearlBehavior`
- `WindChargeBehavior`
- `ConsumableBehavior`
- `TotemBehavior`
- `ElytraBehavior`
- `ComboBehavior`
- `OpportunityScanner`

These are active classes, not dead samples.

But they are not all equal in strategic importance for the current narrow
1v1 direction.

### Why `OpportunityScanner` stands out

`OpportunityScanner` is one of the broadest and most tangled combat surfaces in
the plugin.

It participates in:

- planning
- direct tactical scanning
- immediate execution support
- advanced-tech selection
- delayed cleanup/scheduled behavior

This makes it powerful, but also a prime candidate to eventually wrap or
demote behind a narrower duel-core interface.

## Normal bot vs movement-controller bot vs full-NN bot

### Normal `/bot create` bot

The normal live path still goes through `LegacyAgent.attack(...)`.

That method gives `bot.combatTick(target)` first refusal. If the director
handles the situation, old melee fallback is skipped. If not, legacy melee can
still attack.

### Movement-controller bot

The movement-controller path separates:

- planning combat
- moving according to that plan
- executing combat after movement context has been observed

This is the cleanest runtime expression of the current architectural direction.

### Full-replacement legacy NN bot

The older full-NN path still exists and can drive movement behavior through old
NN classes. It remains part of the runtime surface and command surface, but it
does not represent the main current strategy.

## How damage actually happens

For non-legacy-NN combat, `Bot.attack(entity)` generally delegates to the
vanilla/Bukkit attack path so that real server combat mechanics are used.

For some older or specialized paths, custom logic and guards still exist.

The practical takeaway is:

- attack timing is highly intentional
- fake-player combat is not just "call damage on entity"
- inventory state, held item, movement state, and cooldown state all matter

## What needs runtime testing

Source is enough to understand the structure, but several gameplay judgments
still need runtime test:

- whether advanced scanner tech steals too many fights from melee fundamentals
- whether movement-controller parity matches non-controller combat in real
  duels
- whether mace and trident committed phases behave reliably on rough terrain
- whether consumable/tactical branches create balance drift in duel scenarios
