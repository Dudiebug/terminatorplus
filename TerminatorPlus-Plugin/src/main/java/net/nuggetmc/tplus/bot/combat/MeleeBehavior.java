package net.nuggetmc.tplus.bot.combat;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.nuggetmc.tplus.bot.Bot;
import org.bukkit.entity.LivingEntity;

/**
 * Cooldown-aware melee.
 *
 * <p>Vanilla {@code Player.attack(Entity)} resets the attack-strength ticker
 * on every hit, so swinging at a fixed 3-tick cadence (the previous behavior)
 * meant every swing landed at ~24 % of full damage and never produced a crit
 * or sweep. Gating on {@link BotCombatTiming#chargeReady(Bot)} (>= 0.95) makes
 * the bot wait the full per-weapon recharge window, then hit at full damage.
 *
 * <p>Crit opportunism: if the bot is grounded, not sprinting, and pathing
 * didn't already issue a jump this tick, fire a small upward impulse and let
 * the next tick's {@code fallDistance > 0} register a crit. One wasted tick
 * worst case for +50 % damage on the resulting swing.
 *
 * <p>{@link CombatDirector#tick} early-returns for neural-network bots so
 * training fitness scores remain on the deterministic legacy table.
 */
public final class MeleeBehavior implements WeaponBehavior {

    private static final double ATTACK_RANGE = 4.0;
    /** Vanilla jump impulse — produces fallDistance>0 within one tick. */
    private static final double HOP_IMPULSE_Y = 0.42;

    @Override
    public int ticksFor(Bot bot, LivingEntity target, double distance) {
        if (distance > ATTACK_RANGE) return 0;
        if (BotCombatTiming.targetHasIFrames(target)) return 0;
        if (!BotCombatTiming.chargeReady(bot)) return 0;

        var cd = bot.getBotCooldowns();
        byte hopState = cd.getMeleeHopState();

        if (hopState == 1) {
            // Hop issued last tick. Swing now — fallDistance>0 gives the crit if airborne.
            bot.attack(target);
            cd.setMeleeHopState((byte) 0);
            return 0;
        }

        if (shouldHopFirst(bot)) {
            Vec3 v = bot.getDeltaMovement();
            bot.setDeltaMovement(v.x, HOP_IMPULSE_Y, v.z);
            bot.hasImpulse = true;
            cd.setMeleeHopState((byte) 1);
            return 0;
        }

        bot.attack(target);
        return 0;
    }

    private static boolean shouldHopFirst(Bot bot) {
        if (!bot.onGround()) return false;
        if (bot.isSprinting()) return false;            // sprint+crit collapses into sprint-KB without crit damage
        if (bot.getDeltaMovement().y > 0.05) return false; // pathing already jumped this tick
        if (bot.hasEffect(MobEffects.BLINDNESS)) return false;
        if (bot.isInWater()) return false;
        if (bot.onClimbable()) return false;
        if (bot.isPassenger()) return false;
        return true;
    }
}
