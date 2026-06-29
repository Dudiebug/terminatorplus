# 5. Bot Creation Flow

This section explains how `/bot create <name>` becomes a live fake player in
the world.

## High-level path

The creation path crosses several layers:

1. command parsing
2. manager orchestration
3. profile/skin setup
4. NMS `ServerPlayer` creation
5. fake network connection setup
6. world insertion
7. render packet broadcasting
8. manager registration

The key classes are:

- `BotCommand.java`
- `BotManagerImpl.java`
- `Bot.java`
- `MockConnection.java`
- `MockChannel.java`
- `CustomGameProfile.java`

## Step 1: command parsing

`BotCommand.create(...)` reads the command arguments and chooses where and how
to create the bot.

It gathers details such as:

- name
- spawn location
- optional skin information
- optional count/multi-bot behavior depending on command path

The command does not directly construct NMS objects. It delegates that job to
the manager.

## Step 2: manager orchestration

`BotManagerImpl.createBotsAsync(...)` is the important first manager method.

The manager uses an async path for Mojang skin/profile retrieval so that
network-like work does not block the main thread longer than necessary.

The common pattern is:

- fetch profile/skin data async
- hop back to the main server thread
- do actual entity construction and world mutation only on the main thread

That main-thread return is not optional. NMS entity insertion and most Bukkit
world interactions must happen on the server thread.

## Step 3: profile and skin setup

`CustomGameProfile.create(...)` builds the final profile for the bot.

It can:

- create a profile with a random UUID
- apply name trimming/normalization
- attach skin texture/signature data when available

This layer exists so that bot creation can support skinned fake players without
duplicating low-level profile logic at the command or manager level.

## Step 4: `ServerPlayer` creation

`Bot.createBot(...)` is the main NMS construction path.

`Bot` extends `ServerPlayer`, so creating a bot is not creating a lightweight
wrapper around a Bukkit `Player`. It is constructing a full server-side player
entity with custom runtime behavior.

Important setup in `Bot` includes:

- linking the bot to the plugin and manager context
- setting combat/runtime state holders
- setting inventory/loadout helpers
- creating the packet listener stack

## Step 5: fake network connection setup

The bot gets a `ServerGamePacketListenerImpl`, but the connection behind it is
not a real client socket. It is backed by `MockConnection`.

`MockConnection` exists because large parts of the player runtime stack assume
that a player has a network connection object.

Important behavior in `MockConnection.java`:

- it extends `Connection`
- it reflects into internal listener fields with name-first, fallback-based
  lookup
- it uses a loopback sentinel address such as `127.0.0.1:0`
- `isConnected()` reports true even though there is no real client session
- outbound sends are effectively no-ops

`MockChannel` is inert support scaffolding for this fake transport setup.

This is one of the most Paper/NMS-sensitive areas in the repo.

## Step 6: world insertion

The bot can be inserted differently depending on whether it should be in the
player list.

Two important patterns exist in `Bot.java`:

- player-list-aware path using `PlayerList.getPlayers()`,
  `ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(...)`, and
  `ServerLevel.addNewPlayer(...)`
- lighter path using `ADD_PLAYER` packet behavior plus
  `ServerLevel.addFreshEntity(...)`

This split is intentional.

It should not be simplified into "just always do one insertion style" without
runtime proof, because fake players and Paper internals are sensitive to the
exact path used.

## Step 7: render packets and initial appearance

After the entity exists, `bot.renderAll()` sends the visual packets needed so
real players can see the bot correctly.

That includes:

- player info packets
- add/spawn packets
- entity data packets
- head rotation or look packets

`renderAll()` also sets the skin/customization data using
`Player.DATA_PLAYER_MODE_CUSTOMISATION`, which avoids hardcoding the data slot
index.

That "use the constant, not a hardcoded slot" detail is important across
Minecraft/Paper version movement.

## Step 8: manager registration

Once the bot is created and rendered, `BotManagerImpl.add(bot)` stores it in
the live bot collection.

From that point on:

- the bot will be visited by the agent tick loop
- command surfaces can find and mutate it
- join-time rerender logic can resend it to later viewers

## Join-time rerender

`BotManagerImpl.onJoin(PlayerJoinEvent)` iterates over existing bots and calls
`bot.renderBot(connection, true)` for the joining player.

This means visibility is not only established at spawn time. The plugin also
maintains bot visibility for players who join later.

## First-tick assumptions

The bot is considered a real live runtime participant immediately after
registration.

Important assumptions become true:

- it can be targeted
- it can tick
- it can be moved
- it can enter combat
- its inventory state matters
- cleanup must work if it is removed quickly after creation

## Paper/NMS-sensitive points to remember

The following points are protected:

- `ServerPlayer` construction path
- `MockConnection` reflection fallback logic
- loopback sentinel address behavior
- player-list insertion vs non-player-list insertion split
- render packet ordering and entity-data usage
- main-thread-only world insertion/removal

If a future change breaks bot spawn without compile errors, this path is one of
the first places to inspect.
