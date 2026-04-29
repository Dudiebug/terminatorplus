package net.nuggetmc.tplus.bot.combat;

import net.nuggetmc.tplus.bot.Bot;
import net.nuggetmc.tplus.bot.loadout.BotInventory;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WindCharge;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 * Jump-smash behavior for the 1.21 mace. The bot holds the mace long enough for
 * the vanilla attack-strength clock to recharge, then launches and tracks the
 * target while airborne so the impact can use real mace fall-damage scaling.
 */
public final class MaceBehavior implements WeaponBehavior {

    public static final String COOLDOWN_KEY = "mace";
    private static final double MIN_DISTANCE = 0.5;
    private static final double MAX_DISTANCE = 6.0;
    private static final double ATTACK_RANGE = 3.5;
    private static final int JUMP_COOLDOWN = 55;
    private static final int PRE_JUMP_MIN_HOLD_TICKS = 10;
    private static final int PRE_JUMP_TIMEOUT_TICKS = 45;
    private static final int HEIGHT_LOG_TICKS = 10;

    static final double LAUNCH_Y = 2.0;
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
                if (targetBlocking(target)) {
                    CombatDebugger.log(bot, "mace-skip",
                            "reason=target-blocking held=" + heldType(bot));
                    state.reset();
                    return 0;
                }
                if (!bot.getBotCooldowns().ready(COOLDOWN_KEY, bot.getAliveTicks())) {
                    int left = bot.getBotCooldowns().remaining(COOLDOWN_KEY, bot.getAliveTicks());
                    CombatDebugger.maceCd(bot, left);
                    if (distance <= ATTACK_RANGE && BotCombatTiming.canSwing(bot, target)) {
                        doAttack(bot, target);
                    }
                    return 0;
                }
                if (!bot.isBotOnGround()) {
                    CombatDebugger.log(bot, "mace-skip", "reason=airborne-no-commit");
                    return 0;
                }

                CombatDebugger.log(bot, "mace-charge-start",
                        "minHold=" + PRE_JUMP_MIN_HOLD_TICKS
                                + " charge=" + fmt(bot.getAttackStrengthScale(0.0f))
                                + " held=" + heldType(bot));
                CombatDebugger.macePhase(bot, state.getPhase(), CombatState.Phase.MACE_CHARGING);
                state.setPhase(CombatState.Phase.MACE_CHARGING);
                state.setPhaseStartY(bot.getLocation().getY());
                return 0;
            }
            case MACE_CHARGING: {
                int chargeTicks = state.tickPhase();
                float charge = bot.getAttackStrengthScale(0.0f);
                boolean ready = BotCombatTiming.smashChargeReady(bot);
                boolean planReady = BotCombatTiming.shouldLaunchMaceSmash(bot, target, LAUNCH_Y);
                CombatDebugger.log(bot, "mace-charge",
                        "ticks=" + chargeTicks
                                + " charge=" + fmt(charge)
                                + " ready=" + ready
                                + " airReady=" + planReady
                                + " held=" + heldType(bot)
                                + " ground=" + bot.isBotOnGround());

                if (targetBlocking(target)) {
                    CombatDebugger.log(bot, "mace-reset",
                            "reason=target-blocking held=" + heldType(bot));
                    CombatDebugger.macePhase(bot, state.getPhase(), CombatState.Phase.IDLE);
                    state.reset();
                    return 0;
                }
                if (!bot.isBotOnGround()) {
                    CombatDebugger.log(bot, "mace-reset",
                            "reason=prejump-airborne held=" + heldType(bot));
                    CombatDebugger.macePhase(bot, state.getPhase(), CombatState.Phase.IDLE);
                    state.reset();
                    return 0;
                }
                if (chargeTicks >= PRE_JUMP_TIMEOUT_TICKS && (!ready || !planReady)) {
                    CombatDebugger.log(bot, "mace-reset",
                            "reason=prejump-timeout charge=" + fmt(charge)
                                    + " airReady=" + planReady
                                    + " held=" + heldType(bot));
                    CombatDebugger.macePhase(bot, state.getPhase(), CombatState.Phase.IDLE);
                    state.reset();
                    return 0;
                }
                if (chargeTicks < PRE_JUMP_MIN_HOLD_TICKS || !ready || !planReady) {
                    return 0;
                }

                launch(bot, state, toTarget);
                return 0;
            }
            case AIRBORNE: {
                int airborneTicks = state.tickPhase();
                Vector vel = bot.getVelocity();
                logAirborneHeight(bot, state, airborneTicks, vel, distance);

                if (vel.getY() < -0.2) {
                    Vector horiz = toTarget.clone();
                    horiz.setY(0);
                    if (horiz.lengthSquared() > 1.0e-6) {
                        double speed = vel.getY() < -0.6 ? 0.28 : 0.15;
                        horiz.normalize().multiply(speed);
                        bot.walk(horiz);
                    }
                }

                if (distance <= ATTACK_RANGE && !bot.isBotOnGround() && vel.getY() < -0.3) {
                    boolean iframes = BotCombatTiming.targetHasIFrames(target);
                    CombatDebugger.maceSmash(bot, vel.getY(), iframes, bot.isBotOnGround());
                    if (!BotCombatTiming.canSwingMaceSmash(bot)) {
                        CombatDebugger.log(bot, "mace-smash-wait",
                                "reason=charge charge=" + fmt(bot.getAttackStrengthScale(0.0f)));
                        return 0;
                    }
                    if (!iframes) {
                        doAttack(bot, target);
                    }
                    CombatDebugger.macePhase(bot, state.getPhase(), CombatState.Phase.IDLE);
                    state.reset();
                    return 0;
                }

                if (bot.isBotOnGround()) {
                    CombatDebugger.macePhase(bot, state.getPhase(), CombatState.Phase.IDLE);
                    state.reset();
                }
                if (airborneTicks > 80) {
                    CombatDebugger.log(bot, "mace-reset", "reason=airborne-timeout");
                    CombatDebugger.macePhase(bot, state.getPhase(), CombatState.Phase.IDLE);
                    state.reset();
                }
                return 0;
            }
            default:
                return 0;
        }
    }

    private void launch(Bot bot, CombatState state, Vector toTarget) {
        Vector horiz = toTarget.clone();
        horiz.setY(0);
        if (horiz.lengthSquared() > 1.0e-6) {
            horiz.normalize().multiply(JUMP_XZ);
        } else {
            horiz.setX(0).setZ(0);
        }

        Vector launch = new Vector(horiz.getX(), LAUNCH_Y, horiz.getZ());
        double startY = bot.getLocation().getY();

        throwWindChargeAtFeet(bot);
        bot.setVelocity(launch);

        CombatDebugger.log(bot, "mace-jump",
                "src=behavior vel=" + fmtVec(launch)
                        + " y0=" + fmt(startY)
                        + " charge=" + fmt(bot.getAttackStrengthScale(0.0f))
                        + " held=" + heldType(bot));
        bot.getBotCooldowns().set(COOLDOWN_KEY, JUMP_COOLDOWN, bot.getAliveTicks());
        bot.getBotCooldowns().set(WindChargeBehavior.COOLDOWN_KEY, 55, bot.getAliveTicks());
        CombatDebugger.macePhase(bot, state.getPhase(), CombatState.Phase.AIRBORNE);
        state.setPhase(CombatState.Phase.AIRBORNE);
        state.setPhaseStartY(startY);
        bot.getLocation().getWorld().playSound(bot.getLocation(), Sound.ENTITY_PLAYER_BIG_FALL, 0.3f, 1.6f);
    }

    private static void logAirborneHeight(Bot bot, CombatState state, int airborneTicks, Vector vel, double distance) {
        if (!CombatDebugger.isOn(bot) || airborneTicks > HEIGHT_LOG_TICKS) return;
        double y = bot.getLocation().getY();
        double startY = state.getPhaseStartY();
        double height = Double.isNaN(startY) ? 0.0 : y - startY;
        CombatDebugger.log(bot, "mace-air",
                "airTick=" + airborneTicks
                        + " y=" + fmt(y)
                        + " height=" + fmt(height)
                        + " vy=" + fmt(vel.getY())
                        + " dist=" + fmt(distance)
                        + " charge=" + fmt(bot.getAttackStrengthScale(0.0f))
                        + " held=" + heldType(bot)
                        + " ground=" + bot.isBotOnGround()
                        + " fall=" + fmt(bot.fallDistance));
    }

    private void doAttack(Bot bot, LivingEntity target) {
        boolean crit = BotCombatTiming.isCritWindow(bot);
        float charge = bot.getAttackStrengthScale(0.0f);
        double before = target.getHealth();
        boolean targetBlocking = targetBlocking(target);
        if (CombatDebugger.isOn(bot)) {
            CombatDebugger.log(bot, "mace-attack",
                    "crit=" + crit
                            + " charge=" + fmt(charge)
                            + " fall=" + fmt(bot.fallDistance)
                            + " vy=" + fmt(bot.getVelocity().getY())
                            + " targetBlocking=" + targetBlocking
                            + " targetHp=" + fmt(before)
                            + " held=" + heldType(bot));
        }

        bot.punch();
        if (bot.getBukkitEntity() instanceof Player attacker) {
            attacker.attack(target);
        } else {
            bot.attack(target);
        }

        if (CombatDebugger.isOn(bot)) {
            double after = Math.max(0.0, target.getHealth());
            CombatDebugger.log(bot, "mace-damage",
                    "crit=" + crit
                            + " hp=" + fmt(before) + "->" + fmt(after)
                            + " delta=" + fmt(before - after)
                            + " targetBlocking=" + targetBlocking);
        }
    }

    private static void throwWindChargeAtFeet(Bot bot) {
        BotInventory inv = bot.getBotInventory();
        if (!inv.hasWindCharge()) {
            return;
        }

        Location botLoc = bot.getLocation();
        Location spawn = botLoc.clone().add(0, bot.getBukkitEntity().getEyeHeight() - 0.1, 0);
        Vector downward = new Vector(0, -1.5, 0);

        spawn.getWorld().spawn(spawn, WindCharge.class, w -> {
            w.setShooter(bot.getBukkitEntity());
            w.setVelocity(downward);
        });
        botLoc.getWorld().playSound(botLoc, Sound.ENTITY_WIND_CHARGE_THROW, 1f, 1.1f);
        consumeOneWindCharge(inv);
    }

    private static void consumeOneWindCharge(BotInventory inv) {
        inv.decrementMaterialOrOffhand(Material.WIND_CHARGE);
    }

    private static boolean targetBlocking(LivingEntity target) {
        return target instanceof Player player && player.isBlocking();
    }

    private static String heldType(Bot bot) {
        ItemStack held = bot.getBukkitEntity().getInventory().getItemInMainHand();
        return held == null ? "AIR" : held.getType().name();
    }

    private static String fmt(double value) {
        return String.format("%.2f", value);
    }

    private static String fmtVec(Vector vec) {
        return fmt(vec.getX()) + "," + fmt(vec.getY()) + "," + fmt(vec.getZ());
    }
}
