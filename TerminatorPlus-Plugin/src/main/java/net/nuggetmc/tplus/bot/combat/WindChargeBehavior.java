package net.nuggetmc.tplus.bot.combat;

import net.nuggetmc.tplus.bot.Bot;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.WindCharge;
import org.bukkit.util.Vector;

/**
 * Wind charges have two roles for bots:
 * <ul>
 *   <li><b>Combat zoning</b> ({@link #ticksFor}): lobbed at the target's eye for
 *       knockback when no trident is available.</li>
 *   <li><b>Self-propulsion</b> ({@link #tickMovementBoost}): fired down-and-behind
 *       the bot's feet, so the explosion launches the bot toward its target. Runs
 *       as a passive every tick alongside elytra/totem checks.</li>
 * </ul>
 * Wind charges are part of the bot's automatic movement kit, so neither path
 * decrements inventory.
 */
public final class WindChargeBehavior implements WeaponBehavior {

    public static final String COOLDOWN_KEY = "windcharge";
    public static final String BOOST_COOLDOWN_KEY = "windcharge_boost";
    private static final int COOLDOWN = 55;
    private static final int BOOST_COOLDOWN = 30;
    private static final double MIN_DISTANCE = 4.0;
    private static final double MAX_DISTANCE = 30.0;
    /** Inside this radius the bot is already in (or near) melee — boosting forward would overshoot. */
    private static final double BOOST_MIN_DISTANCE = 8.0;
    private static final double SPEED = 1.6;

    @Override
    public int ticksFor(Bot bot, LivingEntity target, double distance) {
        if (distance < MIN_DISTANCE || distance > MAX_DISTANCE) return 0;
        if (!bot.getBotCooldowns().ready(COOLDOWN_KEY, bot.getAliveTicks())) return 0;

        Location spawn = bot.getLocation().add(0, bot.getBukkitEntity().getEyeHeight() - 0.1, 0);
        Vector aim = target.getEyeLocation().toVector().subtract(spawn.toVector()).normalize();

        bot.faceLocation(target.getLocation());
        bot.punch();

        spawn.getWorld().spawn(spawn, WindCharge.class, w -> {
            w.setVelocity(aim.multiply(SPEED));
            w.setShooter(bot.getBukkitEntity());
        });

        spawn.getWorld().playSound(spawn, Sound.ENTITY_WIND_CHARGE_THROW, 1f, 1f);
        bot.getBotCooldowns().set(COOLDOWN_KEY, COOLDOWN, bot.getAliveTicks());
        return COOLDOWN;
    }

    /**
     * Self-boost: spawn a wind charge just behind and below the bot's feet, aimed
     * at the ground. The vanilla AoE pulse launches the bot toward its target.
     * Returns true if a charge was fired this tick.
     */
    public boolean tickMovementBoost(Bot bot, LivingEntity target, double distance) {
        if (distance < BOOST_MIN_DISTANCE) return false;
        if (!bot.isBotOnGround()) return false;
        if (!bot.getBotInventory().hasWindCharge()) return false;
        if (!bot.getBotCooldowns().ready(BOOST_COOLDOWN_KEY, bot.getAliveTicks())) return false;

        Vector toTarget = target.getLocation().toVector().subtract(bot.getLocation().toVector());
        toTarget.setY(0);
        if (toTarget.lengthSquared() < 0.01) return false;
        toTarget.normalize();

        // Spawn 0.6 blocks behind and 0.4 up so the entity isn't inside the bot's hitbox.
        Vector backward = toTarget.clone().multiply(-0.6);
        Location spawn = bot.getLocation().add(backward).add(0, 0.4, 0);

        // Aim down-and-back so the explosion happens behind the bot's feet.
        Vector aim = backward.clone().normalize().multiply(0.5).setY(-0.5);

        spawn.getWorld().spawn(spawn, WindCharge.class, w -> {
            w.setShooter(bot.getBukkitEntity());
            w.setVelocity(aim);
        });
        spawn.getWorld().playSound(spawn, Sound.ENTITY_WIND_CHARGE_THROW, 1f, 1f);

        bot.getBotCooldowns().set(BOOST_COOLDOWN_KEY, BOOST_COOLDOWN, bot.getAliveTicks());
        return true;
    }
}
