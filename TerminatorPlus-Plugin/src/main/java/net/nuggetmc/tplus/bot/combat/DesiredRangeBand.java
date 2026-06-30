package net.nuggetmc.tplus.bot.combat;

/**
 * Coarse range target for movement routing and telemetry.
 */
public enum DesiredRangeBand {
    MELEE(3.2),
    CLOSE(5.0),
    MID(12.0),
    LONG(28.0),
    FAR(48.0);

    private final double center;

    DesiredRangeBand(double center) {
        this.center = center;
    }

    public double center() {
        return center;
    }

    public static DesiredRangeBand forDistance(double desiredRange) {
        if (!Double.isFinite(desiredRange)) return MELEE;
        if (desiredRange <= 3.7) return MELEE;
        if (desiredRange <= 6.0) return CLOSE;
        if (desiredRange <= 18.0) return MID;
        if (desiredRange <= 36.0) return LONG;
        return FAR;
    }
}
