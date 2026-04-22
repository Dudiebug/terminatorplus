package net.nuggetmc.tplus.bot.loadout;

import net.nuggetmc.tplus.bot.Bot;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

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
     * Change the selected hotbar slot and send the mainhand-equipment
     * packet so viewers see the swap.
     *
     * <p>Must sync the NMS {@code Inventory.selected} field too. Our
     * own {@link #selectedHotbarSlot} tracker is a Bukkit-side mirror;
     * {@code bot.setItem(item, HAND)} underneath calls
     * {@code setItemInMainHand} which writes into whichever slot the
     * NMS inventory thinks is selected. Without the NMS sync that was
     * always slot 0 (the default), so every weapon swap silently
     * overwrote slot 0 with the newly-selected item — the mace-
     * disappearing bug.
     */
    public void setSelectedHotbarSlot(int slot) {
        if (slot < 0 || slot >= HOTBAR_SIZE) return;
        // No-op early return: CombatDirector / OpportunityScanner call this
        // every tick with the slot the bot is already holding. Without this
        // guard, the setItem(item.clone(), HAND) call below writes a brand
        // new ItemStack reference into the mainhand every tick. Vanilla
        // Player.tick's item-change detection (getMainHandItem() !=
        // lastItemInMainHand) fires on reference inequality and calls
        // resetAttackStrengthTicker() — pinning the attack charge at ~0.08
        // forever so canSwing's 0.95 gate never passes and the bot never
        // swings or crits. With the guard, same-slot calls are no-ops, the
        // ticker climbs unmolested, and the bot attacks at vanilla cadence.
        if (this.selectedHotbarSlot == slot) return;
        this.selectedHotbarSlot = slot;
        // Sync NMS (Bukkit API route, since the NMS field is private in
        // Paper 26.1.2). setItemInMainHand below writes into whichever
        // slot this tracker says is selected.
        raw().setHeldItemSlot(slot);
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

    /**
     * Find a slot anywhere in the main 36 inventory slots (hotbar + storage)
     * containing {@code type}. Prefers hotbar matches so the common case
     * (weapon already on the bar) is O(9). Returns -1 if not found.
     */
    public int findHotbar(Material type) {
        if (type == null) return -1;
        PlayerInventory inv = raw();
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            ItemStack it = inv.getItem(i);
            if (it != null && it.getType() == type) return i;
        }
        for (int i = HOTBAR_SIZE; i < 36; i++) {
            ItemStack it = inv.getItem(i);
            if (it != null && it.getType() == type) return i;
        }
        return -1;
    }

    /** Any slot in hotbar or storage contains a stack of {@code type}. */
    public boolean hasHotbar(Material type) {
        return findHotbar(type) >= 0;
    }

    /**
     * If {@code slot} is in storage (9–35), swap it with a hotbar slot so it
     * can be wielded. Skips hotbar slots that already hold a primary weapon
     * (sword/axe/mace) so we don't kick a real melee weapon out for a utility
     * item. Returns the new hotbar index (0–8), the original slot if already
     * on the hotbar, or -1 if no swap target was available.
     */
    public int bringToHotbar(int slot) {
        if (slot < 0 || slot >= 36) return -1;
        if (slot < HOTBAR_SIZE) return slot;
        PlayerInventory inv = raw();
        int target = -1;
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            ItemStack it = inv.getItem(i);
            if (it == null || it.getType() == Material.AIR) { target = i; break; }
        }
        if (target < 0) {
            // Hotbar full: pick the rightmost non-weapon slot to evict.
            for (int i = HOTBAR_SIZE - 1; i >= 0; i--) {
                ItemStack it = inv.getItem(i);
                if (it == null) continue;
                String n = it.getType().name();
                if (n.endsWith("_SWORD") || n.endsWith("_AXE") || it.getType() == Material.MACE) continue;
                target = i;
                break;
            }
        }
        if (target < 0) return -1;
        ItemStack moving = inv.getItem(slot);
        ItemStack evicted = inv.getItem(target);
        net.minecraft.world.entity.player.Inventory nms = bot.getInventory();
        nms.setItem(target, moving == null ? net.minecraft.world.item.ItemStack.EMPTY
                : org.bukkit.craftbukkit.inventory.CraftItemStack.asNMSCopy(moving));
        nms.setItem(slot, evicted == null ? net.minecraft.world.item.ItemStack.EMPTY
                : org.bukkit.craftbukkit.inventory.CraftItemStack.asNMSCopy(evicted));
        nms.setChanged();
        return target;
    }

    public boolean hasMace() {
        return hasHotbar(Material.MACE);
    }

    public boolean hasTrident() {
        return hasHotbar(Material.TRIDENT);
    }

    public boolean hasWindCharge() {
        if (hasHotbar(Material.WIND_CHARGE)) return true;
        // Mace loadouts stash wind charges in the offhand (see autoEquip
        // offhand priority), so the gates of WIND_MACE_SMASH, interrupt
        // plays, etc. would otherwise think the bot has none.
        ItemStack off = raw().getItemInOffHand();
        return off != null && off.getType() == Material.WIND_CHARGE;
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

    public boolean hasBow() {
        return hasHotbar(Material.BOW);
    }

    public boolean hasCrossbow() {
        return hasHotbar(Material.CROSSBOW);
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

    /** First slot (hotbar first, then storage) whose item is a sword, or -1. */
    public int findSword() {
        return findByNameSuffix("_SWORD");
    }

    public int findAxe() {
        return findByNameSuffix("_AXE");
    }

    private int findByNameSuffix(String suffix) {
        PlayerInventory inv = raw();
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            ItemStack it = inv.getItem(i);
            if (it != null && it.getType().name().endsWith(suffix)) return i;
        }
        for (int i = HOTBAR_SIZE; i < 36; i++) {
            ItemStack it = inv.getItem(i);
            if (it != null && it.getType().name().endsWith(suffix)) return i;
        }
        return -1;
    }

    /** Heuristic: first hotbar slot that heals (golden apple / potion of healing / totem / splash healing). */
    public int findHealing() {
        PlayerInventory inv = raw();
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            ItemStack it = inv.getItem(i);
            if (it == null) continue;
            Material m = it.getType();
            if (m == Material.GOLDEN_APPLE || m == Material.ENCHANTED_GOLDEN_APPLE
                    || m == Material.TOTEM_OF_UNDYING) {
                return i;
            }
            if (m == Material.POTION || m == Material.SPLASH_POTION) {
                if (isHealingPotion(it)) return i;
            }
        }
        return -1;
    }

    // -------------------------------------------------------------------------
    // Consumable / potion finders
    //
    // These scan only the hotbar (slots 0-8) so the ConsumableBehavior doesn't
    // spend cycles on storage. If a kit keeps consumables outside the hotbar,
    // the bot must auto-equip them first (see autoEquip).
    // -------------------------------------------------------------------------

    /** First hotbar slot matching {@code type} + (optionally) {@code potionTypes}. Returns -1 if none. */
    public int findHotbarPotion(Material container, PotionType... potionTypes) {
        if (container == null) return -1;
        PlayerInventory inv = raw();
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            ItemStack it = inv.getItem(i);
            if (it == null || it.getType() != container) continue;
            if (!(it.getItemMeta() instanceof PotionMeta pm)) continue;
            PotionType pt = pm.getBasePotionType();
            if (pt == null) continue;
            if (potionTypes == null || potionTypes.length == 0) return i;
            for (PotionType wanted : potionTypes) {
                if (pt == wanted) return i;
            }
        }
        return -1;
    }

    /** First hotbar drinkable potion with a healing base. */
    public int findHealingPotion() {
        return findHotbarPotion(Material.POTION, PotionType.HEALING, PotionType.STRONG_HEALING);
    }

    /** First hotbar splash potion of healing (for on-self self-heal). */
    public int findSplashHealing() {
        return findHotbarPotion(Material.SPLASH_POTION, PotionType.HEALING, PotionType.STRONG_HEALING);
    }

    /** First hotbar drinkable strength potion (for pre-combat buff-up). */
    public int findStrengthPotion() {
        return findHotbarPotion(Material.POTION, PotionType.STRENGTH, PotionType.STRONG_STRENGTH);
    }

    /** First hotbar drinkable fire-resistance potion. */
    public int findFireResPotion() {
        return findHotbarPotion(Material.POTION, PotionType.FIRE_RESISTANCE, PotionType.LONG_FIRE_RESISTANCE);
    }

    /** First hotbar offensive splash (harming / poison / slowness). Used against enemies. */
    public int findSplashHarming() {
        return findHotbarPotion(Material.SPLASH_POTION,
                PotionType.HARMING, PotionType.STRONG_HARMING,
                PotionType.POISON, PotionType.STRONG_POISON, PotionType.LONG_POISON,
                PotionType.SLOWNESS, PotionType.STRONG_SLOWNESS, PotionType.LONG_SLOWNESS);
    }

    /** Cheap pre-check: does the bot have ANY consumable (apple / potion / splash) on its hotbar? */
    public boolean hasAnyConsumable() {
        PlayerInventory inv = raw();
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            ItemStack it = inv.getItem(i);
            if (it == null) continue;
            Material m = it.getType();
            if (m == Material.GOLDEN_APPLE || m == Material.ENCHANTED_GOLDEN_APPLE) return true;
            if (m == Material.POTION || m == Material.SPLASH_POTION || m == Material.LINGERING_POTION) return true;
        }
        return false;
    }

    /** True if this stack is a drinkable or splash potion whose base type heals. */
    private static boolean isHealingPotion(ItemStack it) {
        if (!(it.getItemMeta() instanceof PotionMeta pm)) return false;
        PotionType pt = pm.getBasePotionType();
        return pt == PotionType.HEALING || pt == PotionType.STRONG_HEALING;
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

        // Offhand priority:
        //  1. Wind charges, IF the bot has a mace too — wiki Mace-PvP kit
        //     pairs the two so the bot can throw a wind charge from offhand
        //     for a launch without swapping main-hand off the mace (which
        //     would reset the attack-strength ticker and downgrade the
        //     smash from a crit to a normal swing).
        //  2. Totem of undying (clutch hit savior).
        //  3. Shield.
        boolean hasMaceInPool = false;
        for (ItemStack p : pool) {
            if (p != null && p.getType() == Material.MACE) { hasMaceInPool = true; break; }
        }
        boolean offhandSet = false;
        if (hasMaceInPool) {
            ItemStack wind = removeFirst(pool, Material.WIND_CHARGE);
            if (wind != null) {
                if (wind.getAmount() < 16) wind.setAmount(16);
                bot.setItemOffhand(wind);
                offhandSet = true;
            }
        }
        if (!offhandSet) {
            for (Material m : new Material[]{Material.TOTEM_OF_UNDYING, Material.SHIELD}) {
                ItemStack found = removeFirst(pool, m);
                if (found != null) { bot.setItemOffhand(found); break; }
            }
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

    /** Numeric armor/tool tier from a material name. 5=NETHERITE, 4=DIAMOND, 3=IRON, 2=CHAINMAIL, 1=GOLD/LEATHER, 0=none. */
    public static int armorTier(Material m) {
        if (m == null) return 0;
        String n = m.name();
        if (n.startsWith("NETHERITE")) return 5;
        if (n.startsWith("DIAMOND"))   return 4;
        if (n.startsWith("IRON"))      return 3;
        if (n.startsWith("CHAINMAIL")) return 2;
        if (n.startsWith("GOLD"))      return 1;
        if (n.startsWith("LEATHER"))   return 1;
        return 0;
    }

    /** Highest tier across the four equipped armor pieces. 0 if no armor is worn. */
    public int getEquippedArmorTier() {
        PlayerInventory inv = raw();
        int best = 0;
        ItemStack[] pieces = { inv.getHelmet(), inv.getChestplate(), inv.getLeggings(), inv.getBoots() };
        for (ItemStack p : pieces) {
            if (p == null || p.getType() == Material.AIR) continue;
            int t = armorTier(p.getType());
            if (t > best) best = t;
        }
        return best;
    }

    /**
     * Guarantee the bot has at least one ender pearl and one wind charge in its hotbar,
     * with the stack topped up to {@link #MOVEMENT_KIT_STACK}. If there are no free hotbar
     * slots, packs them into storage instead so they can be auto-equipped on the next pass.
     *
     * <p>Bots get these unconditionally — they're treated as part of every bot's baseline kit
     * rather than something a preset must opt into.
     */
    public void ensureMovementKit() {
        ensureStocked(Material.ENDER_PEARL, MOVEMENT_KIT_STACK);
        ensureStocked(Material.WIND_CHARGE, MOVEMENT_KIT_STACK);
    }

    public static final int MOVEMENT_KIT_STACK = 16;

    private void ensureStocked(Material type, int target) {
        PlayerInventory inv = raw();
        // Check offhand first — wind charges paired with a mace live there
        // (see autoEquip's offhand priority). If we skipped it we'd duplicate
        // the stack onto the hotbar every 40 ticks.
        ItemStack off = inv.getItemInOffHand();
        if (off != null && off.getType() == type) {
            if (off.getAmount() < target) {
                off.setAmount(target);
                bot.setItemOffhand(off);
            }
            return;
        }
        int slot = findHotbar(type);
        if (slot >= 0) {
            ItemStack held = inv.getItem(slot);
            if (held != null && held.getAmount() < target) {
                held.setAmount(target);
                inv.setItem(slot, held);
            }
            return;
        }
        // No hotbar copy — find an empty hotbar slot first.
        int free = -1;
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            ItemStack it = inv.getItem(i);
            if (it == null || it.getType() == Material.AIR) { free = i; break; }
        }
        if (free < 0) {
            // Hotbar full: park it in storage so a later autoEquip can promote it.
            for (int i = 9; i < 36; i++) {
                ItemStack it = inv.getItem(i);
                if (it == null || it.getType() == Material.AIR) { free = i; break; }
            }
        }
        if (free < 0) return;
        ItemStack stack = new ItemStack(type, target);
        inv.setItem(free, stack);
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
