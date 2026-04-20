package net.nuggetmc.tplus.bot.combat;

import net.nuggetmc.tplus.bot.Bot;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

/**
 * Low-priority anti-mobility: if the target is moving away from the bot
 * at a good clip, drop a cobweb at their feet to slow them down.
 */
public final class UtilityBehavior implements WeaponBehavior {

    public static final String COOLDOWN_KEY = "cobweb";
    private static final int COOLDOWN = 30;
    private static final double MAX_DISTANCE = 4.5;
    private static final double FLEE_VELOCITY = 0.25;

    @Override
    public int ticksFor(Bot bot, LivingEntity target, double distance) {
        if (distance > MAX_DISTANCE) return 0;
        if (!bot.getBotCooldowns().ready(COOLDOWN_KEY, bot.getAliveTicks())) return 0;
        if (!bot.getBotInventory().hasCobweb()) return 0;

        // Only drop a cobweb if the target is fleeing *away* from us.
        Vector toTarget = target.getLocation().toVector().subtract(bot.getLocation().toVector()).setY(0);
        if (toTarget.lengthSquared() < 1.0e-6) return 0;
        toTarget.normalize();
        Vector vel = target.getVelocity().clone().setY(0);
        double fleeDot = vel.dot(toTarget);
        if (fleeDot < FLEE_VELOCITY) return 0;

        int slot = bot.getBotInventory().findHotbar(Material.COBWEB);
        if (slot < 0) return 0;
        bot.selectHotbarSlot(slot);
        bot.faceLocation(target.getLocation());
        bot.punch();

        Block block = target.getLocation().getBlock();
        if (!block.getType().isAir()) {
            block = block.getRelative(0, 1, 0);
        }
        if (!block.getType().isAir()) return 0;
        block.setType(Material.COBWEB);

        PlayerInventory inv = bot.getBotInventory().raw();
        ItemStack held = inv.getItem(slot);
        if (held != null) {
            int amt = held.getAmount();
            if (amt <= 1) inv.setItem(slot, new ItemStack(Material.AIR));
            else {
                held.setAmount(amt - 1);
                inv.setItem(slot, held);
            }
        }

        bot.getBotCooldowns().set(COOLDOWN_KEY, COOLDOWN, bot.getAliveTicks());
        return COOLDOWN;
    }
}
