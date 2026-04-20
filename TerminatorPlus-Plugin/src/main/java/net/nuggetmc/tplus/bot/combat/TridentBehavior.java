package net.nuggetmc.tplus.bot.combat;

import net.nuggetmc.tplus.bot.Bot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Trident;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 * Momentum-building trident use. The bot sprints toward the target for
 * up to {@link #MAX_CHARGE_TICKS}, then releases a thrown trident whose
 * exit velocity is amplified by the momentum it built.
 */
public final class TridentBehavior implements WeaponBehavior {

    public static final String COOLDOWN_KEY = "trident";
    private static final int MAX_CHARGE_TICKS = 18;
    private static final int RELEASE_COOLDOWN = 45;
    private static final double MIN_DISTANCE = 5.0;
    private static final double MAX_DISTANCE = 30.0;
    private static final double THROW_BASE_SPEED = 2.5;
    private static final double THROW_MOMENTUM_BONUS = 1.4;

    @Override
    public int ticksFor(Bot bot, LivingEntity target, double distance) {
        if (!bot.getBotCooldowns().ready(COOLDOWN_KEY, bot.getAliveTicks())) return 0;
        if (distance < MIN_DISTANCE || distance > MAX_DISTANCE) return 0;

        CombatState state = bot.getCombatState();
        Location targetLoc = target.getLocation();
        Location botLoc = bot.getLocation();
        Vector toTarget = targetLoc.toVector().subtract(botLoc.toVector());
        toTarget.setY(0);
        if (toTarget.lengthSquared() < 1.0e-6) return 0;
        Vector dir = toTarget.normalize();

        bot.faceLocation(targetLoc);

        if (state.getPhase() != CombatState.Phase.CHARGING) {
            state.setPhase(CombatState.Phase.CHARGING);
            state.setChargeDirection(dir);
        }

        int charge = state.tickPhase();

        // Run up: accumulate horizontal velocity toward the target.
        if (bot.isBotOnGround()) {
            bot.walk(dir.clone().multiply(0.38));
        }

        boolean out = distance < MIN_DISTANCE + 1.0 || distance > MAX_DISTANCE - 2.0;
        if (charge >= MAX_CHARGE_TICKS || out) {
            release(bot, target, dir);
            state.reset();
            bot.getBotCooldowns().set(COOLDOWN_KEY, RELEASE_COOLDOWN, bot.getAliveTicks());
            return RELEASE_COOLDOWN;
        }

        return 0;
    }

    private void release(Bot bot, LivingEntity target, Vector runDir) {
        Location spawn = bot.getLocation().add(0, bot.getBukkitEntity().getEyeHeight() - 0.2, 0)
                .add(runDir.clone().multiply(0.6));
        Vector aim = target.getEyeLocation().toVector().subtract(spawn.toVector()).normalize();
        Vector momentum = bot.getVelocity();
        double momentumBoost = Math.min(THROW_MOMENTUM_BONUS, momentum.length() * 1.2);
        Vector velocity = aim.multiply(THROW_BASE_SPEED + momentumBoost);

        bot.punch();

        Trident trident = spawn.getWorld().spawn(spawn, Trident.class, t -> {
            t.setVelocity(velocity);
            t.setShooter(bot.getBukkitEntity());
            t.setItem(new ItemStack(Material.TRIDENT));
        });

        spawn.getWorld().playSound(spawn, Sound.ITEM_TRIDENT_THROW, 1f, 1f);
    }
}
