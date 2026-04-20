package net.nuggetmc.tplus.bot.loadout;

import net.nuggetmc.tplus.bot.Bot;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;

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

    // Materials placed into the hotbar in this priority order by autoEquip().
    private static final List<Material> HOTBAR_PRIORITY = List.of(
        Material.MACE,
        Material.NETHERITE_SWORD, Material.DIAMOND_SWORD, Material.IRON_SWORD,
        Material.STONE_SWORD, Material.GOLDEN_SWORD, Material.WOODEN_SWORD,
        Material.TRIDENT,
        Material.NETHERITE_AXE, Material.DIAMOND_AXE, Material.IRON_AXE,
        Material.STONE_AXE, Material.GOLDEN_AXE, Material.WOODEN_AXE,
        Material.WIND_CHARGE,
        Material.ENDER_PEARL,
        Material.BOW, Material.CROSSBOW,
        Material.END_CRYSTAL,    // OBSIDIAN paired inline
        Material.RESPAWN_ANCHOR, // GLOWSTONE paired inline
        Material.COBWEB,
        Material.FIREWORK_ROCKET,
        Material.ENCHANTED_GOLDEN_APPLE, Material.GOLDEN_APPLE,
        Material.TOTEM_OF_UNDYING
    );

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
        for (int i = 0; i < 36; i++) {
            ItemStack it = inv.getItem(i);
            if (it != null && it.getType() == type) return i;
        }
        ItemStack off = inv.getItemInOffHand();
        if (off != null && off.getType() == type) return 40;
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

        bot.setItem(stored.clone(), EquipmentSlot.CHEST);
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

    // -------------------------------------------------------------------------
    // Inventory copy (used to propagate GUI edits to all same-name bots)
    // -------------------------------------------------------------------------

    /**
     * Copy this bot's current full inventory (post-autoEquip state) to
     * {@code target}, overwriting its hotbar, storage, armor, offhand, and
     * selected hotbar slot.
     */
    public void copyInventoryTo(Bot target) {
        PlayerInventory src = raw();
        net.minecraft.world.entity.player.Inventory nmsTarget = target.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack it = src.getItem(i);
            nmsTarget.setItem(i, (it == null || it.getType() == Material.AIR)
                    ? net.minecraft.world.item.ItemStack.EMPTY
                    : org.bukkit.craftbukkit.inventory.CraftItemStack.asNMSCopy(it));
        }
        nmsTarget.setChanged();

        ItemStack h   = src.getHelmet(),      ch  = src.getChestplate(),
                  l   = src.getLeggings(),    b2  = src.getBoots(),
                  off = src.getItemInOffHand();
        target.setItem(h   != null ? h   : new ItemStack(Material.AIR), EquipmentSlot.HEAD);
        target.setItem(ch  != null ? ch  : new ItemStack(Material.AIR), EquipmentSlot.CHEST);
        target.setItem(l   != null ? l   : new ItemStack(Material.AIR), EquipmentSlot.LEGS);
        target.setItem(b2  != null ? b2  : new ItemStack(Material.AIR), EquipmentSlot.FEET);
        target.setItemOffhand(off != null ? off : new ItemStack(Material.AIR));
        target.getBotInventory().setSelectedHotbarSlot(selectedHotbarSlot);
    }

    // -------------------------------------------------------------------------
    // Auto-equip
    // -------------------------------------------------------------------------

    /**
     * Examines every item in the bot's full inventory (hotbar, storage, current
     * armor, offhand) and reorganises them into the best combat configuration:
     * best armor → armor slots, weapons/tools → hotbar (by priority), leftover → storage.
     *
     * Called automatically when the inventory GUI closes.
     */
    public void autoEquip() {
        PlayerInventory inv = raw();

        // Collect the entire inventory into a mutable pool (cloned items).
        List<ItemStack> pool = new ArrayList<>();
        for (int i = 0; i < 36; i++) addToPool(pool, inv.getItem(i));
        addToPool(pool, inv.getHelmet());
        addToPool(pool, inv.getChestplate());
        addToPool(pool, inv.getLeggings());
        addToPool(pool, inv.getBoots());
        addToPool(pool, inv.getItemInOffHand());

        // Clear everything.
        clearAll();

        // Armor slots.
        pickAndEquip(pool, EquipmentSlot.HEAD,  "_HELMET");
        pickAndEquipChest(pool);
        pickAndEquip(pool, EquipmentSlot.LEGS,  "_LEGGINGS");
        pickAndEquip(pool, EquipmentSlot.FEET,  "_BOOTS");

        // Offhand: totem > shield.
        for (Material m : new Material[]{Material.TOTEM_OF_UNDYING, Material.SHIELD}) {
            ItemStack found = removeFirst(pool, m);
            if (found != null) { bot.setItemOffhand(found); break; }
        }

        // Hotbar: fill by priority order.
        int hotbarSlot = 0;
        for (Material wanted : HOTBAR_PRIORITY) {
            if (hotbarSlot >= HOTBAR_SIZE) break;
            ItemStack found = removeFirst(pool, wanted);
            if (found == null) continue;
            nmsSet(hotbarSlot++, found);
            // Crystal / anchor kits need their support block adjacent in hotbar.
            if (wanted == Material.END_CRYSTAL && hotbarSlot < HOTBAR_SIZE) {
                ItemStack obs = removeFirst(pool, Material.OBSIDIAN);
                if (obs != null) nmsSet(hotbarSlot++, obs);
            } else if (wanted == Material.RESPAWN_ANCHOR && hotbarSlot < HOTBAR_SIZE) {
                ItemStack glow = removeFirst(pool, Material.GLOWSTONE);
                if (glow != null) nmsSet(hotbarSlot++, glow);
            }
        }

        // Dump remainder into storage (slots 9–35).
        int storage = 9;
        for (ItemStack leftover : pool) {
            if (storage >= 36) break;
            nmsSet(storage++, leftover);
        }

        // Select the first weapon/melee slot, or slot 0.
        setSelectedHotbarSlot(findPrimaryWeaponSlot());
    }

    // ---- private helpers ----

    /** Write directly to the NMS Inventory, bypassing the Bukkit container transaction system. */
    private void nmsSet(int slot, ItemStack bukkit_item) {
        net.minecraft.world.entity.player.Inventory nmsInv = bot.getInventory();
        net.minecraft.world.item.ItemStack nms = (bukkit_item == null || bukkit_item.getType() == Material.AIR)
                ? net.minecraft.world.item.ItemStack.EMPTY
                : org.bukkit.craftbukkit.inventory.CraftItemStack.asNMSCopy(bukkit_item);
        nmsInv.setItem(slot, nms);
        nmsInv.setChanged();
    }

    /** Clear all 36 main slots via NMS, then clear armor/offhand via bot.setItem (sends packets). */
    private void clearAll() {
        net.minecraft.world.entity.player.Inventory nmsInv = bot.getInventory();
        for (int i = 0; i < 36; i++) nmsInv.setItem(i, net.minecraft.world.item.ItemStack.EMPTY);
        nmsInv.setChanged();
        bot.setItem(new ItemStack(Material.AIR), EquipmentSlot.HEAD);
        bot.setItem(new ItemStack(Material.AIR), EquipmentSlot.CHEST);
        bot.setItem(new ItemStack(Material.AIR), EquipmentSlot.LEGS);
        bot.setItem(new ItemStack(Material.AIR), EquipmentSlot.FEET);
        bot.setItemOffhand(new ItemStack(Material.AIR));
    }

    private void pickAndEquip(List<ItemStack> pool, EquipmentSlot slot, String suffix) {
        ItemStack best = null;
        int bestTier = -1;
        for (ItemStack it : pool) {
            String n = it.getType().name();
            boolean match = n.endsWith(suffix)
                    || (suffix.equals("_HELMET") && n.equals("TURTLE_HELMET"));
            if (match && armorTier(it.getType()) > bestTier) {
                best = it;
                bestTier = armorTier(it.getType());
            }
        }
        if (best != null) {
            pool.remove(best);
            bot.setItem(best, slot);
        }
    }

    /**
     * Chest slot: elytra if fireworks are in the pool (skydiver mode),
     * otherwise best chestplate, falling back to elytra if no chestplate.
     */
    private void pickAndEquipChest(List<ItemStack> pool) {
        boolean hasFireworks = pool.stream().anyMatch(it -> it.getType() == Material.FIREWORK_ROCKET);
        ItemStack elytra = findFirst(pool, Material.ELYTRA);
        ItemStack bestChest = null;
        int bestTier = -1;
        for (ItemStack it : pool) {
            if (it.getType().name().endsWith("_CHESTPLATE") && armorTier(it.getType()) > bestTier) {
                bestChest = it;
                bestTier = armorTier(it.getType());
            }
        }
        ItemStack toEquip = (hasFireworks && elytra != null) ? elytra
                : bestChest != null ? bestChest
                : elytra;
        if (toEquip != null) {
            pool.remove(toEquip);
            bot.setItem(toEquip, EquipmentSlot.CHEST);
        }
    }

    private int findPrimaryWeaponSlot() {
        PlayerInventory inv = raw();
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            ItemStack it = inv.getItem(i);
            if (it == null) continue;
            String n = it.getType().name();
            if (n.endsWith("_SWORD") || n.equals("MACE") || n.equals("TRIDENT") || n.endsWith("_AXE")) return i;
        }
        return 0;
    }

    private static int armorTier(Material m) {
        String n = m.name();
        if (n.startsWith("NETHERITE")) return 5;
        if (n.startsWith("DIAMOND"))   return 4;
        if (n.startsWith("IRON"))      return 3;
        if (n.startsWith("CHAINMAIL")) return 2;
        if (n.startsWith("GOLD"))      return 1;
        if (n.startsWith("LEATHER"))   return 1;
        return 0;
    }

    private static ItemStack removeFirst(List<ItemStack> pool, Material type) {
        for (int i = 0; i < pool.size(); i++) {
            if (pool.get(i).getType() == type) return pool.remove(i);
        }
        return null;
    }

    private static ItemStack findFirst(List<ItemStack> pool, Material type) {
        for (ItemStack it : pool) if (it.getType() == type) return it;
        return null;
    }

    private static void addToPool(List<ItemStack> pool, ItemStack it) {
        if (it != null && it.getType() != Material.AIR) pool.add(it.clone());
    }
}
