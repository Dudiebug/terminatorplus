package net.nuggetmc.tplus.api.agent.legacyagent.ai.movement;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Streaming counters for Issue #10 Phase 1 route-league telemetry.
 *
 * <p>No per-tick sample history is retained here. Training records one label
 * at a time and this class keeps only aggregate counts for generation-level
 * summaries.
 */
public final class RouteLeagueTelemetry {
    private long totalTrainingSamples;
    private final EnumMap<LearnerRoute, Long> samplesByLearnerRoute = new EnumMap<>(LearnerRoute.class);
    private final Map<RouteOpponentKey, Long> samplesByRouteAndOpponent = new LinkedHashMap<>();
    private final Map<RouteScenarioKey, Long> samplesByRouteAndScenario = new LinkedHashMap<>();
    private final Map<RouteOpponentScenarioKey, Long> samplesByRouteOpponentScenario = new LinkedHashMap<>();
    private final EnumMap<MobilityReason, Long> mobilityReasonCounts = new EnumMap<>(MobilityReason.class);
    private final Map<String, Long> unknownLabelCounts = new LinkedHashMap<>();

    public void record(RouteLeagueSampleLabel label) {
        RouteLeagueSampleLabel safe = label == null
                ? new RouteLeagueSampleLabel(LearnerRoute.UNKNOWN, OpponentArchetype.UNKNOWN,
                OpponentPool.UNKNOWN, ScenarioType.UNKNOWN, MobilityReason.UNKNOWN, null, false)
                : label;
        totalTrainingSamples++;

        increment(samplesByLearnerRoute, safe.learnerRoute());
        increment(samplesByRouteAndOpponent, new RouteOpponentKey(safe.learnerRoute(), safe.opponentArchetype()));
        increment(samplesByRouteAndScenario, new RouteScenarioKey(safe.learnerRoute(), safe.scenarioType()));
        increment(samplesByRouteOpponentScenario,
                new RouteOpponentScenarioKey(safe.learnerRoute(), safe.opponentArchetype(), safe.scenarioType()));

        if (safe.learnerRoute() == LearnerRoute.MOBILITY) {
            increment(mobilityReasonCounts, safe.mobilityReason());
        }
        if (safe.learnerRoute() == LearnerRoute.UNKNOWN) {
            incrementUnknown("learner_route");
        }
        if (safe.opponentArchetype() == OpponentArchetype.UNKNOWN) {
            incrementUnknown("opponent_archetype");
        }
        if (safe.opponentPool() == OpponentPool.UNKNOWN) {
            incrementUnknown("opponent_pool");
        }
        if (safe.scenarioType() == ScenarioType.UNKNOWN) {
            incrementUnknown("scenario_type");
        }
        if (safe.learnerRoute() == LearnerRoute.MOBILITY
                && safe.mobilityReason() == MobilityReason.UNKNOWN) {
            incrementUnknown("mobility_reason");
        }
    }

    public long totalTrainingSamples() {
        return totalTrainingSamples;
    }

    public Map<LearnerRoute, Long> samplesByLearnerRoute() {
        return Map.copyOf(samplesByLearnerRoute);
    }

    public Map<String, Long> unknownLabelCounts() {
        return Map.copyOf(unknownLabelCounts);
    }

    public List<String> generationSummaryLines(int generation) {
        List<String> lines = new ArrayList<>();
        lines.add("[ROUTE-LEAGUE] gen=" + generation
                + " samples=" + totalTrainingSamples
                + " routes " + describeRoutes());

        for (LearnerRoute route : LearnerRoute.values()) {
            if (route == LearnerRoute.UNKNOWN) continue;
            long samples = samplesByLearnerRoute.getOrDefault(route, 0L);
            if (samples == 0L && route != LearnerRoute.MOBILITY) continue;
            lines.add("[ROUTE-LEAGUE] gen=" + generation
                    + " route=" + route.id()
                    + " samples=" + samples
                    + " opponents " + describeOpponents(route));
            lines.add("[ROUTE-LEAGUE] gen=" + generation
                    + " route=" + route.id()
                    + " scenarios " + describeScenarios(route));
        }

        lines.add("[ROUTE-LEAGUE] gen=" + generation
                + " mobility reasons " + describeMobilityReasons());
        if (!unknownLabelCounts.isEmpty()) {
            lines.add("[ROUTE-LEAGUE] gen=" + generation
                    + " unknowns " + describeMap(unknownLabelCounts));
        }
        return lines;
    }

    private String describeRoutes() {
        StringBuilder out = new StringBuilder();
        boolean first = true;
        for (LearnerRoute route : LearnerRoute.values()) {
            if (!first) out.append(' ');
            first = false;
            out.append(route.display()).append('=')
                    .append(samplesByLearnerRoute.getOrDefault(route, 0L));
        }
        return out.toString();
    }

    private String describeOpponents(LearnerRoute route) {
        StringBuilder out = new StringBuilder();
        boolean first = true;
        for (OpponentArchetype opponent : OpponentArchetype.values()) {
            if (!first) out.append(' ');
            first = false;
            out.append(opponent.display()).append('=')
                    .append(samplesByRouteAndOpponent.getOrDefault(new RouteOpponentKey(route, opponent), 0L));
        }
        return out.toString();
    }

    private String describeScenarios(LearnerRoute route) {
        StringBuilder out = new StringBuilder();
        boolean first = true;
        for (ScenarioType scenario : ScenarioType.values()) {
            if (!first) out.append(' ');
            first = false;
            out.append(scenario.id()).append('=')
                    .append(samplesByRouteAndScenario.getOrDefault(new RouteScenarioKey(route, scenario), 0L));
        }
        return out.toString();
    }

    private String describeMobilityReasons() {
        StringBuilder out = new StringBuilder();
        boolean first = true;
        for (MobilityReason reason : MobilityReason.values()) {
            if (!first) out.append(' ');
            first = false;
            out.append(reason.display()).append('=')
                    .append(mobilityReasonCounts.getOrDefault(reason, 0L));
        }
        return out.toString();
    }

    private String describeMap(Map<String, Long> counts) {
        StringBuilder out = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            if (!first) out.append(' ');
            first = false;
            out.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return out.toString();
    }

    private void incrementUnknown(String label) {
        unknownLabelCounts.merge(label, 1L, Long::sum);
    }

    private static <E extends Enum<E>> void increment(EnumMap<E, Long> counts, E key) {
        counts.merge(key, 1L, Long::sum);
    }

    private static <K> void increment(Map<K, Long> counts, K key) {
        counts.merge(key, 1L, Long::sum);
    }

    private record RouteOpponentKey(LearnerRoute route, OpponentArchetype opponent) {
    }

    private record RouteScenarioKey(LearnerRoute route, ScenarioType scenario) {
    }

    private record RouteOpponentScenarioKey(
            LearnerRoute route,
            OpponentArchetype opponent,
            ScenarioType scenario
    ) {
    }

    public enum LearnerRoute {
        GENERAL_FALLBACK("general_fallback", "general"),
        MELEE("melee", "melee"),
        MACE("mace", "mace"),
        SPEAR_MELEE("spear_melee", "spear"),
        TRIDENT_RANGED("trident_ranged", "trident"),
        EXPLOSIVE_SURVIVAL("explosive_survival", "explosive"),
        MOBILITY("mobility", "mobility"),
        PROJECTILE_RANGED("projectile_ranged", "projectile"),
        UNKNOWN("unknown", "unknown");

        private final String id;
        private final String display;

        LearnerRoute(String id, String display) {
            this.id = id;
            this.display = display;
        }

        public String id() {
            return id;
        }

        public String display() {
            return display;
        }

        public static LearnerRoute fromId(String id) {
            String normalized = normalize(id);
            for (LearnerRoute route : values()) {
                if (route.id.equals(normalized) || route.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                    return route;
                }
            }
            return UNKNOWN;
        }
    }

    public enum OpponentArchetype {
        GENERAL_FALLBACK("general_fallback", "general"),
        MELEE("melee", "melee"),
        MACE("mace", "mace"),
        SPEAR_MELEE("spear_melee", "spear"),
        TRIDENT_RANGED("trident_ranged", "trident"),
        EXPLOSIVE_SURVIVAL("explosive_survival", "explosive"),
        MOBILITY("mobility", "mobility"),
        LEGACY_NON_NN("legacy_non_nn", "legacy"),
        RANDOM_MIXED("random_mixed", "random"),
        MIRROR_SAME_ROUTE("mirror_same_route", "mirror"),
        UNKNOWN("unknown", "unknown");

        private final String id;
        private final String display;

        OpponentArchetype(String id, String display) {
            this.id = id;
            this.display = display;
        }

        public String id() {
            return id;
        }

        public String display() {
            return display;
        }

        public static OpponentArchetype fromId(String id) {
            String normalized = normalize(id);
            for (OpponentArchetype archetype : values()) {
                if (archetype.id.equals(normalized)
                        || archetype.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                    return archetype;
                }
            }
            return UNKNOWN;
        }
    }

    public enum OpponentPool {
        LEGACY("legacy"),
        CURRENT_BEST("current_best"),
        PREVIOUS_CHAMPION("previous_champion"),
        RANDOM_GENOME("random_genome"),
        LATEST_GENERATION("latest_generation"),
        MIRROR_SAME_ROUTE("mirror_same_route"),
        UNKNOWN("unknown");

        private final String id;

        OpponentPool(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    public enum ScenarioType {
        NEUTRAL_ENGAGE("neutral_engage"),
        GAP_CLOSE("gap_close"),
        ESCAPE_BAD_RANGE("escape_bad_range"),
        MAINTAIN_LOS("maintain_los"),
        LOST_LOS_RECOVER("lost_los_recover"),
        VERTICAL_SETUP("vertical_setup"),
        STUCK_RECOVERY("stuck_recovery"),
        HAZARD_AVOIDANCE("hazard_avoidance"),
        LOW_HP_SURVIVAL("low_hp_survival"),
        HANDOFF_SETUP("handoff_setup"),
        MIRROR_DUEL("mirror_duel"),
        COUNTER_PRESSURE("counter_pressure"),
        RECOVERY_AFTER_COMMIT("recovery_after_commit"),
        UNKNOWN("unknown");

        private final String id;

        ScenarioType(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    public enum MobilityReason {
        GAP_CLOSE("gap_close"),
        ESCAPE_BAD_RANGE("escape"),
        LOS_REPOSITION("los"),
        VERTICAL_SETUP("vertical"),
        RECOVER_STUCK("stuck"),
        HAZARD_AVOID("hazard"),
        HANDOFF_SETUP("handoff"),
        UNKNOWN("unknown");

        private final String display;

        MobilityReason(String display) {
            this.display = display;
        }

        public String display() {
            return display;
        }
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) return "";
        return value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }
}
