package net.nuggetmc.tplus.api.agent.legacyagent.ai.movement;

/**
 * Read-only training signal for movement-controller GA fitness. These fields
 * describe movement cooperation with CombatDirector; they are not combat
 * permissions and cannot trigger attacks.
 */
public record MovementTrainingSnapshot(
        boolean available,
        double distance,
        double horizontalDistance,
        double desiredRange,
        double rangeUrgency,
        boolean wantsCritSetup,
        boolean wantsSprintHit,
        boolean wantsHoldPosition,
        boolean isCommitted,
        double commitProgress,
        boolean isSprinting,
        boolean justJumped,
        boolean isFalling,
        boolean isRetreating,
        boolean isCircling,
        double approachSpeed,
        boolean legalCritSetup,
        boolean legalSprintHitSetup,
        boolean holdPositionCompliant,
        boolean movementFallback
) {
    public static MovementTrainingSnapshot unavailable() {
        return new MovementTrainingSnapshot(
                false,
                0.0,
                0.0,
                0.0,
                0.0,
                false,
                false,
                false,
                false,
                0.0,
                false,
                false,
                false,
                false,
                false,
                0.0,
                false,
                false,
                true,
                true
        );
    }
}
