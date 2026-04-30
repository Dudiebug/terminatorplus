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
    "MyBot",               // name (also the Mojang skin lookup)
    null,                  // skin (null = look up by name)
    null                   // signature (null = look up by name)
);
```

Overloads accept custom skin/signature pairs, multi-spawn counts, and pre-built neural networks. See `BotManager.java`.

### Removing

```java
bots.remove(bot);
bots.reset(); // wipe all
```

## Inspecting a bot

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

## Driving the combat director

```java
boolean handled = bot.combatTick(target);
```

- Returns `true` if the director picked a weapon and executed a behavior.
- Returns `false` if you should fall back to your own attack/targeting logic.

This mirrors the internal contract: use the return value to decide whether to skip your custom pipeline.

## Inventory helpers

```java
bot.setItem(new ItemStack(Material.NETHERITE_SWORD), EquipmentSlot.HAND);
bot.setItemOffhand(new ItemStack(Material.SHIELD));
bot.setDefaultItem(new ItemStack(Material.STICK));
```

For full per-slot control (hotbar 0--8, storage 9--35, armor, offhand), cast to the implementation's `Bot` class and call `getBotInventory()`. This is an internal API and may change.

## Events

| Event | Fires when |
| --- | --- |
| `BotFallDamageEvent` | A bot is about to take fall damage (cancellable) |
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

## AI training

The training pipeline is exposed via `AIManager`. In practice, most consumers use the `/ai` command surface. Scripting training sessions programmatically requires the plugin jar as a runtime dependency.

## Movement network classes (API module)

The movement network architecture lives in the API module under `net.nuggetmc.tplus.api.agent.legacyagent.ai.movement`:

| Class | Purpose |
| --- | --- |
| `MovementNetwork` | Feed-forward network with evaluate/validate/flatten methods |
| `MovementNetworkShape` | Constants: INPUT_COUNT=30, OUTPUT_COUNT=8, DEFAULT_LAYERS |
| `MovementNetworkGenetics` | GA operations: crossover, mutation, tournament selection |
| `MovementTrainingConfig` | Config loading from plugin YAML |
| `MovementTrainingSnapshot` | Read-only training signal for fitness |
| `MovementBrainPersistence` | Save/load/reset brain JSON files |

These are public classes, but the movement system is still evolving. Pin to a specific version if you depend on them directly.

## Compatibility

- API artifact is built against Paper 26.1.2 / Paper 1.21.11 with Java 25.
- Internal changes happen behind the `Terminator` interface — breaking API changes are called out in the [Changelog](Changelog).
- Bots are real `ServerPlayer`s, so standard Bukkit APIs (`bot.getBukkitEntity()`) mostly work. Prefer `Terminator` methods for stability.

## Source

See the [API module source](https://github.com/Dudiebug/terminatorplus/tree/master/TerminatorPlus-API/src/main/java/net/nuggetmc/tplus/api).
