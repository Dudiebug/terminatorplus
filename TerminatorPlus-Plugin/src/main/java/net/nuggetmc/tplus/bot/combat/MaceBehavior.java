package net.nuggetmc.tplus.bot.combat;

import net.nuggetmc.tplus.bot.Bot;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Jump-smash behavior for the 1.21 mace. The bot launches itself upward
 * (and slightly toward the target), descends, and slams into the target
 * — vanilla fall-damage scaling then makes the hit catastrophic.
 */
public final class MaceBehavior implements WeaponBehavior {

    public static final String COOLDOWN_KEY = "mace";
    private static final double MIN_DISTANCE = 0.5;
    private static final double MAX_DISTANCE = 6.0;
    private static final double ATTACK_RANGE = 3.5;
    private static final int JUMP_COOLDOWN = 55;

    /** Total upward impulse (Y component of initial velocity). Tuned for ~3-block peak. */
    private static final double JUMP_Y = 1.35;
    /** Horizontal impulse blended with target direction. */
    private static final double JUMP_XZ = 0.25;

    @Override
    public int ticksFor(Bot bot, LivingEntity target, double distance) {
        CombatState state = bot.getCombatState();
        Location targetLoc = target.getLocation();
        bot.faceLocation(targetLoc);

        if (distance < MIN_DISTANCE) {
            state.reset();
            return 0;
        }
        // During an aerial dive the bot may be far above the target; let it keep tracking.
        if (distance > MAX_DISTANCE && state.getPhase() != CombatState.Phase.AIRBORNE) {
            state.reset();
            return 0;
        }

        Vector botPos = bot.getLocation().toVector();
        Vector toTarget = targetLoc.toVector().subtract(botPos);

        switch (state.getPhase()) {
            case CHARGING:
            case IDLE:
            case RELEASE: {
                if (!bot.getBotCooldowns().ready(COOLDOWN_KEY, bot.getAliveTicks())) {
                    int left = bot.getBotCooldowns().remaining(COOLDOWN_KEY, bot.getAliveTicks());
                    CombatDebugger.maceCd(bot, left);
                    // Stay close but don't jump-smash. Fall through to melee on the sword if lateral.
                    if (distance <= ATTACK_RANGE && BotCombatTiming.canSwing(bot, target)) {
                        doAttack(bot, target);
                    }
                    return 0;
                }
                if (!bot.isBotOnGround()) {
                    CombatDebugger.log(bot, "mace-skip", "reason=airborne-no-commit");
                    return 0;
                }

                Vector horiz = toTarget.clone();
                horiz.setY(0);
                if (horiz.lengthSquared() > 1.0e-6) horiz.normalize().multiply(JUMP_XZ);
                Vector launch = horiz.setY(JUMP_Y);

                bot.jump(launch);
                bot.getBotCooldowns().set(COOLDOWN_KEY, JUMP_COOLDOWN, bot.getAliveTicks());
                CombatDebugger.macePhase(bot, state.getPhase(), CombatState.Phase.AIRBORNE);
                state.setPhase(CombatState.Phase.AIRBORNE);
                bot.getLocation().getWorld().playSound(bot.getLocation(), Sound.ENTITY_PLAYER_BIG_FALL, 0.3f, 1.6f);
                return 0;
            }
            case AIRBORNE: {
                // Track the target on the way down; accelerate harder during a fast dive.
                Vector vel = bot.getVelocity();
                if (vel.getY() < -0.2) {
                    Vector horiz = toTarget.clone();
                    horiz.setY(0);
                    if (horiz.lengthSquared() > 1.0e-6) {
                        double speed = vel.getY() < -0.6 ? 0.28 : 0.15;
                        horiz.normalize().multiply(speed);
                        bot.walk(horiz);
                    }
                }
                // Impact check. The dive itself is committed, so don't gate on charge — the
                // density + fall-damage bonus dominates base damage. Just skip deep i-frames
                // so the smash isn't wasted on a target that can't take the hit.
                if (distance <= ATTACK_RANGE && (bot.isBotOnGround() || vel.getY() < -0.6)) {
                    boolean iframes = BotCombatTiming.targetHasIFrames(target);
                    CombatDebugger.maceSmash(bot, vel.getY(), iframes, bot.isBotOnGround());
                    if (!iframes) {
                        doAttack(bot, target);
                    }
                    CombatDebugger.macePhase(bot, state.getPhase(), CombatState.Phase.IDLE);
                    state.reset();
                }
                return 0;
            }
            default:
                return 0;
        }
    }

    private void doAttack(Bot bot, LivingEntity target) {
        bot.punch();
        if (bot.getBukkitEntity() instanceof Player attacker) {
            // Use vanilla attack so 1.21 mace fall-damage scaling + density bonuses apply.
            attacker.attack(target);
        } else {
            bot.attack(target);
        }
    }
}
