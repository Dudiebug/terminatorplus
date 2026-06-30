package net.nuggetmc.tplus.api.agent.legacyagent.ai.movement;

public record LiveDuelMetricsSnapshot(
        boolean available,
        int ticks,
        double damageDealt,
        double damageTaken,
        double damageDelta,
        double damageRatio,
        int desiredRangeTicks,
        int tooCloseTicks,
        int tooFarTicks,
        double timeInDesiredRangeRate,
        double tooCloseRate,
        double tooFarRate,
        int movementFallbackTicks,
        int movementHeldTicks,
        int routeThrashCount,
        double fallbackRate,
        int fakeActionCount,
        int instantConsumeCount,
        int illegalSameTickActionCount,
        int actionInterruptionCount,
        int healCompletionCount,
        int healCancelCount,
        int retreatTicks,
        int retreatSuccessTicks,
        int retreatFailureTicks,
        double retreatSuccessRate
) {
    public static LiveDuelMetricsSnapshot unavailable() {
        return new LiveDuelMetricsSnapshot(false, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
