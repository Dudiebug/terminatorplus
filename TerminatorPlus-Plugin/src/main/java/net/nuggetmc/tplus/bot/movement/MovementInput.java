package net.nuggetmc.tplus.bot.movement;

import net.nuggetmc.tplus.api.agent.legacyagent.LegacyUtils;
import net.nuggetmc.tplus.api.agent.legacyagent.ai.movement.MovementNetworkShape;
import net.nuggetmc.tplus.bot.Bot;
import net.nuggetmc.tplus.bot.combat.CombatIntent;
import net.nuggetmc.tplus.bot.combat.MovementBranchFamily;
import net.nuggetmc.tplus.bot.combat.MovementState;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.List;

/**
 * Stable MovementNetwork input vector. Persistence/training must preserve this
 * order:
 * relX, relY, relZ, distance, horizontalDistance, botVelX, botVelY, botVelZ,
 * botHorizontalSpeed, targetVelX, targetVelY, targetVelZ, targetHorizontalSpeed,
 * facingDot, facingCross, grounded, falling, sprinting, recentlyJumped,
 * reachable, obstructed, desiredRange, rangeUrgency, wantsCritSetup,
 * wantsSprintHit, wantsHoldPosition, isCommitted, commitProgress, weaponRange,
 * branchFamily one-hot fields in {@link MovementBranchFamily#values()} order.
 */
public record MovementInput(double[] values, boolean valid, String fallbackReason) {

    public static final List<String> ORDER = MovementNetworkShape.INPUT_FIELDS;

    public static final int COUNT = MovementNetworkShape.INPUT_COUNT;

    public MovementInput {
        values = values == null ? new double[COUNT] : Arrays.copyOf(values, values.length);
        for (int i = 0; i < values.length; i++) {
            values[i] = clamp(values[i], -1.0, 1.0);
        }
        fallbackReason = fallbackReason == null ? "" : fallbackReason;
        valid = valid && values.length == COUNT;
    }

    @Override
    public double[] values() {
        return Arrays.copyOf(values, values.length);
    }

    public static MovementInput invalid(String reason) {
        return new MovementInput(new double[COUNT], false, reason);
    }

    public static MovementInput from(Bot bot, LivingEntity target) {
        return from(bot, target, bot == null ? CombatIntent.DEFAULT : bot.getCombatIntent());
    }

    public static MovementInput from(Bot bot, LivingEntity target, CombatIntent intent) {
        if (bot == null || target == null || !target.isValid()) {
            return invalid("missing-bot-or-target");
        }
        CombatIntent safeIntent = intent == null ? CombatIntent.DEFAULT : intent;

        try {
            Location botLoc = bot.getLocation();
            Location targetLoc = target.getLocation();
            Vector relative = targetLoc.toVector().subtract(botLoc.toVector());
            Vector botVelocity = bot.getVelocity();
            Vector targetVelocity = target.getVelocity();
            Vector botHorizontal = botVelocity.clone().setY(0);
            Vector targetHorizontal = targetVelocity.clone().setY(0);
            Vector toTargetHorizontal = relative.clone().setY(0);
            double horizontalDistance = toTargetHorizontal.length();
            double distance = relative.length();
            Vector facing = botLoc.getDirection().setY(0);
            if (facing.lengthSquared() > 1.0e-9) {
                facing.normalize();
            } else {
                facing.setX(0).setY(0).setZ(1);
            }
            Vector targetDir = toTargetHorizontal.clone();
            if (targetDir.lengthSquared() > 1.0e-9) {
                targetDir.normalize();
            }

            boolean reachable = LegacyUtils.checkFreeSpace(bot.getBukkitEntity().getEyeLocation(), target.getEyeLocation())
                    || LegacyUtils.checkFreeSpace(bot.getBukkitEntity().getEyeLocation(), targetLoc);

            double[] values = new double[COUNT];
            int i = 0;
            values[i++] = norm(relative.getX(), 32.0);
            values[i++] = norm(relative.getY(), 16.0);
            values[i++] = norm(relative.getZ(), 32.0);
            values[i++] = norm(distance, 64.0);
            values[i++] = norm(horizontalDistance, 64.0);
            values[i++] = norm(botVelocity.getX(), 1.0);
            values[i++] = norm(botVelocity.getY(), 3.5);
            values[i++] = norm(botVelocity.getZ(), 1.0);
            values[i++] = norm(botHorizontal.length(), 1.0);
            values[i++] = norm(targetVelocity.getX(), 1.0);
            values[i++] = norm(targetVelocity.getY(), 3.5);
            values[i++] = norm(targetVelocity.getZ(), 1.0);
            values[i++] = norm(targetHorizontal.length(), 1.0);
            values[i++] = clamp(facing.dot(targetDir), -1.0, 1.0);
            values[i++] = clamp(facing.getX() * targetDir.getZ() - facing.getZ() * targetDir.getX(), -1.0, 1.0);
            values[i++] = bool(bot.isBotOnGround());
            values[i++] = bool(bot.isFalling());
            values[i++] = bool(bot.isSprinting());
            values[i++] = bool(recentlyJumped(bot.getMovementState()));
            values[i++] = bool(reachable);
            values[i++] = bool(!reachable);
            values[i++] = norm(safeIntent.desiredRange(), 64.0);
            values[i++] = clamp01(safeIntent.rangeUrgency());
            values[i++] = bool(safeIntent.wantsCritSetup());
            values[i++] = bool(safeIntent.wantsSprintHit());
            values[i++] = bool(safeIntent.wantsHoldPosition());
            values[i++] = bool(safeIntent.isCommitted());
            values[i++] = clamp01(safeIntent.commitProgress());
            values[i++] = norm(safeIntent.weaponRange(), 64.0);
            MovementBranchFamily family = safeIntent.branchFamily();
            for (MovementBranchFamily candidate : MovementBranchFamily.values()) {
                values[i++] = bool(candidate == family);
            }
            return new MovementInput(values, true, "");
        } catch (RuntimeException ex) {
            return invalid(ex.getClass().getSimpleName());
        }
    }

    private static boolean recentlyJumped(MovementState state) {
        return state != null && state.justJumped();
    }

    private static double bool(boolean value) {
        return value ? 1.0 : 0.0;
    }

    private static double norm(double value, double scale) {
        if (!Double.isFinite(value) || scale <= 0.0) return 0.0;
        return clamp(value / scale, -1.0, 1.0);
    }

    private static double clamp01(double value) {
        return clamp(value, 0.0, 1.0);
    }

    private static double clamp(double value, double min, double max) {
        if (!Double.isFinite(value)) return 0.0;
        return Math.max(min, Math.min(max, value));
    }
}
