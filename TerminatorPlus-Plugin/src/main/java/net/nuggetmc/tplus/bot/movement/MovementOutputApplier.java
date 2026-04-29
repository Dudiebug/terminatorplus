package net.nuggetmc.tplus.bot.movement;

import net.nuggetmc.tplus.api.agent.legacyagent.ai.movement.MovementNetwork;
import net.nuggetmc.tplus.bot.Bot;
import net.nuggetmc.tplus.bot.combat.CombatDebugger;
import net.nuggetmc.tplus.bot.combat.CombatIntent;
import net.nuggetmc.tplus.bot.combat.MovementState;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Converts movement-network preferences into bot movement. It never selects
 * weapons, starts combat branches, uses items, or calls attack methods.
 */
public final class MovementOutputApplier {

    private static final double WALK_SPEED = 0.32;
    private static final double SPRINT_SPEED = 0.42;
    private static final double JUMP_THRESHOLD = 0.65;
    private static final double SPRINT_THRESHOLD = 0.55;
    private static final double HOLD_THRESHOLD = 0.65;
    private static final Map<UUID, String> LAST_INVALID_SHAPE = new ConcurrentHashMap<>();

    private boolean enabled = true;

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ApplyResult tryApply(Bot bot, LivingEntity target, MovementNetwork network) {
        if (bot == null || target == null || !target.isValid()) {
            return ApplyResult.fallback("missing-bot-or-target");
        }
        if (!enabled) {
            writeObservedState(bot, target, false);
            return ApplyResult.fallback("disabled");
        }
        if (network == null) {
            writeObservedState(bot, target, false);
            return ApplyResult.fallback("missing-network");
        }

        MovementInput input = MovementInput.from(bot, target, bot.getCombatIntent());
        if (!input.valid()) {
            writeObservedState(bot, target, false);
            return ApplyResult.fallback("invalid-input:" + input.fallbackReason());
        }

        MovementNetwork.Validation validation = network.validate(MovementInput.COUNT, MovementOutput.COUNT);
        if (!validation.valid()) {
            reportInvalidShape(bot, validation.reason());
            writeObservedState(bot, target, false);
            return ApplyResult.fallback("invalid-shape:" + validation.reason());
        }

        MovementOutput output = MovementOutput.fromRaw(network.evaluate(input.values()));
        CombatIntent intent = bot.getCombatIntent();
        if (intent.wantsHoldPosition() || output.holdPosition() >= HOLD_THRESHOLD) {
            writeObservedState(bot, target, false);
            return ApplyResult.held(output);
        }

        applyMovement(bot, target, output);
        writeObservedState(bot, target, output.jumpDesire() >= JUMP_THRESHOLD && bot.isBotOnGround());
        return ApplyResult.applied(output);
    }

    private static void applyMovement(Bot bot, LivingEntity target, MovementOutput output) {
        Location botLoc = bot.getLocation();
        Vector toTarget = target.getLocation().toVector().subtract(botLoc.toVector()).setY(0);
        if (toTarget.lengthSquared() > 1.0e-9) {
            toTarget.normalize();
        } else {
            toTarget = botLoc.getDirection().setY(0);
            if (toTarget.lengthSquared() > 1.0e-9) {
                toTarget.normalize();
            } else {
                toTarget = new Vector(0, 0, 1);
            }
        }

        toTarget.rotateAroundY(output.facingAdjustment() * Math.PI / 3.0);
        Vector strafe = new Vector(-toTarget.getZ(), 0, toTarget.getX());
        double forward = output.retreatDesire() > 0.55
                ? -Math.max(Math.abs(output.forwardPressure()), output.retreatDesire())
                : output.forwardPressure();
        Vector desired = toTarget.multiply(forward).add(strafe.multiply(output.strafePressure()));
        if (desired.lengthSquared() > 1.0) desired.normalize();

        boolean sprinting = output.sprintDesire() >= SPRINT_THRESHOLD && output.retreatDesire() < 0.55;
        bot.setSprinting(sprinting);
        double speed = (sprinting ? SPRINT_SPEED : WALK_SPEED) * Math.max(0.25, output.urgency());
        desired.multiply(speed);

        if (output.jumpDesire() >= JUMP_THRESHOLD && bot.isBotOnGround()) {
            bot.jump(new Vector(desired.getX(), 0.42, desired.getZ()));
        } else {
            bot.walk(desired);
        }
    }

    private static void writeObservedState(Bot bot, LivingEntity target, boolean justJumped) {
        Vector velocity = bot.getVelocity();
        Vector horizontal = velocity.clone().setY(0);
        Vector toTarget = target.getLocation().toVector().subtract(bot.getLocation().toVector()).setY(0);
        double approachSpeed = 0.0;
        boolean retreating = false;
        boolean circling = false;

        if (horizontal.lengthSquared() > 1.0e-9 && toTarget.lengthSquared() > 1.0e-9) {
            Vector direction = toTarget.clone().normalize();
            double forwardSpeed = horizontal.dot(direction);
            double lateralSpeed = horizontal.getX() * direction.getZ() - horizontal.getZ() * direction.getX();
            approachSpeed = Math.max(0.0, forwardSpeed);
            retreating = forwardSpeed < -0.03;
            circling = Math.abs(lateralSpeed) > Math.abs(forwardSpeed) && Math.abs(lateralSpeed) > 0.03;
        }

        bot.setMovementState(new MovementState(
                bot.isSprinting(),
                justJumped || bot.getMovementState().justJumped(),
                bot.isFalling(),
                retreating,
                circling,
                approachSpeed,
                bot.getLocation().getDirection()
        ));
    }

    private static void reportInvalidShape(Bot bot, String reason) {
        String previous = LAST_INVALID_SHAPE.put(bot.getUUID(), reason);
        if (reason.equals(previous) && bot.getAliveTicks() % 100 != 0) return;
        CombatDebugger.log(bot, "move-net-invalid", "reason=" + reason);
    }

    public record ApplyResult(boolean applied, boolean fallback, boolean held, String reason, MovementOutput output) {
        public static ApplyResult applied(MovementOutput output) {
            return new ApplyResult(true, false, false, "applied", output);
        }

        public static ApplyResult held(MovementOutput output) {
            return new ApplyResult(false, false, true, "hold-position", output);
        }

        public static ApplyResult fallback(String reason) {
            return new ApplyResult(false, true, false, reason, MovementOutput.ZERO);
        }
    }
}
