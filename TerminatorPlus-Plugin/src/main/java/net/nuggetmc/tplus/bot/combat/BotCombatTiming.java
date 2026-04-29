package net.nuggetmc.tplus.bot.combat;

import net.nuggetmc.tplus.bot.Bot;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

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
    private static final double CRIT_DESCENT_VELOCITY = -0.06;

    private BotCombatTiming() {}

    /** True when the bot is charged AND the target can take full damage right now. */
    public static boolean canSwing(Bot bot, LivingEntity target) {
        return canSwing(bot, target, READY_CHARGE);
    }

    public static boolean canSwing(Bot bot, LivingEntity target, float minCharge) {
        float charge = charge(bot);
        int iframes = ((CraftLivingEntity) target).getHandle().invulnerableTime;
        if (!bot.getBotInventory().isSelectedMeleeWeapon()) {
            CombatDebugger.swingGate(bot, charge, minCharge, iframes, false, "held");
            return false;
        }
        if (charge < minCharge) {
            CombatDebugger.swingBlock(bot, "charge", charge);
            CombatDebugger.swingGate(bot, charge, minCharge, iframes, false, "charge");
            return false;
        }
        if (iframes > IFRAME_BLOCK_THRESHOLD) {
            CombatDebugger.swingBlock(bot, "iframes", iframes);
            CombatDebugger.swingGate(bot, charge, minCharge, iframes, false, "iframes");
            return false;
        }
        CombatDebugger.swingGate(bot, charge, minCharge, iframes, true, "ready");
        return true;
    }

    public static boolean shouldWaitForCritWindow(Bot bot, LivingEntity target, double distance) {
        if (bot.hasNeuralNetwork()) return false;
        if (!bot.getBotInventory().isSelectedMeleeWeapon()) return false;
        if (targetHasIFrames(target)) return false;
        if (!chargeReady(bot)) return false;
        if (distance > MeleeBehavior.ATTACK_RANGE) return false;

        Vector velocity = bot.getVelocity();
        if (isCritWindow(bot)) return false;
        if (!bot.isBotOnGround() && velocity.getY() > CRIT_DESCENT_VELOCITY) {
            if (bot.isSprinting()) bot.setSprinting(false);
            CombatDebugger.log(bot, "melee-wait", "reason=crit-window vy=" + String.format("%.2f", velocity.getY()));
            return true;
        }
        if (bot.isBotOnGround() && distance <= 3.2) {
            if (bot.isSprinting()) bot.setSprinting(false);
            Vector jump = bot.getVelocity();
            jump.setY(0.42);
            bot.setVelocity(jump);
            CombatDebugger.log(bot, "melee-wait", "reason=crit-jump");
            return true;
        }
        return false;
    }

    public static boolean isCritWindow(Bot bot) {
        return !bot.isBotOnGround()
                && bot.getVelocity().getY() < CRIT_DESCENT_VELOCITY
                && bot.fallDistance > 0.0f
                && !bot.isSprinting();
    }

    /** Attack-strength charge scaled to [0, 1]; 1.0 means fully recharged. */
    public static boolean chargeReady(Bot bot) {
        return charge(bot) >= READY_CHARGE;
    }

    /**
     * More permissive gate for mace dive-impacts where the smash density bonus dominates.
     * Uses strict {@code >} to match vanilla's {@code attackStrengthScale > 0.848F} crit/sweep
     * gate in {@code Player.attack} — {@code >=} would fire one tick early at the boundary.
     */
    public static boolean smashChargeReady(Bot bot) {
        return bot.getAttackStrengthScale(0.0f) > SMASH_READY_CHARGE;
    }

    /**
     * Like {@link #canSwing(Bot, LivingEntity)} but bypasses the i-frame block.
     * Mace smashes scale damage with fall distance, so the landed {@code amount}
     * trivially exceeds any prior {@code lastHurt} and the {@code amount - lastHurt}
     * residual is still a meaningful hit inside the i-frame window.
     */
    public static boolean canSwingMaceSmash(Bot bot) {
        return charge(bot) > SMASH_READY_CHARGE;
    }

    /** True if hitting the target now would be wasted on its i-frame window. */
    public static boolean targetHasIFrames(LivingEntity target) {
        return ((CraftLivingEntity) target).getHandle().invulnerableTime > IFRAME_BLOCK_THRESHOLD;
    }

    /** Raw vanilla attack-strength charge in [0, 1]. */
    public static float charge(Bot bot) {
        return bot.getAttackStrengthScale(0.0f);
    }

    /** Planning guard: a full melee hit can execute right now. */
    public static boolean readyForFullMelee(Bot bot) {
        return bot.getBotInventory().isSelectedMeleeWeapon() && chargeReady(bot);
    }

    /** Planning guard for vanilla special-hit branches (crit/sprint-kb windows). */
    public static boolean readyForVanillaSpecial(Bot bot) {
        return bot.getBotInventory().isSelectedMeleeWeapon() && charge(bot) > SMASH_READY_CHARGE;
    }

    public static boolean targetCanTakeFullHit(LivingEntity target) {
        return !targetHasIFrames(target);
    }

    public static boolean shouldPlanNormalMelee(Bot bot, LivingEntity target) {
        return readyForFullMelee(bot) && targetCanTakeFullHit(target);
    }

    public static boolean shouldPlanSprintReset(Bot bot, LivingEntity target) {
        return readyForVanillaSpecial(bot) && targetCanTakeFullHit(target);
    }
}
