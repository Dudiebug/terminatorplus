package net.nuggetmc.tplus.bot.combat;

import net.nuggetmc.tplus.bot.Bot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.WindCharge;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

/**
 * Wind charges have two roles:
 *   <ul>
 *     <li><b>Combat zoning</b> — lob at the target's eyes for knockback. Used by
 *         {@link #ticksFor} when the bot is close enough to be in a fight.</li>
 *     <li><b>Movement boost</b> — throw straight down at the bot's feet; the
 *         explosion's wind burst rockets the bot upward, and its existing
 *         horizontal velocity (set by the agent's pathing toward the target)
 *         carries it forward. Combines with elytra for sustained traversal.</li>
 *   </ul>
 */
public final class WindChargeBehavior implements WeaponBehavior {

    public static final String COOLDOWN_KEY = "windcharge";
    /** Shorter than the combat-throw cooldown — movement abilities should feel responsive. */
    public static final String BOOST_COOLDOWN_KEY = "windcharge_boost";
    private static final int COOLDOWN = 55;
    private static final int BOOST_COOLDOWN = 35;
    private static final double MIN_DISTANCE = 4.0;
    private static final double MAX_DISTANCE = 30.0;
    private static final double SPEED = 1.6;
    private static final double BOOST_SPEED = 2.5;

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
        consumeOne(bot);
        return COOLDOWN;
    }

    /**
     * Throws a wind charge straight down at the bot's feet so the explosion launches it
     * upward. Caller is responsible for upstream gating (grounded, has wind charge in
     * hotbar, target is far enough that traversal is wanted, both cooldowns ready).
     *
     * @return cooldown ticks consumed (0 if no throw fired)
     */
    public int boostTowards(Bot bot, LivingEntity target) {
        if (!bot.getBotCooldowns().ready(BOOST_COOLDOWN_KEY, bot.getAliveTicks())) return 0;

        bot.faceLocation(target.getLocation());

        Location feet = bot.getLocation().add(0, 0.2, 0);
        Vector aim = new Vector(0, -1, 0);

        feet.getWorld().spawn(feet, WindCharge.class, w -> {
            w.setVelocity(aim.clone().multiply(BOOST_SPEED));
            w.setShooter(bot.getBukkitEntity());
        });

        feet.getWorld().playSound(feet, Sound.ENTITY_WIND_CHARGE_THROW, 1f, 1.2f);
        bot.getBotCooldowns().set(BOOST_COOLDOWN_KEY, BOOST_COOLDOWN, bot.getAliveTicks());
        // Also park the combat-throw on a brief cooldown so we don't double-fire this tick.
        bot.getBotCooldowns().set(COOLDOWN_KEY, 10, bot.getAliveTicks());
        consumeOne(bot);
        return BOOST_COOLDOWN;
    }

    private static void consumeOne(Bot bot) {
        PlayerInventory inv = bot.getBotInventory().raw();
        int slot = bot.getBotInventory().findHotbar(Material.WIND_CHARGE);
        if (slot < 0) return;
        ItemStack held = inv.getItem(slot);
        if (held == null) return;
        int amt = held.getAmount();
        if (amt <= 1) inv.setItem(slot, new ItemStack(Material.AIR));
        else {
            held.setAmount(amt - 1);
            inv.setItem(slot, held);
        }
    }
}
