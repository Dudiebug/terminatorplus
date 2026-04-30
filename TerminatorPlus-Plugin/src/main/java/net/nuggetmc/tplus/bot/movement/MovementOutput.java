package net.nuggetmc.tplus.bot.movement;

import net.nuggetmc.tplus.api.agent.legacyagent.ai.movement.MovementNetworkShape;

import java.util.List;

/**
 * Stable MovementNetwork output schema. Outputs are movement preferences only:
 * forwardPressure, strafePressure, jumpDesire, sprintDesire, retreatDesire,
 * facingAdjustment, urgency, holdPosition.
 */
public record MovementOutput(
        double forwardPressure,
        double strafePressure,
        double jumpDesire,
        double sprintDesire,
        double retreatDesire,
        double facingAdjustment,
        double urgency,
        double holdPosition
) {
    public static final int COUNT = MovementNetworkShape.OUTPUT_COUNT;
    public static final List<String> ORDER = MovementNetworkShape.OUTPUT_FIELDS;
    public static final MovementOutput ZERO = new MovementOutput(0, 0, 0, 0, 0, 0, 0, 0);

    public MovementOutput {
        forwardPressure = clampSigned(forwardPressure);
        strafePressure = clampSigned(strafePressure);
        jumpDesire = clamp01(jumpDesire);
        sprintDesire = clamp01(sprintDesire);
        retreatDesire = clamp01(retreatDesire);
        facingAdjustment = clampSigned(facingAdjustment);
        urgency = clamp01(urgency);
        holdPosition = clamp01(holdPosition);
    }

    public static MovementOutput fromRaw(double[] raw) {
        if (raw == null || raw.length != COUNT) return ZERO;
        return new MovementOutput(
                signed(raw[0]),
                signed(raw[1]),
                desire(raw[2]),
                desire(raw[3]),
                desire(raw[4]),
                signed(raw[5]),
                desire(raw[6]),
                desire(raw[7])
        );
    }

    private static double signed(double value) {
        return clampSigned(value);
    }

    private static double desire(double value) {
        if (!Double.isFinite(value)) return 0.0;
        return clamp01((value + 1.0) * 0.5);
    }

    private static double clampSigned(double value) {
        if (!Double.isFinite(value)) return 0.0;
        return Math.max(-1.0, Math.min(1.0, value));
    }

    private static double clamp01(double value) {
        if (!Double.isFinite(value)) return 0.0;
        return Math.max(0.0, Math.min(1.0, value));
    }
}
