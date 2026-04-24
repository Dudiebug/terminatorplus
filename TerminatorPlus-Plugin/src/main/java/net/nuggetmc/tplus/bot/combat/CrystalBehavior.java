package net.nuggetmc.tplus.bot.combat;

import net.nuggetmc.tplus.bot.Bot;
import net.nuggetmc.tplus.bot.loadout.BotInventory;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.LivingEntity;

import java.util.Arrays;
import java.util.List;

/**
 * Crystal-PvP loop: place a host block (obsidian; glowstone in nether),
 * summon an end crystal on top, then damage it so its explosion lands
 * on the target. Short range, no-lapse cooldown.
 */
public final class CrystalBehavior implements WeaponBehavior {

    public static final String COOLDOWN_KEY = "crystal";
    private static final int COOLDOWN = 30;
    private static final double MAX_DISTANCE = 6.0;

    @Override
    public int ticksFor(Bot bot, LivingEntity target, double distance) {
        int alive = bot.getAliveTicks();
        if (distance > MAX_DISTANCE) {
            CombatDebugger.log(bot, "crystal-skip", "reason=range dist=" + String.format("%.2f", distance));
            return 0;
        }
        if (!bot.getBotCooldowns().ready(COOLDOWN_KEY, alive)) {
            CombatDebugger.log(bot, "crystal-skip", "reason=cooldown left=" + bot.getBotCooldowns().remaining(COOLDOWN_KEY, alive));
            return 0;
        }

        BotInventory inv = bot.getBotInventory();
        if (!inv.hasCrystalKit()) {
            CombatDebugger.log(bot, "crystal-skip", "reason=no-kit");
            return 0;
        }

        World world = target.getWorld();
        // First try to spawn on an existing host block next to the target.
        Block hostBlock = findHostBlockNear(target.getLocation());
        if (hostBlock == null) {
            hostBlock = placeHost(bot, target, world);
            if (hostBlock == null) {
                CombatDebugger.log(bot, "crystal-skip", "reason=no-host-block");
                return 0;
            }
        }

        Location spawn = hostBlock.getLocation().add(0.5, 1.0, 0.5);
        bot.faceLocation(spawn);
        bot.punch();

        EnderCrystal crystal = world.spawn(spawn, EnderCrystal.class, c -> c.setShowingBottom(false));
        // Hit the crystal immediately so the explosion lands where we just placed it.
        crystal.remove();
        world.createExplosion(spawn, 6.0f, false, true, bot.getBukkitEntity());
        world.playSound(spawn, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
        CombatDebugger.log(bot, "crystal-boom", "dist=" + String.format("%.2f", distance));

        consumeOne(inv, Material.END_CRYSTAL);
        bot.getBotCooldowns().set(COOLDOWN_KEY, COOLDOWN, alive);
        return COOLDOWN;
    }

    private Block findHostBlockNear(Location around) {
        World w = around.getWorld();
        int bx = around.getBlockX();
        int by = around.getBlockY();
        int bz = around.getBlockZ();
        List<int[]> offsets = Arrays.asList(
                new int[]{0, -1, 0},
                new int[]{1, -1, 0}, new int[]{-1, -1, 0},
                new int[]{0, -1, 1}, new int[]{0, -1, -1}
        );
        for (int[] o : offsets) {
            Block b = w.getBlockAt(bx + o[0], by + o[1], bz + o[2]);
            Material type = b.getType();
            Block above = b.getRelative(0, 1, 0);
            if (!above.getType().isAir()) continue;
            if (type == Material.OBSIDIAN || type == Material.BEDROCK || type == Material.GLOWSTONE || type == Material.CRYING_OBSIDIAN) {
                return b;
            }
        }
        return null;
    }

    private Block placeHost(Bot bot, LivingEntity target, World world) {
        BotInventory inv = bot.getBotInventory();
        Material hostMat;
        int hostSlot;
        if (world.getEnvironment() == World.Environment.NETHER && inv.findMainInventory(Material.GLOWSTONE) >= 0) {
            hostMat = Material.GLOWSTONE;
            hostSlot = inv.findMainInventory(Material.GLOWSTONE);
        } else {
            hostMat = Material.OBSIDIAN;
            hostSlot = inv.findMainInventory(Material.OBSIDIAN);
        }
        if (hostSlot < 0) return null;

        Location footLoc = target.getLocation().clone();
        Block candidate = footLoc.getBlock().getRelative(0, -1, 0);
        if (!candidate.getType().isAir()) {
            // The block under the target is already solid — place next to it.
            candidate = footLoc.getBlock();
            if (!candidate.getType().isAir()) return null;
        }
        candidate.setType(hostMat);
        consumeOne(inv, hostMat);
        return candidate;
    }

    private void consumeOne(BotInventory inv, Material type) {
        inv.decrementMaterial(type);
    }
}
