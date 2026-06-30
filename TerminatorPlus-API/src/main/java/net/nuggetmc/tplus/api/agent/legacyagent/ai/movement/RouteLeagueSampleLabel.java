package net.nuggetmc.tplus.api.agent.legacyagent.ai.movement;

import java.util.Locale;

/**
 * Immutable labels for Issue #10 Phase 1 route-league telemetry.
 *
 * <p>This is observability-only scaffolding: labels are derived from already
 * available movement/combat snapshots and must not influence scoring, routing,
 * selection, autosave, mutation, crossover, or combat ownership.
 */
public record RouteLeagueSampleLabel(
        RouteLeagueTelemetry.LearnerRoute learnerRoute,
        RouteLeagueTelemetry.OpponentArchetype opponentArchetype,
        RouteLeagueTelemetry.OpponentPool opponentPool,
        RouteLeagueTelemetry.ScenarioType scenarioType,
        RouteLeagueTelemetry.MobilityReason mobilityReason,
        Long seed,
        boolean validRouteSample
) {
    public RouteLeagueSampleLabel {
        learnerRoute = learnerRoute == null ? RouteLeagueTelemetry.LearnerRoute.UNKNOWN : learnerRoute;
        opponentArchetype = opponentArchetype == null
                ? RouteLeagueTelemetry.OpponentArchetype.UNKNOWN
                : opponentArchetype;
        opponentPool = opponentPool == null ? RouteLeagueTelemetry.OpponentPool.UNKNOWN : opponentPool;
        scenarioType = scenarioType == null ? RouteLeagueTelemetry.ScenarioType.UNKNOWN : scenarioType;
        mobilityReason = mobilityReason == null ? RouteLeagueTelemetry.MobilityReason.UNKNOWN : mobilityReason;
    }

    public static RouteLeagueSampleLabel fromSnapshots(
            MovementTrainingSnapshot movement,
            CombatTrainingSnapshot learnerCombat,
            CombatTrainingSnapshot opponentCombat,
            String candidateTrainingFamily,
            boolean opponentUsesMovementController,
            RouteLeagueTelemetry.OpponentPool opponentPool,
            Long seed,
            double learnerHealth
    ) {
        MovementTrainingSnapshot safeMovement = movement == null
                ? MovementTrainingSnapshot.unavailable()
                : movement;
        CombatTrainingSnapshot safeOpponent = opponentCombat == null
                ? CombatTrainingSnapshot.unavailable()
                : opponentCombat;
        RouteLeagueTelemetry.LearnerRoute learnerRoute = learnerRoute(safeMovement, candidateTrainingFamily);
        RouteLeagueTelemetry.OpponentArchetype opponentArchetype =
                opponentArchetype(safeOpponent, opponentUsesMovementController);
        RouteLeagueTelemetry.ScenarioType scenarioType =
                scenarioType(safeMovement, learnerCombat, learnerHealth);
        RouteLeagueTelemetry.MobilityReason mobilityReason = learnerRoute == RouteLeagueTelemetry.LearnerRoute.MOBILITY
                ? mobilityReason(safeMovement, scenarioType)
                : RouteLeagueTelemetry.MobilityReason.UNKNOWN;
        boolean validRouteSample = safeMovement.available()
                && learnerRoute != RouteLeagueTelemetry.LearnerRoute.UNKNOWN;

        return new RouteLeagueSampleLabel(learnerRoute, opponentArchetype, opponentPool,
                scenarioType, mobilityReason, seed, validRouteSample);
    }

    private static RouteLeagueTelemetry.LearnerRoute learnerRoute(
            MovementTrainingSnapshot movement,
            String candidateTrainingFamily
    ) {
        if (movement != null && movement.available()) {
            RouteLeagueTelemetry.LearnerRoute route =
                    RouteLeagueTelemetry.LearnerRoute.fromId(movement.activeBranchFamily());
            if (route != RouteLeagueTelemetry.LearnerRoute.UNKNOWN) return route;
        }
        return RouteLeagueTelemetry.LearnerRoute.fromId(candidateTrainingFamily);
    }

    private static RouteLeagueTelemetry.OpponentArchetype opponentArchetype(
            CombatTrainingSnapshot opponent,
            boolean opponentUsesMovementController
    ) {
        if (!opponentUsesMovementController) {
            return RouteLeagueTelemetry.OpponentArchetype.LEGACY_NON_NN;
        }
        if (opponent == null || !opponent.available()) {
            return RouteLeagueTelemetry.OpponentArchetype.UNKNOWN;
        }
        return RouteLeagueTelemetry.OpponentArchetype.fromId(opponent.loadoutFamily());
    }

    private static RouteLeagueTelemetry.ScenarioType scenarioType(
            MovementTrainingSnapshot movement,
            CombatTrainingSnapshot combat,
            double learnerHealth
    ) {
        if (movement == null || !movement.available()) {
            return RouteLeagueTelemetry.ScenarioType.UNKNOWN;
        }

        String token = token(movement.playId() + " " + movement.plannedAction());
        if (containsAny(token, "lava", "fire", "water_douse", "tnt", "hazard")) {
            return RouteLeagueTelemetry.ScenarioType.HAZARD_AVOIDANCE;
        }
        if (containsAny(token, "aerial", "airborne", "wind_mace", "sky", "elytra")) {
            return RouteLeagueTelemetry.ScenarioType.VERTICAL_SETUP;
        }
        if (containsAny(token, "stuck", "unstick")) {
            return RouteLeagueTelemetry.ScenarioType.STUCK_RECOVERY;
        }
        if (containsAny(token, "pursue_gap_close", "gap_close")) {
            return RouteLeagueTelemetry.ScenarioType.GAP_CLOSE;
        }
        if (containsAny(token, "los", "line_of_sight")) {
            return RouteLeagueTelemetry.ScenarioType.MAINTAIN_LOS;
        }
        if (learnerHealth > 0.0 && learnerHealth <= 8.0) {
            return RouteLeagueTelemetry.ScenarioType.LOW_HP_SURVIVAL;
        }
        if (movement.movementLocked() && !isNone(movement.plannedAction())) {
            return RouteLeagueTelemetry.ScenarioType.HANDOFF_SETUP;
        }
        if (movement.committed() && movement.commitProgress() >= 0.75) {
            return RouteLeagueTelemetry.ScenarioType.RECOVERY_AFTER_COMMIT;
        }

        double tooFar = movement.distance() - movement.desiredRange();
        double tooClose = movement.desiredRange() - movement.distance();
        if (movement.rangeUrgency() >= 0.25 && tooFar > 1.5) {
            return RouteLeagueTelemetry.ScenarioType.GAP_CLOSE;
        }
        if (movement.rangeUrgency() >= 0.25 && tooClose > 1.0 && movement.retreating()) {
            return RouteLeagueTelemetry.ScenarioType.ESCAPE_BAD_RANGE;
        }
        if (combat != null && combat.available() && combat.damageTaken() > combat.damageDealt() + 4.0) {
            return RouteLeagueTelemetry.ScenarioType.COUNTER_PRESSURE;
        }
        if (!movement.movementLocked() && isNone(movement.plannedAction())) {
            return RouteLeagueTelemetry.ScenarioType.NEUTRAL_ENGAGE;
        }
        return RouteLeagueTelemetry.ScenarioType.UNKNOWN;
    }

    private static RouteLeagueTelemetry.MobilityReason mobilityReason(
            MovementTrainingSnapshot movement,
            RouteLeagueTelemetry.ScenarioType scenarioType
    ) {
        String token = token(movement.playId() + " " + movement.plannedAction());
        if (scenarioType == RouteLeagueTelemetry.ScenarioType.GAP_CLOSE) {
            return RouteLeagueTelemetry.MobilityReason.GAP_CLOSE;
        }
        if (scenarioType == RouteLeagueTelemetry.ScenarioType.ESCAPE_BAD_RANGE) {
            return RouteLeagueTelemetry.MobilityReason.ESCAPE_BAD_RANGE;
        }
        if (scenarioType == RouteLeagueTelemetry.ScenarioType.MAINTAIN_LOS
                || scenarioType == RouteLeagueTelemetry.ScenarioType.LOST_LOS_RECOVER) {
            return RouteLeagueTelemetry.MobilityReason.LOS_REPOSITION;
        }
        if (scenarioType == RouteLeagueTelemetry.ScenarioType.VERTICAL_SETUP) {
            return RouteLeagueTelemetry.MobilityReason.VERTICAL_SETUP;
        }
        if (scenarioType == RouteLeagueTelemetry.ScenarioType.STUCK_RECOVERY) {
            return RouteLeagueTelemetry.MobilityReason.RECOVER_STUCK;
        }
        if (scenarioType == RouteLeagueTelemetry.ScenarioType.HAZARD_AVOIDANCE) {
            return RouteLeagueTelemetry.MobilityReason.HAZARD_AVOID;
        }
        if (scenarioType == RouteLeagueTelemetry.ScenarioType.HANDOFF_SETUP
                || containsAny(token, "combo", "finisher", "pearl")) {
            return RouteLeagueTelemetry.MobilityReason.HANDOFF_SETUP;
        }
        return RouteLeagueTelemetry.MobilityReason.UNKNOWN;
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) return true;
        }
        return false;
    }

    private static boolean isNone(String value) {
        String token = token(value);
        return token.isBlank() || token.equals("none");
    }

    private static String token(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }
}
