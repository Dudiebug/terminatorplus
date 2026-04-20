package net.nuggetmc.tplus.bot.combat;

import net.nuggetmc.tplus.bot.Bot;
import net.nuggetmc.tplus.bot.loadout.BotInventory;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Nether-only bomb: place a respawn anchor next to the target, charge it
 * with glowstone, then detonate it. Short range — the blast is large, so
 * the director only runs this when the bot has space behind it to retreat
 * (approximated by trusting the cooldown).
 */
public final class AnchorBombBehavior implements WeaponBehavior {

    public static final String COOLDOWN_KEY = "anchor";
    private static final int COOLDOWN = 50;
    private static final double MAX_DISTANCE = 5.0;

    @Override
    public int ticksFor(Bot bot, LivingEntity target, double distance) {
        if (target.getWorld().getEnvironment() != World.Environment.NETHER) return 0;
        if (distance > MAX_DISTANCE) return 0;
        if (!bot.getBotCooldowns().ready(COOLDOWN_KEY, bot.getAliveTicks())) return 0;

        BotInventory inv = bot.getBotInventory();
        if (!inv.hasAnchorKit()) return 0;

        World world = target.getWorld();
        Block placeAt = findPlaceableBlock(target.getLocation());
        if (placeAt == null) return 0;

        placeAt.setType(Material.RESPAWN_ANCHOR);
        consumeOne(inv.raw(), Material.RESPAWN_ANCHOR);
        // Consume glowstone to represent the "charge" step.
        consumeOne(inv.raw(), Material.GLOWSTONE);

        Location boom = placeAt.getLocation().add(0.5, 0.5, 0.5);
        bot.faceLocation(boom);
        bot.punch();

        // Replace with air before detonating so the block-form anchor doesn't absorb the blast.
        placeAt.setType(Material.AIR);
        world.createExplosion(boom, 5.0f, false, true, bot.getBukkitEntity());
        world.playSound(boom, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1f, 0.7f);

        bot.getBotCooldowns().set(COOLDOWN_KEY, COOLDOWN, bot.getAliveTicks());
        return COOLDOWN;
    }

    private Block findPlaceableBlock(Location around) {
        World w = around.getWorld();
        int bx = around.getBlockX();
        int by = around.getBlockY();
        int bz = around.getBlockZ();
        // Prefer the block the target is standing on (replacement) or the adjacent air block.
        Block onFeet = w.getBlockAt(bx, by, bz);
        if (onFeet.getType().isAir()) return onFeet;
        for (int[] o : new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}}) {
            Block b = w.getBlockAt(bx + o[0], by + o[1], bz + o[2]);
            if (b.getType().isAir()) return b;
        }
        return null;
    }

    private void consumeOne(PlayerInventory inv, Material type) {
        for (int i = 0; i < 36; i++) {
            ItemStack it = inv.getItem(i);
            if (it != null && it.getType() == type) {
                int amt = it.getAmount();
                if (amt <= 1) inv.setItem(i, new ItemStack(Material.AIR));
                else {
                    it.setAmount(amt - 1);
                    inv.setItem(i, it);
                }
                return;
            }
        }
    }
}
