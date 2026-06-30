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
        double weaponRange,
        MovementObjective movementObjective,
        CombatActionCategory actionCategory,
        double minSafeRange,
        double maxUsefulRange,
        double rangeErrorSigned,
        int commitTicksRemaining,
        double botAttackStrength,
        boolean targetHasIFrames,
        float botHealthFraction,
        float targetHealthFraction,
        double healthAdvantage,
        boolean targetBlocking,
        boolean targetAirborne,
        boolean targetRising,
        boolean targetSprintingAway,
        boolean targetInCobweb,
        boolean targetOverVoid,
        boolean openSkyAboveBot,
        boolean botInLavaArea,
        boolean botOnFire,
        boolean needsLineOfSight
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
            MeleeBehavior.ATTACK_RANGE,
            MovementObjective.ORBIT,
            CombatActionCategory.NONE,
            1.8,
            5.0,
            0.0,
            0,
            0.0,
            false,
            1.0f,
            1.0f,
            0.0,
            false,
            false,
            false,
            false,
            false,
            false,
            true,
            false,
            false,
            false
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
        movementObjective = movementObjective == null ? MovementObjective.ORBIT : movementObjective;
        actionCategory = actionCategory == null ? CombatActionCategory.NONE : actionCategory;
        minSafeRange = finiteOrDefault(minSafeRange, 1.8);
        maxUsefulRange = finiteOrDefault(maxUsefulRange, Math.max(weaponRange, desiredRange + 1.5));
        rangeErrorSigned = finiteOrDefault(rangeErrorSigned, 0.0);
        commitTicksRemaining = Math.max(0, commitTicksRemaining);
        botAttackStrength = clamp01(botAttackStrength);
        botHealthFraction = (float) clamp01(botHealthFraction);
        targetHealthFraction = (float) clamp01(targetHealthFraction);
        healthAdvantage = clamp(healthAdvantage, -1.0, 1.0);
    }

    public boolean movementLocked(int aliveTick) {
        return lockUntilTick > aliveTick;
    }

    public int lockTicksRemaining(int aliveTick) {
        return Math.max(0, lockUntilTick - aliveTick);
    }

    public String debugSummary() {
        return "obj=" + movementObjective
                + " action=" + actionCategory
                + " planned=" + plannedAction
                + " rangeErr=" + String.format("%.2f", rangeErrorSigned)
                + " min=" + String.format("%.2f", minSafeRange)
                + " max=" + String.format("%.2f", maxUsefulRange)
                + " commitLeft=" + commitTicksRemaining
                + " atk=" + String.format("%.2f", botAttackStrength)
                + " hp=" + String.format("%.2f", botHealthFraction)
                + "/" + String.format("%.2f", targetHealthFraction);
    }

    private static final double DEFAULT_DESIRED_RANGE = 3.5;

    private static double finiteOrDefault(double value, double fallback) {
        return Double.isFinite(value) ? value : fallback;
    }

    private static double clamp01(double value) {
        return clamp(value, 0.0, 1.0);
    }

    private static double clamp(double value, double min, double max) {
        if (!Double.isFinite(value)) return 0.0;
        return Math.max(min, Math.min(max, value));
    }

    private static String tokenOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String tokenOrDefault(String value, String fallback) {
        String token = tokenOrEmpty(value);
        return token.isBlank() ? fallback : token;
    }
}
