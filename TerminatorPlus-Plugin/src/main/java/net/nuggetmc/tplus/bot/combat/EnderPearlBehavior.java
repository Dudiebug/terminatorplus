package net.nuggetmc.tplus.bot.combat;

import net.nuggetmc.tplus.bot.Bot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

/**
 * Pearl as both gap-closer and long-range movement tool. Vanilla pearl landing
 * teleports the bot. At {@code distance > 28} the bot prioritizes pearling over
 * walking, with a higher arc + faster pearl so it actually carries that far.
 */
public final class EnderPearlBehavior implements WeaponBehavior {

    public static final String COOLDOWN_KEY = "pearl";
    private static final int COOLDOWN = 80;
    private static final double MIN_DISTANCE = 14.0;
    /** Movement mode kicks in beyond this — uses faster pearl + steeper arc. */
    private static final double MOVEMENT_DISTANCE = 28.0;
    private static final double MAX_DISTANCE = 80.0;
    private static final double SPEED = 1.8;
    private static final double MOVEMENT_SPEED = 2.6;

    @Override
    public int ticksFor(Bot bot, LivingEntity target, double distance) {
        if (distance < MIN_DISTANCE || distance > MAX_DISTANCE) return 0;
        if (!bot.getBotCooldowns().ready(COOLDOWN_KEY, bot.getAliveTicks())) return 0;

        int slot = bot.getBotInventory().findHotbar(Material.ENDER_PEARL);
        if (slot < 0) return 0;

        bot.selectHotbarSlot(slot);
        bot.faceLocation(target.getLocation());
        bot.punch();

        boolean movementMode = distance > MOVEMENT_DISTANCE;
        double speed = movementMode ? MOVEMENT_SPEED : SPEED;

        Location spawn = bot.getLocation().add(0, bot.getBukkitEntity().getEyeHeight() - 0.1, 0);
        // Lead the target slightly based on their horizontal velocity.
        Vector targetVel = target.getVelocity();
        Vector aimPoint = target.getLocation().toVector()
                .add(targetVel.clone().multiply(distance / 12.0))
                .add(new Vector(0, target.getHeight() * 0.5, 0));
        Vector aim = aimPoint.subtract(spawn.toVector()).normalize();
        // Arc upward; steeper at long range so gravity still drops the pearl onto the target.
        double arc = movementMode ? Math.min(0.45, 0.12 + (distance - MOVEMENT_DISTANCE) * 0.012) : 0.12;
        aim.setY(aim.getY() + arc).normalize();

        spawn.getWorld().spawn(spawn, EnderPearl.class, p -> {
            p.setShooter(bot.getBukkitEntity());
            p.setVelocity(aim.multiply(speed));
        });

        spawn.getWorld().playSound(spawn, Sound.ENTITY_ENDER_PEARL_THROW, 1f, 1f);

        // Consume one pearl so the stack shrinks like a real player's.
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
