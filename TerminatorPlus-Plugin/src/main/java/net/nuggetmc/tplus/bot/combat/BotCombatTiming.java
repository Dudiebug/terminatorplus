package net.nuggetmc.tplus.bot.combat;

import net.nuggetmc.tplus.bot.Bot;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.entity.LivingEntity;

/**
 * Zero-allocation gates for bot melee swings:
 *   (1) attacker's vanilla attack-strength charge
 *   (2) target's invulnerability window
 *
 * Vanilla {@code Player.getAttackStrengthScale(float)} returns the fraction of
 * the current weapon's full charge (clamped 0..1). At {@code >= 0.848} the hit
 * gains crit / sweep / sprint-knockback eligibility; at {@code >= 0.95} the
 * damage multiplier is essentially full ({@code >= 0.92}).
 */
public final class BotCombatTiming {

    /** Charge fraction that produces a "full" hit (damage mult >= 0.92, crit eligible). */
    public static final float READY_CHARGE = 0.95f;

    /** Mace smash gate: density bonus dominates so 0.848 (crit threshold) is enough. */
    public static final float SMASH_READY_CHARGE = 0.848f;

    /**
     * Vanilla {@code LivingEntity.hurt} bypasses the new hit when
     * {@code invulnerableTime > 10}. Above this we either get nothing or
     * just the damage delta — wasteful for a bot's homogeneous swings.
     */
    public static final int IFRAME_BLOCK_THRESHOLD = 10;

    private BotCombatTiming() {}

    public static boolean canSwing(Bot bot, LivingEntity target) {
        return canSwing(bot, target, READY_CHARGE);
    }

    public static boolean canSwing(Bot bot, LivingEntity target, float minCharge) {
        if (bot.getAttackStrengthScale(0.0f) < minCharge) return false;
        return !targetHasIFrames(target);
    }

    public static boolean chargeReady(Bot bot) {
        return bot.getAttackStrengthScale(0.0f) >= READY_CHARGE;
    }

    public static boolean smashChargeReady(Bot bot) {
        return bot.getAttackStrengthScale(0.0f) >= SMASH_READY_CHARGE;
    }

    public static boolean targetHasIFrames(LivingEntity target) {
        return ((CraftLivingEntity) target).getHandle().invulnerableTime > IFRAME_BLOCK_THRESHOLD;
    }
}
