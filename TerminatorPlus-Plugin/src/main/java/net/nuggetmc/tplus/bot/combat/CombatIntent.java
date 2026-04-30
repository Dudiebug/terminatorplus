package net.nuggetmc.tplus.bot.combat;

/**
 * Director -> MovementNetwork contract. CombatDirector writes these movement
 * hints, but remains responsible for combat timing, legality, and execution.
 */
public record CombatIntent(
        MovementBranchFamily branchFamily,
        String playId,
        double desiredRange,
        DesiredRangeBand desiredRangeBand,
        double rangeUrgency,
        MovementBranchFamily lockFamily,
        String lockReason,
        int lockUntilTick,
        boolean interruptible,
        String plannedAction,
        boolean wantsCritSetup,
        boolean wantsSprintHit,
        boolean wantsHoldPosition,
        boolean isCommitted,
        double commitProgress,
        double weaponRange
) {
    public static final CombatIntent DEFAULT = new CombatIntent(
            MovementBranchFamily.GENERAL_FALLBACK,
            "",
            3.5,
            DesiredRangeBand.MELEE,
            0.0,
            MovementBranchFamily.GENERAL_FALLBACK,
            "",
            0,
            true,
            "none",
            false,
            false,
            false,
            false,
            0.0,
            MeleeBehavior.ATTACK_RANGE
    );

    public CombatIntent {
        branchFamily = branchFamily == null ? MovementBranchFamily.GENERAL_FALLBACK : branchFamily;
        playId = tokenOrEmpty(playId);
        desiredRange = finiteOrDefault(desiredRange, DEFAULT_DESIRED_RANGE);
        desiredRangeBand = desiredRangeBand == null ? DesiredRangeBand.forDistance(desiredRange) : desiredRangeBand;
        rangeUrgency = clamp01(rangeUrgency);
        lockFamily = lockFamily == null ? MovementBranchFamily.GENERAL_FALLBACK : lockFamily;
        lockReason = tokenOrEmpty(lockReason);
        lockUntilTick = Math.max(0, lockUntilTick);
        plannedAction = tokenOrDefault(plannedAction, "none");
        commitProgress = clamp01(commitProgress);
        weaponRange = finiteOrDefault(weaponRange, MeleeBehavior.ATTACK_RANGE);
    }

    public boolean movementLocked(int aliveTick) {
        return lockUntilTick > aliveTick;
    }

    public int lockTicksRemaining(int aliveTick) {
        return Math.max(0, lockUntilTick - aliveTick);
    }

    private static final double DEFAULT_DESIRED_RANGE = 3.5;

    private static double finiteOrDefault(double value, double fallback) {
        return Double.isFinite(value) ? value : fallback;
    }

    private static double clamp01(double value) {
        if (!Double.isFinite(value)) return 0.0;
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static String tokenOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String tokenOrDefault(String value, String fallback) {
        String token = tokenOrEmpty(value);
        return token.isBlank() ? fallback : token;
    }
}
