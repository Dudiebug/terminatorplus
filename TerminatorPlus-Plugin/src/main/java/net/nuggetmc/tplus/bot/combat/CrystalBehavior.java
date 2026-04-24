package net.nuggetmc.tplus.bot.combat;

import net.nuggetmc.tplus.bot.Bot;
import net.nuggetmc.tplus.bot.loadout.BotInventory;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.List;

/**
 * Crystal-PvP loop: place a host block, summon an end crystal on top, then
 * detonate it. The behavior is deliberately conservative so the bot does not
 * spend resources or mutate terrain unless the final explosion is survivable.
 */
public final class CrystalBehavior implements WeaponBehavior {

    public static final String COOLDOWN_KEY = "crystal";
    public static final double MIN_DISTANCE = 4.5;
    public static final double MAX_DISTANCE = 7.0;
    private static final int COOLDOWN = 30;

    @Override
    public int ticksFor(Bot bot, LivingEntity target, double distance) {
        int alive = bot.getAliveTicks();
        if (distance < MIN_DISTANCE || distance > MAX_DISTANCE) {
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
        HostPlan host = findHostPlan(bot, target, world);
        if (host == null) {
            CombatDebugger.log(bot, "crystal-skip", "reason=no-host-block");
            return 0;
        }

        Location spawn = host.block().getLocation().add(0.5, 1.0, 0.5);
        double blastDistance = bot.getLocation().distance(spawn);
        if (blastDistance < MIN_DISTANCE) {
            CombatDebugger.log(bot, "crystal-skip", "reason=unsafe-blast dist=" + String.format("%.2f", blastDistance));
            return 0;
        }
        if (!hasClearLine(bot, spawn)) {
            CombatDebugger.log(bot, "crystal-skip", "reason=blocked-line");
            return 0;
        }

        if (host.placedMaterial() != null) {
            host.block().setType(host.placedMaterial());
            consumeOne(inv, host.placedMaterial());
        }

        bot.faceLocation(spawn);
        bot.punch();

        EnderCrystal crystal = world.spawn(spawn, EnderCrystal.class, c -> c.setShowingBottom(false));
        crystal.remove();
        world.createExplosion(spawn, 6.0f, false, true, bot.getBukkitEntity());
        world.playSound(spawn, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
        CombatDebugger.log(bot, "crystal-boom", "dist=" + String.format("%.2f", distance));

        consumeOne(inv, Material.END_CRYSTAL);
        bot.getBotCooldowns().set(COOLDOWN_KEY, COOLDOWN, alive);
        return COOLDOWN;
    }

    private HostPlan findHostPlan(Bot bot, LivingEntity target, World world) {
        Block existing = findHostBlockNear(target.getLocation());
        if (existing != null) return new HostPlan(existing, null);

        BotInventory inv = bot.getBotInventory();
        Material hostMat;
        if (world.getEnvironment() == World.Environment.NETHER && inv.findMainInventory(Material.GLOWSTONE) >= 0) {
            hostMat = Material.GLOWSTONE;
        } else if (inv.findMainInventory(Material.OBSIDIAN) >= 0) {
            hostMat = Material.OBSIDIAN;
        } else {
            return null;
        }

        Location footLoc = target.getLocation().clone();
        Block candidate = footLoc.getBlock().getRelative(0, -1, 0);
        if (!candidate.getType().isAir()) {
            candidate = footLoc.getBlock();
            if (!candidate.getType().isAir()) return null;
        }
        if (!canSpawnCrystalAbove(candidate)) return null;
        return new HostPlan(candidate, hostMat);
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
            if (!canSpawnCrystalAbove(b)) continue;
            if (type == Material.OBSIDIAN || type == Material.BEDROCK
                    || type == Material.GLOWSTONE || type == Material.CRYING_OBSIDIAN) {
                return b;
            }
        }
        return null;
    }

    private boolean canSpawnCrystalAbove(Block host) {
        return host.getRelative(0, 1, 0).getType().isAir()
                && host.getRelative(0, 2, 0).getType().isAir();
    }

    private boolean hasClearLine(Bot bot, Location spawn) {
        Location eye = bot.getBukkitEntity().getEyeLocation();
        Vector direction = spawn.toVector().subtract(eye.toVector());
        double length = direction.length();
        if (length < 1.0e-6) return true;
        direction.normalize();
        return eye.getWorld().rayTraceBlocks(eye, direction, length, FluidCollisionMode.NEVER, true) == null;
    }

    private void consumeOne(BotInventory inv, Material type) {
        inv.decrementMaterial(type);
    }

    private record HostPlan(Block block, Material placedMaterial) {}
}
