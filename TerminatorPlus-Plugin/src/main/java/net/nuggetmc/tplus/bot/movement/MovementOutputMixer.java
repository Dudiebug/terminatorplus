package net.nuggetmc.tplus.bot.movement;

import net.nuggetmc.tplus.bot.combat.CombatIntent;
import net.nuggetmc.tplus.bot.combat.MovementObjective;

public final class MovementOutputMixer {

    public enum Mode {
        BASELINE_ONLY,
        NN_ONLY,
        BLENDED,
        NN_RESIDUAL,
        NN_WHEN_CONFIDENT
    }

    private static final double NN_WEIGHT = 0.35;
    private static final double RESIDUAL_WEIGHT = 0.30;

    public MixResult mix(CombatIntent intent, MovementOutput baseline, MovementOutput nn, boolean nnAvailable) {
        CombatIntent safeIntent = intent == null ? CombatIntent.DEFAULT : intent;
        MovementOutput safeBaseline = baseline == null ? MovementOutput.ZERO : baseline;
        if (!nnAvailable || nn == null) {
            return new MixResult(Mode.BASELINE_ONLY, safeBaseline, "nn-unavailable");
        }

        Mode mode = Mode.NN_RESIDUAL;
        MovementOutput mixed = residual(safeBaseline, nn);
        String reason = "none";

        if (safeIntent.wantsHoldPosition() || safeIntent.movementObjective() == MovementObjective.HOLD) {
            mixed = forceHold(mixed, safeBaseline);
            reason = "hold-floor";
        } else if (safeIntent.movementObjective() == MovementObjective.EXPLOSIVE_ESCAPE
                || safeIntent.movementObjective() == MovementObjective.DISENGAGE
                || safeIntent.movementObjective() == MovementObjective.RETREAT) {
            mixed = forceRetreat(mixed, safeBaseline);
            reason = "retreat-floor";
        } else if (safeBaseline.jumpDesire() < 0.5 && nn.jumpDesire() > 0.85) {
            mixed = new MovementOutput(mixed.forwardPressure(), mixed.strafePressure(), safeBaseline.jumpDesire(),
                    mixed.sprintDesire(), mixed.retreatDesire(), mixed.facingAdjustment(), mixed.urgency(), mixed.holdPosition());
            reason = "jump-floor";
        }

        return new MixResult(mode, mixed, reason);
    }

    private static MovementOutput residual(MovementOutput baseline, MovementOutput nn) {
        return new MovementOutput(
                blend(baseline.forwardPressure(), nn.forwardPressure(), RESIDUAL_WEIGHT),
                blend(baseline.strafePressure(), nn.strafePressure(), RESIDUAL_WEIGHT),
                blend(baseline.jumpDesire(), nn.jumpDesire(), RESIDUAL_WEIGHT),
                blend(baseline.sprintDesire(), nn.sprintDesire(), RESIDUAL_WEIGHT),
                Math.max(baseline.retreatDesire(), nn.retreatDesire() * NN_WEIGHT),
                blend(baseline.facingAdjustment(), nn.facingAdjustment(), RESIDUAL_WEIGHT),
                Math.max(baseline.urgency(), nn.urgency() * NN_WEIGHT),
                Math.max(baseline.holdPosition(), nn.holdPosition() * NN_WEIGHT)
        );
    }

    private static MovementOutput forceHold(MovementOutput mixed, MovementOutput baseline) {
        return new MovementOutput(0.0, 0.0, 0.0, 0.0, 0.0, mixed.facingAdjustment(),
                Math.max(mixed.urgency(), baseline.urgency()), 1.0);
    }

    private static MovementOutput forceRetreat(MovementOutput mixed, MovementOutput baseline) {
        return new MovementOutput(Math.min(mixed.forwardPressure(), baseline.forwardPressure()),
                mixed.strafePressure(), Math.min(mixed.jumpDesire(), baseline.jumpDesire()),
                0.0, Math.max(mixed.retreatDesire(), baseline.retreatDesire()),
                mixed.facingAdjustment(), Math.max(mixed.urgency(), baseline.urgency()), mixed.holdPosition());
    }

    private static double blend(double baseline, double nn, double weight) {
        return baseline * (1.0 - weight) + nn * weight;
    }

    public record MixResult(Mode mode, MovementOutput output, String safetyReason) {
    }
}
