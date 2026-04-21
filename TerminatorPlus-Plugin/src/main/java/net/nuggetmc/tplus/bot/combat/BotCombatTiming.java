package net.nuggetmc.tplus.bot.combat;

import net.nuggetmc.tplus.bot.Bot;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.entity.LivingEntity;

/**
 * Zero-allocation helpers that gate bot swings against:
 * <ol>
 *   <li>the attacker's vanilla attack-strength charge (so we never swing at 25% damage
 *       like the old 3-tick melee loop did), and</li>
 *   <li>the target's invulnerability window (the i-frame dead zone where extra hits
 *       are either ignored or reduced to a delta).</li>
 * </ol>
 *
 * <p>Both methods are pure field reads + arithmetic, safe to call every tick.
 *
 * <p>Constants follow vanilla:
 * <ul>
 *   <li>{@link #READY_CHARGE} = 0.95 — charge >= 0.95 produces full damage (0.9025+ mult)
 *       and passes vanilla's 0.848 crit/sweep/sprint-KB gate.</li>
 *   <li>{@link #SMASH_READY_CHARGE} = 0.848 — matches vanilla's crit threshold; the mace
 *       smash density bonus dominates base damage so we accept anything above this.</li>
 *   <li>{@link #IFRAME_BLOCK_THRESHOLD} = 10 — matches vanilla LivingEntity.hurt's
 *       {@code invulnerableTime > 10.0F} branch. Hits in the lower half of the window
 *       still land for full damage.</li>
 * </ul>
 */
public final class BotCombatTiming {

    public static final float READY_CHARGE = 0.95f;
    public static final float SMASH_READY_CHARGE = 0.848f;
    public static final int IFRAME_BLOCK_THRESHOLD = 10;

    private BotCombatTiming() {}

    /** True when the bot is charged AND the target can take full damage right now. */
    public static boolean canSwing(Bot bot, LivingEntity target) {
        return canSwing(bot, target, READY_CHARGE);
    }

    public static boolean canSwing(Bot bot, LivingEntity target, float minCharge) {
        if (bot.getAttackStrengthScale(0.0f) < minCharge) return false;
        return !targetHasIFrames(target);
    }

    /** Attack-strength charge scaled to [0, 1]; 1.0 means fully recharged. */
    public static boolean chargeReady(Bot bot) {
        return bot.getAttackStrengthScale(0.0f) >= READY_CHARGE;
    }

    /** More permissive gate for mace dive-impacts where the smash density bonus dominates. */
    public static boolean smashChargeReady(Bot bot) {
        return bot.getAttackStrengthScale(0.0f) >= SMASH_READY_CHARGE;
    }

    /** True if hitting the target now would be wasted on its i-frame window. */
    public static boolean targetHasIFrames(LivingEntity target) {
        return ((CraftLivingEntity) target).getHandle().invulnerableTime > IFRAME_BLOCK_THRESHOLD;
    }
}
