package net.nuggetmc.tplus.bot.gui;

import net.nuggetmc.tplus.bot.Bot;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.PlayerInventory;

/**
 * Double-chest style GUI that mirrors a bot's full inventory.
 *
 * <p>Slot layout (54 slots / 6 rows):
 * <pre>
 *  0–8    → hotbar (bot slots 0–8)
 *  9–17   → storage row 1 (bot slots 9–17)
 *  18–26  → storage row 2 (bot slots 18–26)
 *  27–35  → storage row 3 (bot slots 27–35)
 *  36     → head         (bot helmet)
 *  37     → chest        (bot chestplate / elytra)
 *  38     → legs         (bot leggings)
 *  39     → feet         (bot boots)
 *  40     → offhand      (bot offhand)
 *  41–44  → filler (barrier glass)
 *  45–53  → filler (barrier glass)
 * </pre>
 */
public final class BotInventoryGUI implements InventoryHolder {

    public static final int SIZE = 54;
    private static final int[] ARMOR_GUI_SLOTS = {36, 37, 38, 39, 40};

    private final Bot bot;
    private final Inventory inventory;

    public BotInventoryGUI(Bot bot) {
        this.bot = bot;
        this.inventory = Bukkit.createInventory(this, SIZE,
                ChatColor.GOLD + "Bot: " + ChatColor.YELLOW + bot.getBotName());
        syncFromBot();
    }

    public Bot getBot() {
        return bot;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open(Player viewer) {
        viewer.openInventory(inventory);
    }

    /** Pull bot state into the GUI inventory (call when opening). */
    public void syncFromBot() {
        PlayerInventory pi = bot.getBukkitEntity().getInventory();
        for (int i = 0; i < 36; i++) {
            inventory.setItem(i, pi.getItem(i));
        }
        inventory.setItem(36, pi.getHelmet());
        inventory.setItem(37, pi.getChestplate());
        inventory.setItem(38, pi.getLeggings());
        inventory.setItem(39, pi.getBoots());
        inventory.setItem(40, pi.getItemInOffHand());

        ItemStack filler = buildFiller();
        for (int i = 41; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }
    }

    /** Push GUI state back onto the bot (call when closing). */
    public void syncToBot() {
        PlayerInventory pi = bot.getBukkitEntity().getInventory();
        for (int i = 0; i < 36; i++) {
            pi.setItem(i, safe(inventory.getItem(i)));
        }
        bot.setItem(safe(inventory.getItem(36)), org.bukkit.inventory.EquipmentSlot.HEAD);
        bot.setItem(safe(inventory.getItem(37)), org.bukkit.inventory.EquipmentSlot.CHEST);
        bot.setItem(safe(inventory.getItem(38)), org.bukkit.inventory.EquipmentSlot.LEGS);
        bot.setItem(safe(inventory.getItem(39)), org.bukkit.inventory.EquipmentSlot.FEET);
        bot.setItem(safe(inventory.getItem(40)), org.bukkit.inventory.EquipmentSlot.OFF_HAND);

        // Re-sync the mainhand with the currently selected hotbar slot.
        bot.selectHotbarSlot(bot.getBotInventory().getSelectedHotbarSlot());
    }

    /** Slots in the GUI that are decorative/locked. */
    public static boolean isFillerSlot(int slot) {
        return slot >= 41 && slot < SIZE;
    }

    public static boolean isArmorOrOffhand(int slot) {
        for (int s : ARMOR_GUI_SLOTS) if (s == slot) return true;
        return false;
    }

    private static ItemStack safe(ItemStack it) {
        return it == null ? new ItemStack(Material.AIR) : it;
    }

    private static ItemStack buildFiller() {
        ItemStack it = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            it.setItemMeta(meta);
        }
        return it;
    }
}
