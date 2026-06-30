package net.nuggetmc.tplus.api.agent.legacyagent.ai.movement;

public record MovementTrainingSnapshot(
        boolean available,
        double distance,
        double horizontalDistance,
        double desiredRange,
        double rangeUrgency,
        boolean wantsCritSetup,
        boolean wantsSprintHit,
        boolean wantsHoldPosition,
        boolean committed,
        double commitProgress,
        boolean sprinting,
        boolean justJumped,
        boolean falling,
        boolean retreating,
        boolean circling,
        double approachSpeed,
        boolean legalCritSetup,
        boolean legalSprintSetup,
        boolean holdCompliant,
        boolean controllerFallback,
        String activeBranchFamily,
        String playId,
        String plannedAction,
        boolean movementLocked,
        String lockFamily
) {
    public MovementTrainingSnapshot {
        activeBranchFamily = MovementTrainingConfig.normalizeFamilyId(activeBranchFamily);
        playId = playId == null ? "" : playId.trim();
        plannedAction = plannedAction == null || plannedAction.isBlank() ? "none" : plannedAction.trim();
        lockFamily = MovementTrainingConfig.normalizeFamilyId(lockFamily);
    }

    public static MovementTrainingSnapshot unavailable() {
        return new MovementTrainingSnapshot(false, 0, 0, 0, 0, false, false,
                false, false, 0, false, false, false, false, false, 0,
                false, false, false, true, MovementBrainBank.FALLBACK_BRAIN_NAME,
                "", "none", false, MovementBrainBank.FALLBACK_BRAIN_NAME);
    }
}
