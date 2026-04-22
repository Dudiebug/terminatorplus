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
            ItemStack it = pi.getItem(i);
            inventory.setItem(i, it == null ? null : it.clone());
        }
        inventory.setItem(36, cloneOrNull(pi.getHelmet()));
        inventory.setItem(37, cloneOrNull(pi.getChestplate()));
        inventory.setItem(38, cloneOrNull(pi.getLeggings()));
        inventory.setItem(39, cloneOrNull(pi.getBoots()));
        inventory.setItem(40, cloneOrNull(pi.getItemInOffHand()));

        ItemStack filler = buildFiller();
        for (int i = 41; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }
    }

    private static ItemStack cloneOrNull(ItemStack it) {
        return it == null ? null : it.clone();
    }

    /** Push GUI state back onto the bot (call when closing). */
    public void syncToBot() {
        // Write hotbar + storage directly to the NMS Inventory, bypassing the Bukkit
        // container-transaction system (which Paper 26.x would roll back for a MockConnection).
        net.minecraft.world.entity.player.Inventory nmsInv = bot.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack it = inventory.getItem(i);
            nmsInv.setItem(i, (it == null || it.getType() == Material.AIR)
                    ? net.minecraft.world.item.ItemStack.EMPTY
                    : org.bukkit.craftbukkit.inventory.CraftItemStack.asNMSCopy(it));
        }
        nmsInv.setChanged();

        // Armor + offhand go through bot.setItem() so ClientboundSetEquipmentPacket is sent.
        bot.setItem(safe(inventory.getItem(36)), org.bukkit.inventory.EquipmentSlot.HEAD);
        bot.setItem(safe(inventory.getItem(37)), org.bukkit.inventory.EquipmentSlot.CHEST);
        bot.setItem(safe(inventory.getItem(38)), org.bukkit.inventory.EquipmentSlot.LEGS);
        bot.setItem(safe(inventory.getItem(39)), org.bukkit.inventory.EquipmentSlot.FEET);
        bot.setItem(safe(inventory.getItem(40)), org.bukkit.inventory.EquipmentSlot.OFF_HAND);

        // Do NOT autoEquip here — that would reshuffle every item back into the
        // default priority layout and wipe any manual arrangement the user made
        // in the GUI. CombatDirector now scans all 36 slots via findHotbar and
        // swaps weapons into the hotbar on demand, so freeform layouts work.
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
