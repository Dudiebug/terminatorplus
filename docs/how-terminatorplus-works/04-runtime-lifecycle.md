# 4. Runtime Lifecycle: Server Startup to Shutdown

This section traces what happens when Paper loads the plugin and what must be
cleaned up when the plugin stops.

## Startup flow

The entrypoint is `TerminatorPlus.onEnable()` in
`TerminatorPlus-Plugin/src/main/java/net/nuggetmc/tplus/TerminatorPlus.java`.

The startup sequence is roughly:

1. `saveDefaultConfig()` ensures the plugin data folder has a config file.
2. The plugin logs version information and performs basic startup reporting.
3. `BotManagerImpl` is constructed.
4. `CombatDirector` is constructed.
5. `PresetManager` is constructed.
6. `CommandHandler` is constructed and registers command classes.
7. The API bridge is published through `TerminatorPlusAPI`.
8. Listener registration becomes active through manager/command setup.

### The most important hidden startup fact

`BotManagerImpl` constructs `new LegacyAgent(...)` inside its constructor.

That matters because `LegacyAgent` extends `Agent`, and `Agent` starts its
repeating task from the constructor. So the top-level AI tick scheduler is not
created lazily when the first bot is spawned. It becomes active at plugin
startup.

In practical terms:

- if the manager exists, the agent exists
- if the agent exists, the recurring tick task exists

That means startup bugs can have tick-loop implications even before a bot is
created.

## Plugin-wide state created during startup

Important state created and/or published at enable time includes:

- bot manager singleton-like access through `TerminatorPlusAPI`
- internal bridge implementation
- combat director instance
- preset manager instance
- command registry metadata
- default config-backed behavior flags

This repo is not heavily dependency-injected. It uses shared plugin-owned
instances and API bridge publication instead.

## Listener and command registration

`CommandHandler` scans command classes and maps annotated methods into command
execution handlers.

Registered root commands are:

- `/bot`
- `/terminatorplus`
- `/ai`
- `/botenvironment`

Bot-related event listeners are also active through `BotManagerImpl`, such as:

- join-time rerender handling
- death handling
- target-prevention hooks for hostile mobs when manager settings disable them

## Startup implications for bot runtime

Even before bots exist, startup establishes several runtime assumptions:

- a combat director is available
- a bot manager is available
- the live agent loop is available
- persistence/config paths are known
- command surfaces can mutate high-risk runtime state later

That is why lifecycle understanding is part of architecture understanding here.

## Shutdown flow

The main shutdown entrypoint is `TerminatorPlus.onDisable()`.

The order matters.

The shutdown sequence is roughly:

1. `manager.reset()` is called first.
2. Bots are removed.
3. agent tasks are cancelled.
4. plugin-held manager/director/preset/handler references are nulled.
5. `TerminatorPlusAPI` shared bridge state is cleared.
6. `CustomGameProfile` username cache is cleared.
7. `MojangAPI.shutdown()` is called.
8. `Debugger.shutdown()` is called.
9. `CombatDebugger.shutdown()` is called.
10. `MovementOutputApplier.clearAll()` clears static movement output state.

### Why `manager.reset()` comes first

The plugin cannot safely clear global/plugin state before it removes bots and
stops the agent. The bot layer still depends on manager/director/plugin context
while cleaning itself up.

## Bot removal during shutdown

Each bot's `removeBot()` path in `Bot.java` is a deep cleanup operation.

It:

- reschedules onto the main thread if removal was requested off-thread
- guards against duplicate cleanup
- cancels bot-scheduled tasks
- removes the bot from the manager
- lets `CombatDirector` clean per-bot state
- disables combat debug for that bot
- clears movement output state
- removes the entity from the world
- sends visual removal packets
- removes fake player-list entries if needed
- closes menus/container state

This is one of the clearest "do not simplify casually" areas in the codebase.

## Runtime state and cleanup risks

If shutdown or bot cleanup is incomplete, likely failure modes include:

- stale scheduled tasks continuing to reference dead plugin objects
- stale movement state in `MovementOutputApplier`
- stale combat debug files or open writer state
- ghost crack-animation state from legacy block-breaking tasks
- fake player-list entries not removed correctly
- entities visually removed but not fully cleaned, or vice versa
- future player joins not seeing consistent bot render state

## What this means for future changes

Any work that touches:

- plugin startup ordering
- manager construction
- agent scheduling
- bot removal ordering
- static/shared caches

should be treated as lifecycle-sensitive, not just local cleanup.

If a future refactor breaks lifecycle order but not compilation, the plugin can
still regress badly at runtime.
