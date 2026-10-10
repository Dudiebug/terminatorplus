package net.nuggetmc.tplus.bot;

import net.nuggetmc.tplus.api.agent.legacyagent.ai.NeuralNetwork;
import net.nuggetmc.tplus.api.utils.SkinData;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.List;
import java.util.UUID;

record BotRespawnState(
        UUID uuid,
        String name,
        Location spawnLocation,
        Location pinnedSpawn,
        SkinData skin,
        ItemStack[] storage,
        ItemStack[] armor,
        ItemStack[] extra,
        int selectedHotbarSlot,
        ItemStack defaultItem,
        NeuralNetwork network,
        boolean shield,
        UUID targetPlayer,
        int kills,
        boolean respectLoadout,
        String trainingLoadout,
        boolean inPlayerList
) {

    static BotRespawnState capture(Bot bot) {
        Location pinned = bot.pinnedSpawn();
        Location spawn = chooseSpawn(pinned, bot.respawnAnchor(), bot.originLocation());
        if (spawn == null) return null;

        var botInventory = bot.getBotInventory();
        return new BotRespawnState(
                bot.getUUID(),
                bot.getBotName(),
                spawn,
                pinned,
                bot.skinData(),
                botInventory.respawnStorageContents(),
                botInventory.respawnArmorContents(),
                botInventory.respawnExtraContents(),
                botInventory.respawnSelectedHotbarSlot(),
                cloneItem(bot.defaultItem),
                bot.getNeuralNetwork(),
                bot.hasShieldEnabled(),
                bot.getTargetPlayer(),
                bot.getKills(),
                botInventory.isRespectingLoadout(),
                bot.trainingLoadout(),
                bot.isInPlayerList()
        );
    }

    /**
     * A setspawn point wins, then the auto-captured landing spot, then the
     * point the bot was created at (covers bots that die before ever landing).
     */
    static Location chooseSpawn(Location pinned, Location anchor, Location origin) {
        for (Location candidate : new Location[]{pinned, anchor, origin}) {
            if (candidate != null) return candidate.clone();
        }
        return null;
    }

    BotRespawnState withPinnedSpawn(Location pinned) {
        Location copy = pinned == null ? null : pinned.clone();
        return new BotRespawnState(uuid, name, copy == null ? spawnLocation : copy, copy, skin, storage, armor,
                extra, selectedHotbarSlot, defaultItem, network, shield, targetPlayer, kills, respectLoadout,
                trainingLoadout, inPlayerList);
    }

    /**
     * @param useWorldSpawnFallback when the usual spot is unusable, try the
     *                              world spawn instead of giving up
     */
    Bot respawn(boolean useWorldSpawnFallback) {
        Location location = pinnedSpawn != null
                ? RespawnSafety.resolvePinned(pinnedSpawn)
                : RespawnSafety.findNearestSafe(spawnLocation);
        if (location == null && useWorldSpawnFallback) {
            location = RespawnSafety.findNearestSafe(worldSpawn());
        }
        if (location == null) return null;

        Bot bot = Bot.createBot(location, name, skin, uuid, inPlayerList);
        PlayerInventory inventory = bot.getBukkitEntity().getInventory();
        inventory.setStorageContents(copy(storage));
        inventory.setArmorContents(copy(armor));
        inventory.setExtraContents(copy(extra));
        bot.getBotInventory().setSelectedHotbarSlot(selectedHotbarSlot);
        bot.setDefaultItem(cloneItem(defaultItem));
        bot.setNeuralNetwork(network);
        bot.restoreShieldFlag(shield);
        bot.setTargetPlayer(targetPlayer);
        bot.restoreKills(kills);
        bot.restoreTrainingLoadout(trainingLoadout);
        bot.setPinnedSpawn(pinnedSpawn);
        bot.grantRespawnFallGrace();
        if (respectLoadout) {
            bot.getBotInventory().markLoadoutApplied();
        } else {
            bot.getBotInventory().saveForRespawn();
        }
        bot.getBukkitEntity().updateInventory();
        RespawnSafety.emitPoof(bot.getLocation(), BotRespawnState::spawnPoof);
        return bot;
    }

    private Location worldSpawn() {
        World world = spawnLocation != null && spawnLocation.isWorldLoaded() ? spawnLocation.getWorld() : null;
        if (world == null) {
            List<World> worlds = Bukkit.getWorlds();
            if (worlds.isEmpty()) return null;
            world = worlds.get(0);
        }
        return world.getSpawnLocation();
    }

    private static void spawnPoof(Location location) {
        World world = location.getWorld();
        if (world != null) {
            world.spawnParticle(Particle.CLOUD, location, 12, 0.25, 0.35, 0.25, 0.02);
        }
    }

    static ItemStack[] copy(ItemStack[] contents) {
        if (contents == null) return new ItemStack[0];
        ItemStack[] copy = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            copy[i] = cloneItem(contents[i]);
        }
        return copy;
    }

    private static ItemStack cloneItem(ItemStack item) {
        return item == null ? null : item.clone();
    }
}
