package net.nuggetmc.tplus.bot.combat;

/**
 * Director -> MovementNetwork contract. CombatDirector writes these movement
 * hints, but remains responsible for combat timing, legality, and execution.
 */
public record CombatIntent(
        double desiredRange,
        double rangeUrgency,
        boolean wantsCritSetup,
        boolean wantsSprintHit,
        boolean wantsHoldPosition,
        boolean isCommitted,
        double commitProgress,
        double weaponRange,
        String branchName
) {
    public static final CombatIntent DEFAULT = new CombatIntent(
            3.5,
            0.0,
            false,
            false,
            false,
            false,
            0.0,
            MeleeBehavior.ATTACK_RANGE,
            "none"
    );

    public CombatIntent {
        desiredRange = finiteOrDefault(desiredRange, DEFAULT_DESIRED_RANGE);
        rangeUrgency = clamp01(rangeUrgency);
        commitProgress = clamp01(commitProgress);
        weaponRange = finiteOrDefault(weaponRange, MeleeBehavior.ATTACK_RANGE);
        branchName = (branchName == null || branchName.isBlank()) ? "none" : branchName;
    }

    private static final double DEFAULT_DESIRED_RANGE = 3.5;

    private static double finiteOrDefault(double value, double fallback) {
        return Double.isFinite(value) ? value : fallback;
    }

    private static double clamp01(double value) {
        if (!Double.isFinite(value)) return 0.0;
        return Math.max(0.0, Math.min(1.0, value));
    }
}
