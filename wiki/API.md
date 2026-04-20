# API

TerminatorPlus ships a separate artifact, `TerminatorPlus-API`, so plugins can drive bots without depending on the plugin jar directly.

## Entry point

```java
import net.nuggetmc.tplus.api.TerminatorPlusAPI;
import net.nuggetmc.tplus.api.BotManager;
import net.nuggetmc.tplus.api.Terminator;

BotManager bots = TerminatorPlusAPI.getBotManager();
```

The `BotManager` is the root object for everything spawning- and registry-related.

## Spawning

```java
Terminator bot = bots.createBot(
    location,              // Location
    "MyBot",               // name (also the Mojang skin lookup)
    null,                  // skin (null → look up by name)
    null                   // signature (null → look up by name)
);
```

Overloads accept custom skin/signature pairs, multi-spawn counts, pre-built neural networks, etc. See `BotManager.java`.

### Removing

```java
bots.remove(bot);
bots.reset(); // wipe all
```

## Inspecting a bot

The `Terminator` interface exposes the subset of `ServerPlayer` behavior that's safe to call from the main thread:

```java
bot.getBotName();
bot.getLocation();
bot.getVelocity();
bot.getBotHealth();        // current HP
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

The new weapon-aware AI is reachable through a single call:

```java
boolean handled = bot.combatTick(target);
```

- Returns `true` if the director picked a weapon and executed a behavior.
- Returns `false` if you should fall back to your own attack/targeting logic.
- Returns `false` unconditionally for bots with a neural network (see [Neural Network Mode](Neural-Network-Mode)).

This mirrors the internal contract: use the return value to decide whether to skip your custom pipeline.

## Inventory helpers

`Terminator` gives you the legacy setters:

```java
bot.setItem(new ItemStack(Material.NETHERITE_SWORD), EquipmentSlot.HAND);
bot.setItemOffhand(new ItemStack(Material.SHIELD));
bot.setDefaultItem(new ItemStack(Material.STICK));
```

For full per-slot control (hotbar 0–8, storage 9–35, armor, offhand), cast to the implementation's `Bot` class and call `getBotInventory()`. This is an internal API and may change — prefer the public methods when possible.

## Events

Listeners you can subscribe to:

| Event | Fires when |
| --- | --- |
| `BotFallDamageEvent` | A bot is about to take fall damage (cancellable). |
| `BotDamageByPlayerEvent` | A player attacks a bot. |
| `BotDeathEvent` | A bot dies from any source. |
| `BotKilledByPlayerEvent` | A bot is killed specifically by a player. |
| `TerminatorLocateTargetEvent` | A bot's targeting logic picks a new entity. |

Example:

```java
@EventHandler
public void onBotKilled(BotKilledByPlayerEvent e) {
    Player killer = e.getKiller();
    killer.sendMessage("You killed " + e.getBot().getBotName() + "!");
}
```

## AI training

The training pipeline is exposed via `AIManager`:

```java
AIManager ai = /* see internal bridge */;
```

In practice, most consumers use the `/ai` command surface. Scripting training sessions programmatically requires the plugin jar as a runtime dependency rather than the API artifact alone.

## Compatibility

- API artifact is published against Paper 1.21.1 / Java 21.
- Internal changes happen behind the `Terminator` interface — breaking API changes are called out in the [Changelog](Changelog).
- Because bots are real `ServerPlayer`s, standard Bukkit APIs (`bot.getBukkitEntity()`) mostly work too — but prefer the `Terminator` methods where available for stability.
