# 6. Main Runtime Tick and Process Flow

This section explains what keeps bots alive after they are spawned.

## The top-level scheduler

The live tick loop is owned by `Agent.java`.

`LegacyAgent` extends `Agent`, and `Agent` starts a repeating scheduled task in
its constructor. Because `BotManagerImpl` constructs `LegacyAgent` at plugin
startup, the recurring runtime loop exists for the lifetime of the plugin.

That means the main orchestration flow is:

- plugin startup constructs manager
- manager constructs agent
- agent schedules repeating task
- repeating task calls `LegacyAgent.tick()`

## Why `LegacyAgent` still matters so much

Even though combat and movement systems have evolved, `LegacyAgent` still owns
the outer per-bot orchestration loop.

That includes:

- iterating live bots
- locating targets
- running environment and survival checks
- choosing movement mode
- deciding when to call combat planning/execution hooks
- falling back to old melee when newer combat code does not consume the tick

So even where newer systems are more aligned with the current strategy,
`LegacyAgent` still decides the order in which those systems get a chance to
run.

## Per-bot tick outline

Inside `LegacyAgent.tick()`, the runtime repeatedly does roughly this for each
live bot:

1. Gather bot-specific runtime state.
2. Find a target with `locateTarget(...)`.
3. Run target-override event flow through `TerminatorLocateTargetEvent`.
4. Run survival and obstacle checks.
5. Advance already committed combat phases.
6. Choose movement mode.
7. Execute movement.
8. Execute planned or fallback combat.

The exact helpers are spread across `LegacyAgent`, `Bot`, and combat/movement
classes, but this is the effective control loop.

## Entity-side tick work in `Bot.java`

`Bot.java` also contains entity-local ticking behavior.

Important work in `Bot.tick()` and `doTick()` includes:

- chunk loading / safe tick assumptions
- calling `super.tick()`
- movement-state updates
- fall handling
- passive regeneration behavior
- damage-related checks
- periodic `BotInventory.ensureMovementKit()`
- equipment update detection
- base tick behavior
- manual attack-strength ticker updates

This means there are two important kinds of "tick logic" in the plugin:

- outer orchestration tick in `LegacyAgent`
- entity-local maintenance tick in `Bot`

They are related but not identical.

## Text sequence diagram

```text
Plugin enabled
  -> BotManagerImpl created
    -> LegacyAgent created
      -> Agent schedules repeating task

Each server tick
  -> Agent scheduled task fires
    -> LegacyAgent.tick()
      -> for each live bot
        -> locateTarget()
        -> event override/cancel opportunity
        -> pre-MLG / clutch / hazard / obstacle checks
        -> bot.tickCommittedCombat(target)
        -> choose movement mode
        -> if movement-controller:
             -> bot.planCombat(target)
             -> bot.tryMovementControllerMove(target)
             -> bot.executePlannedCombat(target)
           else if full-NN:
             -> legacy full-replacement NN movement path
             -> LegacyAgent.attack()
           else:
             -> moveLegacy()
             -> LegacyAgent.attack()

Bot entity tick
  -> Bot.tick()
    -> super.tick()
    -> maintenance / state updates / inventory upkeep
    -> doTick()
```

## Committed combat phases

One subtle but important part of the tick flow is the existence of
`bot.tickCommittedCombat(target)`.

This exists so that combat branches with multi-tick commitments, especially
mace and trident flows, can continue progressing even if movement timing would
otherwise interrupt them.

That is a bridge layer between:

- old immediate-combat assumptions
- new plan-then-move-then-execute movement-controller flow

## Why tick order matters

The order is not arbitrary.

Examples:

- survival checks before ordinary movement can prevent obvious fall/lava
  failures
- combat planning before controller movement lets movement react to combat
  intent
- committed combat advancement before ordinary attack branching prevents some
  multi-tick actions from stalling

Changing order can easily alter behavior without looking like a "big change" in
code review.

## Fast way to reason about a bot tick

A useful simplification is:

- `LegacyAgent` decides what problem the bot is solving this tick
- movement code decides how to locomote toward that problem
- `CombatDirector` decides which legal combat action should happen
- `Bot` maintains the entity state needed to make all of that behave like a
  server-side player

That model is not perfect, but it is close enough to make the codebase much
easier to navigate.
