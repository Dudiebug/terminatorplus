package net.nuggetmc.tplus.bot.loadout;

import net.nuggetmc.tplus.bot.Bot;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Helper around a bot's underlying PlayerInventory that adds a tracked
 * "selected hotbar slot" (since bots are not real players and don't get
 * ClientboundSetCarriedItemPacket state from a client) and a handful of
 * convenience queries used by the combat / weapon-switching layer.
 *
 * All item storage lives on the bot's native Bukkit PlayerInventory, so
 * the chest-based GUI editor can read and write the same 41 slots
 * (9 hotbar + 27 storage + 4 armor + 1 offhand).
 */
public final class BotInventory {

    public static final int HOTBAR_SIZE = 9;

    private final Bot bot;
    private int selectedHotbarSlot;

    public BotInventory(Bot bot) {
        this.bot = bot;
        this.selectedHotbarSlot = 0;
    }

    public Bot getBot() {
        return bot;
    }

    public PlayerInventory raw() {
        return bot.getBukkitEntity().getInventory();
    }

    public int getSelectedHotbarSlot() {
        return selectedHotbarSlot;
    }

    /**
     * Change the selected hotbar slot. Syncs the mainhand item to
     * whatever is at that slot so other players see the swap.
     */
    public void setSelectedHotbarSlot(int slot) {
        if (slot < 0 || slot >= HOTBAR_SIZE) return;
        this.selectedHotbarSlot = slot;
        ItemStack item = raw().getItem(slot);
        bot.setItem(item == null ? new ItemStack(Material.AIR) : item.clone(), EquipmentSlot.HAND);
    }

    public ItemStack getSelected() {
        ItemStack item = raw().getItem(selectedHotbarSlot);
        return item == null ? new ItemStack(Material.AIR) : item;
    }

    public ItemStack getHotbar(int slot) {
        if (slot < 0 || slot >= HOTBAR_SIZE) return new ItemStack(Material.AIR);
        ItemStack item = raw().getItem(slot);
        return item == null ? new ItemStack(Material.AIR) : item;
    }

    public int findHotbar(Material type) {
        if (type == null) return -1;
        PlayerInventory inv = raw();
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            ItemStack it = inv.getItem(i);
            if (it != null && it.getType() == type) return i;
        }
        return -1;
    }

    /** Any hotbar slot contains a stack of {@code type}. */
    public boolean hasHotbar(Material type) {
        return findHotbar(type) >= 0;
    }

    public boolean hasMace() {
        return hasHotbar(Material.MACE);
    }

    public boolean hasTrident() {
        return hasHotbar(Material.TRIDENT);
    }

    public boolean hasWindCharge() {
        return hasHotbar(Material.WIND_CHARGE);
    }

    public boolean hasFirework() {
        return hasHotbar(Material.FIREWORK_ROCKET);
    }

    public boolean hasEnderPearl() {
        return hasHotbar(Material.ENDER_PEARL);
    }

    /** Any slot (not only hotbar) holds a totem of undying. */
    public boolean hasTotem() {
        PlayerInventory inv = raw();
        for (int i = 0; i < 36; i++) {
            ItemStack it = inv.getItem(i);
            if (it != null && it.getType() == Material.TOTEM_OF_UNDYING) return true;
        }
        ItemStack off = inv.getItemInOffHand();
        return off != null && off.getType() == Material.TOTEM_OF_UNDYING;
    }

    /** True if the bot has both an end crystal and a host block (obsidian or, in nether, glowstone). */
    public boolean hasCrystalKit() {
        return hasHotbar(Material.END_CRYSTAL)
                && (hasHotbar(Material.OBSIDIAN) || hasHotbar(Material.GLOWSTONE));
    }

    public boolean hasAnchorKit() {
        return hasHotbar(Material.RESPAWN_ANCHOR) && hasHotbar(Material.GLOWSTONE);
    }

    public boolean hasCobweb() {
        return hasHotbar(Material.COBWEB);
    }

    public boolean hasElytra() {
        ItemStack chest = raw().getChestplate();
        if (chest != null && chest.getType() == Material.ELYTRA) return true;
        return findStoredChestpieceOfType(Material.ELYTRA) >= 0;
    }

    /**
     * Search the full inventory (storage + hotbar + offhand) for a
     * chestplate-style item (ELYTRA or any *_CHESTPLATE). Returns the
     * raw inventory slot index, or -1.
     */
    public int findStoredChestpieceOfType(Material type) {
        PlayerInventory inv = raw();
        // Slots 0-35 are hotbar + main storage in Bukkit's PlayerInventory.
        for (int i = 0; i < 36; i++) {
            ItemStack it = inv.getItem(i);
            if (it != null && it.getType() == type) return i;
        }
        ItemStack off = inv.getItemInOffHand();
        if (off != null && off.getType() == type) return 40; // offhand slot index
        return -1;
    }

    /**
     * If the chest slot doesn't already hold {@code desired}, swap it
     * with the first instance found elsewhere in the inventory. Does
     * nothing if no such item is carried.
     *
     * @return true if a swap happened
     */
    public boolean swapChest(Material desired) {
        PlayerInventory inv = raw();
        ItemStack current = inv.getChestplate();
        if (current != null && current.getType() == desired) return false;

        int slot = findStoredChestpieceOfType(desired);
        if (slot < 0) return false;

        ItemStack stored = inv.getItem(slot);
        ItemStack chestPrev = current == null ? new ItemStack(Material.AIR) : current.clone();

        // Put desired item on the body.
        bot.setItem(stored.clone(), EquipmentSlot.CHEST);
        // Stash the previous chest item where the desired one used to live.
        if (slot == 40) {
            inv.setItemInOffHand(chestPrev);
        } else {
            inv.setItem(slot, chestPrev);
        }
        return true;
    }

    /** First hotbar slot whose item is a sword, or -1. */
    public int findSword() {
        PlayerInventory inv = raw();
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            ItemStack it = inv.getItem(i);
            if (it == null) continue;
            String n = it.getType().name();
            if (n.endsWith("_SWORD")) return i;
        }
        return -1;
    }

    public int findAxe() {
        PlayerInventory inv = raw();
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            ItemStack it = inv.getItem(i);
            if (it == null) continue;
            String n = it.getType().name();
            if (n.endsWith("_AXE")) return i;
        }
        return -1;
    }

    /** Heuristic: first hotbar slot that heals (golden apple / potion of healing / totem). */
    public int findHealing() {
        PlayerInventory inv = raw();
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            ItemStack it = inv.getItem(i);
            if (it == null) continue;
            Material m = it.getType();
            if (m == Material.GOLDEN_APPLE || m == Material.ENCHANTED_GOLDEN_APPLE
                    || m == Material.TOTEM_OF_UNDYING || m == Material.POTION) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Move the given material onto the offhand slot from any other slot.
     * If something else was in the offhand, park it into the source slot so
     * the swap is a true exchange. Returns true on swap.
     */
    public boolean equipOffhand(Material desired) {
        PlayerInventory inv = raw();
        ItemStack off = inv.getItemInOffHand();
        if (off != null && off.getType() == desired) return false;

        int source = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack it = inv.getItem(i);
            if (it != null && it.getType() == desired) {
                source = i;
                break;
            }
        }
        if (source < 0) return false;

        ItemStack pickup = inv.getItem(source).clone();
        ItemStack previous = off == null ? new ItemStack(Material.AIR) : off.clone();
        bot.setItemOffhand(pickup);
        inv.setItem(source, previous);
        return true;
    }
}
