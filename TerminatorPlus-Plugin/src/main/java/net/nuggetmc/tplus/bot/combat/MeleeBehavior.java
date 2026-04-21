package net.nuggetmc.tplus.bot.combat;

import net.nuggetmc.tplus.bot.Bot;
import org.bukkit.entity.LivingEntity;

/**
 * Cooldown-aware melee. Swings only when:
 * <ul>
 *   <li>attack-strength charge &gt;= 0.95 (full-damage + crit/sweep eligible), and</li>
 *   <li>the target isn't deep in an i-frame window that would swallow the hit.</li>
 * </ul>
 *
 * <p>Replaces the old 3-tick fixed cadence which produced ~25% weapon damage every swing
 * and never met vanilla's 0.848 crit/sweep/sprint-KB threshold.
 *
 * <p>Only reached via {@link CombatDirector#tick}, which short-circuits for neural-network
 * training bots — training still uses the deterministic damage table.
 */
public final class MeleeBehavior implements WeaponBehavior {

    private static final double ATTACK_RANGE = 4.0;

    @Override
    public int ticksFor(Bot bot, LivingEntity target, double distance) {
        if (distance > ATTACK_RANGE) return 0;
        if (!BotCombatTiming.canSwing(bot, target)) return 0;
        bot.attack(target);
        return 0;
    }
}
