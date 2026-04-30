# API

TerminatorPlus ships a separate artifact, `TerminatorPlus-API`, so plugins can drive bots without depending on the plugin jar directly.

## Entry point

```java
import net.nuggetmc.tplus.api.TerminatorPlusAPI;
import net.nuggetmc.tplus.api.BotManager;
import net.nuggetmc.tplus.api.Terminator;

BotManager bots = TerminatorPlusAPI.getBotManager();
```

The `BotManager` is the root object for spawning and registry.

## Spawning

```java
Terminator bot = bots.createBot(
    location,              // Location
    "MyBot",               // name and default skin lookup
    null,                  // skin, null means look up by name
    null                   // signature, null means look up by name
);
```

Overloads accept custom skin/signature pairs, multi-spawn counts, and pre-built neural networks. See `BotManager.java`.

### Removing

```java
bots.remove(bot);
bots.reset();
```

## Inspecting a Bot

The `Terminator` interface exposes the subset of `ServerPlayer` behavior safe to call from the main thread:

```java
bot.getBotName();
bot.getLocation();
bot.getVelocity();
bot.getBotHealth();
bot.getBotMaxHealth();
bot.isBotOnGround();
bot.isFalling();
bot.getDimension();        // World.Environment
bot.getAliveTicks();
```

Actions:

```java
bot.attack(targetEntity);
bot.jump();
bot.walk(velocityVector);
bot.faceLocation(location);
bot.attemptBlockPlace(loc, Material.OBSIDIAN, false);
bot.setShield(true);
```

## Driving the Combat Director

```java
boolean handled = bot.combatTick(target);
```

- Returns `true` if the director picked a weapon and executed a behavior.
- Returns `false` if you should fall back to your own attack/targeting logic.

This mirrors the internal contract: use the return value to decide whether to skip your custom pipeline.

## Inventory Helpers

```java
bot.setItem(new ItemStack(Material.NETHERITE_SWORD), EquipmentSlot.HAND);
bot.setItemOffhand(new ItemStack(Material.SHIELD));
bot.setDefaultItem(new ItemStack(Material.STICK));
```

For full per-slot control over hotbar, storage, armor, and offhand, cast to the implementation's `Bot` class and call `getBotInventory()`. This is an internal API and may change.

## Events

| Event | Fires when |
| --- | --- |
| `BotFallDamageEvent` | A bot is about to take fall damage, cancellable |
| `BotDamageByPlayerEvent` | A player attacks a bot |
| `BotDeathEvent` | A bot dies from any source |
| `BotKilledByPlayerEvent` | A bot is killed specifically by a player |
| `TerminatorLocateTargetEvent` | A bot's targeting logic picks a new entity |

Example:

```java
@EventHandler
public void onBotKilled(BotKilledByPlayerEvent e) {
    Player killer = e.getKiller();
    killer.sendMessage("You killed " + e.getBot().getBotName() + "!");
}
```

## AI Training

The training pipeline is exposed via `AIManager`. In practice, most consumers use the `/ai` command surface. Scripting training sessions programmatically requires the plugin jar as a runtime dependency.

## Movement Network Classes

The movement network architecture lives in the API module under `net.nuggetmc.tplus.api.agent.legacyagent.ai.movement`:

| Class | Purpose |
| --- | --- |
| `MovementNetwork` | Feed-forward network with evaluate/validate/flatten methods |
| `MovementNetworkShape` | Constants: INPUT_COUNT=37, OUTPUT_COUNT=8, DEFAULT_LAYERS |
| `MovementNetworkGenetics` | GA operations: crossover, mutation, tournament selection |
| `MovementTrainingConfig` | Config loading from plugin YAML |
| `MovementTrainingSnapshot` | Read-only movement signal captured on the server thread for fitness |
| `CombatTrainingSnapshot` | Read-only combat/loadout signal captured on the server thread for fitness |
| `MovementLoadoutSampler` | Weighted training loadout sampler for named mixes |
| `MovementRewardProfile` | Per-family reward scoring |
| `MovementBrainBank` | Runtime family router and in-memory bank |
| `MovementBrainPersistence` | Manifest plus per-family brain save/load/reset |

These are public classes, but the movement system is still evolving. Pin to a specific version if you depend on them directly.

## Compatibility

- Current 5.2.x jars are built for Paper 26.1.2 with Java 25.
- Internal changes happen behind the `Terminator` interface; breaking API changes are called out in the [Changelog](Changelog).
- Bots are real `ServerPlayer`s, so standard Bukkit APIs such as `bot.getBukkitEntity()` mostly work. Prefer `Terminator` methods for stability.

## Source

See the [API module source](https://github.com/Dudiebug/terminatorplus/tree/master/TerminatorPlus-API/src/main/java/net/nuggetmc/tplus/api).
