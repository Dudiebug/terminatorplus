package net.nuggetmc.tplus.bot.combat;

import net.nuggetmc.tplus.bot.Bot;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.WindCharge;
import org.bukkit.util.Vector;

/**
 * Cooldown-gated wind charge lobbing. Used as the bot's long-range
 * zoning / knockback tool when the target is out of melee and no trident
 * is available.
 */
public final class WindChargeBehavior implements WeaponBehavior {

    public static final String COOLDOWN_KEY = "windcharge";
    private static final int COOLDOWN = 55;
    private static final double MIN_DISTANCE = 4.0;
    private static final double MAX_DISTANCE = 30.0;
    private static final double SPEED = 1.6;

    @Override
    public int ticksFor(Bot bot, LivingEntity target, double distance) {
        if (distance < MIN_DISTANCE || distance > MAX_DISTANCE) return 0;
        if (!bot.getCooldowns().ready(COOLDOWN_KEY, bot.getAliveTicks())) return 0;

        Location spawn = bot.getLocation().add(0, bot.getBukkitEntity().getEyeHeight() - 0.1, 0);
        Vector aim = target.getEyeLocation().toVector().subtract(spawn.toVector()).normalize();

        bot.faceLocation(target.getLocation());
        bot.punch();

        spawn.getWorld().spawn(spawn, WindCharge.class, w -> {
            w.setVelocity(aim.multiply(SPEED));
            w.setShooter(bot.getBukkitEntity());
        });

        spawn.getWorld().playSound(spawn, Sound.ENTITY_WIND_CHARGE_THROW, 1f, 1f);
        bot.getCooldowns().set(COOLDOWN_KEY, COOLDOWN, bot.getAliveTicks());
        return COOLDOWN;
    }
}
