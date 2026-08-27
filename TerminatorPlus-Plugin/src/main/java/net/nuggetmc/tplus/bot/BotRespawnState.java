package net.nuggetmc.tplus.bot;

import net.nuggetmc.tplus.api.agent.legacyagent.ai.NeuralNetwork;
import net.nuggetmc.tplus.api.utils.SkinData;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.UUID;

record BotRespawnState(
        UUID uuid,
        String name,
        Location spawnLocation,
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
        PlayerInventory inventory = bot.getBukkitEntity().getInventory();
        return new BotRespawnState(
                bot.getUUID(),
                bot.getBotName(),
                bot.originalSpawnLocation(),
                bot.skinData(),
                copy(inventory.getStorageContents()),
                copy(inventory.getArmorContents()),
                copy(inventory.getExtraContents()),
                bot.getBotInventory().getSelectedHotbarSlot(),
                cloneItem(bot.defaultItem),
                bot.getNeuralNetwork(),
                bot.hasShieldEnabled(),
                bot.getTargetPlayer(),
                bot.getKills(),
                bot.getBotInventory().isRespectingLoadout(),
                bot.getTrainingLoadout(),
                bot.isInPlayerList()
        );
    }

    Bot respawn() {
        Bot bot = Bot.createBot(spawnLocation.clone(), name, skin, uuid, inPlayerList);
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
        if (respectLoadout) {
            bot.getBotInventory().markLoadoutApplied();
        }
        bot.getBukkitEntity().updateInventory();
        return bot;
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
