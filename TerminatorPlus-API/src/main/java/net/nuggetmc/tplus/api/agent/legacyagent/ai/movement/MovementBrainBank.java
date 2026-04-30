package net.nuggetmc.tplus.api.agent.legacyagent.ai.movement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable runtime view of the movement brain bank. Route keys are stable
 * movement-family ids; plugin code maps them from MovementBranchFamily.
 */
public final class MovementBrainBank {
    public static final int MANIFEST_SCHEMA_VERSION = 1;
    public static final int ROUTING_TABLE_VERSION = 1;
    public static final String FALLBACK_BRAIN_NAME = "general_fallback";
    public static final String FALLBACK_FILE_FAMILY = "general";

    private final String defaultBrainName;
    private final int manifestSchemaVersion;
    private final int routingTableVersion;
    private final String observationSchemaHash;
    private final String actionSchemaHash;
    private final String trainingBuildVersion;
    private final Map<String, String> routes;
    private final Map<String, Brain> brains;
    private final MigrationMetadata migrationMetadata;

    public MovementBrainBank(
            String defaultBrainName,
            int manifestSchemaVersion,
            int routingTableVersion,
            String observationSchemaHash,
            String actionSchemaHash,
            String trainingBuildVersion,
            Map<String, String> routes,
            Map<String, Brain> brains,
            MigrationMetadata migrationMetadata
    ) {
        this.defaultBrainName = normalizeName(defaultBrainName, FALLBACK_BRAIN_NAME);
        this.manifestSchemaVersion = manifestSchemaVersion <= 0 ? MANIFEST_SCHEMA_VERSION : manifestSchemaVersion;
        this.routingTableVersion = routingTableVersion <= 0 ? ROUTING_TABLE_VERSION : routingTableVersion;
        this.observationSchemaHash = blankToDefault(observationSchemaHash, MovementNetworkShape.OBSERVATION_SCHEMA_HASH);
        this.actionSchemaHash = blankToDefault(actionSchemaHash, MovementNetworkShape.ACTION_SCHEMA_HASH);
        this.trainingBuildVersion = trainingBuildVersion == null ? "" : trainingBuildVersion;
        this.routes = Collections.unmodifiableMap(copyRoutes(routes, this.defaultBrainName));
        this.brains = Collections.unmodifiableMap(copyBrains(brains));
        this.migrationMetadata = migrationMetadata == null ? MigrationMetadata.none() : migrationMetadata;
    }

    public static MovementBrainBank empty(String defaultBrainName, String trainingBuildVersion) {
        String fallback = normalizeName(defaultBrainName, FALLBACK_BRAIN_NAME);
        return new MovementBrainBank(
                fallback,
                MANIFEST_SCHEMA_VERSION,
                ROUTING_TABLE_VERSION,
                MovementNetworkShape.OBSERVATION_SCHEMA_HASH,
                MovementNetworkShape.ACTION_SCHEMA_HASH,
                trainingBuildVersion,
                defaultRoutes(fallback),
                Map.of(),
                MigrationMetadata.none()
        );
    }

    public static MovementBrainBank singleFallback(
            MovementNetwork network,
            MovementBrainPersistence.TrainingMetadata metadata,
            String trainingBuildVersion,
            String source
    ) {
        Brain fallback = new Brain(
                FALLBACK_BRAIN_NAME,
                FALLBACK_BRAIN_NAME,
                MovementNetworkGenetics.copy(network),
                metadata == null ? MovementBrainPersistence.TrainingMetadata.manual() : metadata,
                NormalizationStats.none(),
                RolloutStats.empty(),
                source,
                trainingBuildVersion
        );
        return empty(FALLBACK_BRAIN_NAME, trainingBuildVersion).withBrain(fallback);
    }

    public MovementBrainBank withBrain(Brain brain) {
        if (brain == null || !MovementNetworkGenetics.isValid(brain.network())) {
            return this;
        }
        Map<String, Brain> copy = new LinkedHashMap<>(brains);
        copy.put(normalizeName(brain.name(), FALLBACK_BRAIN_NAME), brain);
        return new MovementBrainBank(defaultBrainName, manifestSchemaVersion, routingTableVersion,
                observationSchemaHash, actionSchemaHash, trainingBuildVersion, routes, copy, migrationMetadata);
    }

    public MovementBrainBank withRoutes(Map<String, String> routes) {
        return new MovementBrainBank(defaultBrainName, manifestSchemaVersion, routingTableVersion,
                observationSchemaHash, actionSchemaHash, trainingBuildVersion, routes, brains, migrationMetadata);
    }

    public MovementBrainBank withMigrationMetadata(MigrationMetadata migrationMetadata) {
        return new MovementBrainBank(defaultBrainName, manifestSchemaVersion, routingTableVersion,
                observationSchemaHash, actionSchemaHash, trainingBuildVersion, routes, brains, migrationMetadata);
    }

    public Selection select(String familyId) {
        String requested = normalizeFamily(familyId);
        String brainName = routes.getOrDefault(requested, defaultBrainName);
        Brain selected = brains.get(brainName);
        if (selected != null && MovementNetworkGenetics.isValid(selected.network())) {
            return new Selection(requested, brainName, selected, false, "matched");
        }

        Brain fallback = fallbackBrain();
        if (fallback != null && MovementNetworkGenetics.isValid(fallback.network())) {
            String reason = selected == null ? "missing:" + brainName : "invalid:" + brainName;
            return new Selection(requested, defaultBrainName, fallback, !Objects.equals(requested, fallback.familyId()), reason);
        }

        return new Selection(requested, defaultBrainName, null, true, "missing-fallback");
    }

    public Brain fallbackBrain() {
        Brain brain = brains.get(defaultBrainName);
        if (brain != null) return brain;
        return brains.get(FALLBACK_BRAIN_NAME);
    }

    public MovementNetwork fallbackNetwork() {
        Brain fallback = fallbackBrain();
        return fallback == null ? null : MovementNetworkGenetics.copy(fallback.network());
    }

    public MovementBrainPersistence.TrainingMetadata fallbackMetadata() {
        Brain fallback = fallbackBrain();
        return fallback == null ? MovementBrainPersistence.TrainingMetadata.manual() : fallback.metadata();
    }

    public boolean hasValidFallback() {
        return MovementNetworkGenetics.isValid(fallbackNetwork());
    }

    public List<String> availableBrainNames() {
        return List.copyOf(brains.keySet());
    }

    public List<String> missingRouteFamilies() {
        List<String> missing = new ArrayList<>();
        for (String family : MovementNetworkShape.BRANCH_FAMILY_IDS) {
            String brainName = routes.getOrDefault(family, defaultBrainName);
            if (!brains.containsKey(brainName)) {
                missing.add(family);
            }
        }
        return List.copyOf(missing);
    }

    public static Map<String, String> defaultRoutes(String fallbackBrainName) {
        String fallback = normalizeName(fallbackBrainName, FALLBACK_BRAIN_NAME);
        Map<String, String> result = new LinkedHashMap<>();
        for (String family : MovementNetworkShape.BRANCH_FAMILY_IDS) {
            result.put(family, family.equals(FALLBACK_BRAIN_NAME) ? fallback : family);
        }
        return result;
    }

    public static String normalizeFamily(String value) {
        return normalizeName(value, FALLBACK_BRAIN_NAME);
    }

    public static String normalizeName(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        return value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    public String defaultBrainName() {
        return defaultBrainName;
    }

    public int manifestSchemaVersion() {
        return manifestSchemaVersion;
    }

    public int routingTableVersion() {
        return routingTableVersion;
    }

    public String observationSchemaHash() {
        return observationSchemaHash;
    }

    public String actionSchemaHash() {
        return actionSchemaHash;
    }

    public String trainingBuildVersion() {
        return trainingBuildVersion;
    }

    public Map<String, String> routes() {
        return routes;
    }

    public Map<String, Brain> brains() {
        return brains;
    }

    public MigrationMetadata migrationMetadata() {
        return migrationMetadata;
    }

    private static Map<String, String> copyRoutes(Map<String, String> routes, String defaultBrainName) {
        Map<String, String> copy = new LinkedHashMap<>(defaultRoutes(defaultBrainName));
        if (routes != null) {
            routes.forEach((family, brainName) ->
                    copy.put(normalizeFamily(family), normalizeName(brainName, defaultBrainName)));
        }
        return copy;
    }

    private static Map<String, Brain> copyBrains(Map<String, Brain> brains) {
        Map<String, Brain> copy = new LinkedHashMap<>();
        if (brains != null) {
            brains.forEach((name, brain) -> {
                if (brain != null) {
                    copy.put(normalizeName(name, FALLBACK_BRAIN_NAME), brain.copy());
                }
            });
        }
        return copy;
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public record Selection(
            String requestedFamily,
            String brainName,
            Brain brain,
            boolean fallback,
            String reason
    ) {
        public MovementNetwork network() {
            return brain == null ? null : MovementNetworkGenetics.copy(brain.network());
        }
    }

    public record Brain(
            String name,
            String familyId,
            MovementNetwork network,
            MovementBrainPersistence.TrainingMetadata metadata,
            NormalizationStats normalization,
            RolloutStats rolloutStats,
            String source,
            String trainingBuildVersion
    ) {
        public Brain {
            name = normalizeName(name, FALLBACK_BRAIN_NAME);
            familyId = normalizeFamily(familyId);
            network = MovementNetworkGenetics.copy(network);
            metadata = metadata == null ? MovementBrainPersistence.TrainingMetadata.manual() : metadata;
            normalization = normalization == null ? NormalizationStats.none() : normalization.copy();
            rolloutStats = rolloutStats == null ? RolloutStats.empty() : rolloutStats.copy();
            source = source == null ? "" : source;
            trainingBuildVersion = trainingBuildVersion == null ? "" : trainingBuildVersion;
        }

        public Brain copy() {
            return new Brain(name, familyId, network, metadata, normalization, rolloutStats, source, trainingBuildVersion);
        }
    }

    public record NormalizationStats(String mode, double[] mean, double[] scale) {
        public NormalizationStats {
            mode = mode == null || mode.isBlank() ? "pre_normalized" : mode;
            mean = mean == null ? new double[0] : Arrays.copyOf(mean, mean.length);
            scale = scale == null ? new double[0] : Arrays.copyOf(scale, scale.length);
        }

        public static NormalizationStats none() {
            return new NormalizationStats("pre_normalized", new double[0], new double[0]);
        }

        @Override
        public double[] mean() {
            return Arrays.copyOf(mean, mean.length);
        }

        @Override
        public double[] scale() {
            return Arrays.copyOf(scale, scale.length);
        }

        public NormalizationStats copy() {
            return new NormalizationStats(mode, mean, scale);
        }
    }

    public record RolloutStats(Map<String, Double> metrics) {
        public RolloutStats {
            Map<String, Double> copy = new LinkedHashMap<>();
            if (metrics != null) {
                metrics.forEach((key, value) -> {
                    if (key != null && value != null && Double.isFinite(value)) {
                        copy.put(key, value);
                    }
                });
            }
            metrics = Collections.unmodifiableMap(copy);
        }

        public static RolloutStats empty() {
            return new RolloutStats(Map.of());
        }

        public RolloutStats copy() {
            return new RolloutStats(metrics);
        }
    }

    public record MigrationMetadata(
            boolean legacyImportAttempted,
            boolean legacyImported,
            String legacyPath,
            String result,
            String timestamp
    ) {
        public MigrationMetadata {
            legacyPath = legacyPath == null ? "" : legacyPath;
            result = result == null ? "" : result;
            timestamp = timestamp == null ? "" : timestamp;
        }

        public static MigrationMetadata none() {
            return new MigrationMetadata(false, false, "", "", "");
        }
    }
}
