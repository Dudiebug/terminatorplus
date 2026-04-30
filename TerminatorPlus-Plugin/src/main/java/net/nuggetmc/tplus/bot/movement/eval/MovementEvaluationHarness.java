package net.nuggetmc.tplus.bot.movement.eval;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.nuggetmc.tplus.TerminatorPlus;
import net.nuggetmc.tplus.api.agent.legacyagent.ai.movement.CombatTrainingSnapshot;
import net.nuggetmc.tplus.api.agent.legacyagent.ai.movement.MovementBrainBank;
import net.nuggetmc.tplus.api.agent.legacyagent.ai.movement.MovementLoadoutSampler;
import net.nuggetmc.tplus.api.agent.legacyagent.ai.movement.MovementNetworkShape;
import net.nuggetmc.tplus.api.agent.legacyagent.ai.movement.MovementTrainingConfig;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

/**
 * Lightweight evaluation initializer/exporter for the movement-brain bank.
 * Full live duel scoring still requires an arena runner; this report makes the
 * configured ablation, route table, fallback state, seeds, scenarios, and metric
 * schema repeatable and inspectable.
 */
public final class MovementEvaluationHarness {
    public static final int REPORT_SCHEMA_VERSION = 1;

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .serializeNulls()
            .create();

    private final TerminatorPlus plugin;

    public MovementEvaluationHarness(TerminatorPlus plugin) {
        this.plugin = plugin;
    }

    public ExportResult evaluate(
            MovementBrainBank bank,
            MovementTrainingConfig config,
            String variantToken,
            String scenarioToken,
            List<Long> seeds
    ) throws IOException {
        EvaluationVariant variant = EvaluationVariant.fromToken(variantToken);
        List<ScenarioDefinition> scenarios = scenariosFor(scenarioToken);
        List<Long> safeSeeds = seeds == null || seeds.isEmpty() ? defaultSeeds() : List.copyOf(seeds);
        MovementTrainingConfig safeConfig = config == null ? MovementTrainingConfig.load(plugin) : config;

        List<ScenarioResult> results = new ArrayList<>();
        AggregateCounts aggregate = new AggregateCounts();
        for (ScenarioDefinition scenario : scenarios) {
            List<SeedResult> seedResults = new ArrayList<>();
            for (long seed : safeSeeds) {
                SeedResult seedResult = evaluateSeed(bank, variant, scenario, seed);
                seedResults.add(seedResult);
                aggregate.add(seedResult);
            }
            results.add(new ScenarioResult(
                    scenario.id(),
                    scenario.displayName(),
                    scenario.loadouts(),
                    scenario.notes(),
                    seedResults
            ));
        }

        Path exportPath = exportPath(variant, scenarioToken);
        EvaluationReport report = new EvaluationReport(
                REPORT_SCHEMA_VERSION,
                Instant.now().toString(),
                metadata(bank, safeConfig, variant, scenarioToken, safeSeeds, exportPath),
                variant.summary(),
                results,
                aggregate.summary(),
                rewardComponents(bank),
                notesFor(variant, bank),
                exportPath.toString()
        );
        writeAtomic(exportPath, GSON.toJson(report));
        return new ExportResult(report, exportPath);
    }

    public static List<String> variantIds() {
        return Arrays.stream(EvaluationVariant.values()).map(EvaluationVariant::id).toList();
    }

    public static List<String> scenarioIds() {
        List<String> ids = new ArrayList<>();
        ids.add("all");
        SCENARIOS.forEach(scenario -> ids.add(scenario.id()));
        ids.add("movement_balanced");
        return List.copyOf(ids);
    }

    public static boolean isKnownVariant(String token) {
        return EvaluationVariant.tryFromToken(token) != null;
    }

    public static boolean isKnownScenario(String token) {
        if (token == null || token.isBlank()) return true;
        String normalized = normalize(token);
        return "all".equals(normalized)
                || "movement_balanced".equals(normalized)
                || SCENARIOS.stream().anyMatch(scenario -> scenario.id().equals(normalized));
    }

    public static List<Long> parseSeeds(Collection<String> tokens) {
        List<Long> seeds = new ArrayList<>();
        if (tokens == null) return seeds;
        for (String token : tokens) {
            if (token == null) continue;
            for (String part : token.split(",")) {
                String trimmed = part.trim();
                if (trimmed.isBlank()) continue;
                seeds.add(Long.parseLong(trimmed));
            }
        }
        return seeds;
    }

    public static String describeCounts(Map<String, Integer> counts) {
        return MovementLoadoutSampler.describeCounts(counts);
    }

    private SeedResult evaluateSeed(
            MovementBrainBank bank,
            EvaluationVariant variant,
            ScenarioDefinition scenario,
            long seed
    ) {
        Random random = new Random(seed);
        List<String> loadouts = shuffledLoadouts(scenario.loadouts(), random);
        Map<String, Integer> loadoutDistribution = new LinkedHashMap<>();
        Map<String, Integer> requestedDistribution = new LinkedHashMap<>();
        Map<String, Integer> activeDistribution = new LinkedHashMap<>();
        List<RouteProbe> probes = new ArrayList<>();

        int fallbackCount = 0;
        int missingFallbackCount = 0;
        int routeSwitches = 0;
        String lastActive = "";
        for (String loadout : loadouts) {
            String requestedFamily = requestedFamilyFor(loadout, variant);
            RouteProbe probe = routeProbe(bank, variant, loadout, requestedFamily);
            probes.add(probe);

            loadoutDistribution.merge(loadout, 1, Integer::sum);
            requestedDistribution.merge(requestedFamily, 1, Integer::sum);
            activeDistribution.merge(probe.activeFamily(), 1, Integer::sum);
            if (probe.fallback()) fallbackCount++;
            if (probe.missingOrIncompatibleFallback()) missingFallbackCount++;
            if (!lastActive.isBlank() && !Objects.equals(lastActive, probe.activeFamily())) {
                routeSwitches++;
            }
            lastActive = probe.activeFamily();
        }

        int sampleCount = Math.max(1, loadouts.size());
        MetricSet metrics = MetricSet.forReportOnly(
                variant.supportStatus(),
                variant.metricStatus(),
                routeSwitches,
                routeSwitches / (double) sampleCount,
                fallbackCount,
                fallbackCount / (double) sampleCount,
                missingFallbackCount
        );
        if (!variant.probesRoutes()) {
            metrics = MetricSet.pending(variant.supportStatus(), variant.metricStatus());
        }

        return new SeedResult(
                seed,
                scenario.id(),
                sampleCount,
                loadoutDistribution,
                requestedDistribution,
                activeDistribution,
                metrics,
                probes
        );
    }

    private RouteProbe routeProbe(
            MovementBrainBank bank,
            EvaluationVariant variant,
            String loadout,
            String requestedFamily
    ) {
        if (!variant.probesRoutes()) {
            return new RouteProbe(loadout, requestedFamily, "pending", "", false,
                    false, variant.metricStatus());
        }
        if (bank == null) {
            return new RouteProbe(loadout, requestedFamily, MovementBrainBank.FALLBACK_BRAIN_NAME,
                    MovementBrainBank.FALLBACK_BRAIN_NAME, true, true, "missing-bank");
        }

        String family = variant == EvaluationVariant.GENERAL_BRAIN
                ? MovementBrainBank.FALLBACK_BRAIN_NAME
                : requestedFamily;
        MovementBrainBank.Selection selection = bank.select(family);
        MovementBrainBank.Brain brain = selection.brain();
        String activeFamily = brain == null ? MovementBrainBank.FALLBACK_BRAIN_NAME : brain.familyId();
        boolean missing = selection.fallback() && !"matched".equals(selection.reason());
        return new RouteProbe(loadout, requestedFamily, activeFamily, selection.brainName(),
                selection.fallback(), missing, selection.reason());
    }

    private String requestedFamilyFor(String loadout, EvaluationVariant variant) {
        if (variant == EvaluationVariant.GENERAL_BRAIN) return MovementBrainBank.FALLBACK_BRAIN_NAME;
        if (variant == EvaluationVariant.LEGACY_HAND_AUTHORED) return "legacy_hand_authored";
        String family = CombatTrainingSnapshot.familyForLoadout(loadout);
        return MovementTrainingConfig.normalizeFamilyId(family);
    }

    private List<Long> defaultSeeds() {
        List<Long> configured = plugin.getConfig().getLongList("ai.evaluation.default-seeds");
        if (!configured.isEmpty()) return List.copyOf(configured);
        return List.of(1337L, 7331L, 424242L);
    }

    private ReportMetadata metadata(
            MovementBrainBank bank,
            MovementTrainingConfig config,
            EvaluationVariant variant,
            String scenarioToken,
            List<Long> seeds,
            Path exportPath
    ) {
        return new ReportMetadata(
                plugin.getDescription().getVersion(),
                TerminatorPlus.REQUIRED_VERSION,
                TerminatorPlus.getMcVersion(),
                bank == null ? MovementBrainBank.MANIFEST_SCHEMA_VERSION : bank.manifestSchemaVersion(),
                bank == null ? MovementNetworkShape.OBSERVATION_SCHEMA_HASH : bank.observationSchemaHash(),
                bank == null ? MovementNetworkShape.ACTION_SCHEMA_HASH : bank.actionSchemaHash(),
                bank == null ? MovementBrainBank.ROUTING_TABLE_VERSION : bank.routingTableVersion(),
                seeds,
                variant.id(),
                normalize(scenarioToken == null || scenarioToken.isBlank()
                        ? plugin.getConfig().getString("ai.evaluation.default-scenario", "all")
                        : scenarioToken),
                config.effectiveLoadoutMix(),
                config.manifestPath(plugin).toString(),
                exportPath.getParent().toString(),
                MovementNetworkShape.BRANCH_FAMILY_IDS
        );
    }

    private AggregateSummary summary(AggregateCounts counts) {
        return counts.summary();
    }

    private Map<String, Map<String, Double>> rewardComponents(MovementBrainBank bank) {
        Map<String, Map<String, Double>> components = new LinkedHashMap<>();
        if (bank == null) return components;
        for (MovementBrainBank.Brain brain : bank.brains().values()) {
            Map<String, Double> family = components.computeIfAbsent(brain.familyId(), ignored -> new LinkedHashMap<>());
            brain.rolloutStats().metrics().forEach((key, value) -> {
                String prefix = "component." + brain.familyId() + ".";
                if (key.startsWith(prefix)) {
                    family.put(key.substring(prefix.length()), value);
                } else if (key.startsWith("family." + brain.familyId() + ".")) {
                    family.put(key.substring(("family." + brain.familyId() + ".").length()), value);
                } else if (key.startsWith("best.") || key.startsWith("generation")) {
                    family.put(key, value);
                }
            });
        }
        return components;
    }

    private List<String> notesFor(EvaluationVariant variant, MovementBrainBank bank) {
        List<String> notes = new ArrayList<>();
        notes.add("CombatDirector remains the sole combat owner; this harness never calls combat actions.");
        notes.add("Movement brains output movement only. Live win/damage metrics require an arena run and are null in report-only exports.");
        notes.add("Mixed movement training currently records per-family telemetry but updates general_fallback; curriculum mode updates the configured family brain.");
        if (variant.supportStatus() != VariantSupport.REPORT_ONLY) {
            notes.add(variant.metricStatus());
        }
        if (bank == null) {
            notes.add("No movement bank was loaded; route probes report missing-bank fallbacks.");
        } else if (!bank.missingRouteFamilies().isEmpty()) {
            notes.add("Missing optional expert brains route through " + bank.defaultBrainName() + ": "
                    + String.join(", ", bank.missingRouteFamilies()));
        }
        return List.copyOf(notes);
    }

    private List<ScenarioDefinition> scenariosFor(String scenarioToken) {
        String token = normalize(scenarioToken == null || scenarioToken.isBlank()
                ? plugin.getConfig().getString("ai.evaluation.default-scenario", "all")
                : scenarioToken);
        if ("all".equals(token)) return SCENARIOS;
        if ("movement_balanced".equals(token)) {
            MovementTrainingConfig config = MovementTrainingConfig.load(plugin);
            return List.of(new ScenarioDefinition("movement_balanced",
                    "Configured movement_balanced mix",
                    new ArrayList<>(config.loadoutMix("movement_balanced").keySet()),
                    List.of("Uses the configured default weighted training pool once per seed.")));
        }
        return SCENARIOS.stream()
                .filter(scenario -> scenario.id().equals(token))
                .findFirst()
                .map(scenario -> List.of(scenario))
                .orElse(SCENARIOS);
    }

    private Path exportPath(EvaluationVariant variant, String scenarioToken) {
        String configured = plugin.getConfig().getString("ai.evaluation.export-directory", "ai/movement/evaluations");
        Path directory = plugin.getDataFolder().toPath().resolve(configured);
        String scenario = normalize(scenarioToken == null || scenarioToken.isBlank() ? "all" : scenarioToken);
        String timestamp = Instant.now().toString().replace(':', '-');
        return directory.resolve("movement-eval-" + timestamp + "-" + variant.id() + "-" + scenario + ".json");
    }

    private static List<String> shuffledLoadouts(List<String> loadouts, Random random) {
        List<String> copy = new ArrayList<>(loadouts);
        if (copy.isEmpty()) copy.add("sword");
        for (int i = copy.size() - 1; i > 0; i--) {
            int swap = random.nextInt(i + 1);
            String tmp = copy.get(i);
            copy.set(i, copy.get(swap));
            copy.set(swap, tmp);
        }
        return copy;
    }

    private static void writeAtomic(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Path temp = path.resolveSibling(path.getFileName() + ".tmp-" + UUID.randomUUID());
        Files.writeString(temp, content);
        try {
            Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String normalize(String token) {
        if (token == null || token.isBlank()) return "all";
        return token.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private static final List<ScenarioDefinition> SCENARIOS = List.of(
            new ScenarioDefinition("sword_axe_melee", "Sword/axe melee",
                    List.of("sword", "axe", "smp"), List.of("Default melee kits.")),
            new ScenarioDefinition("mace", "Mace",
                    List.of("mace"), List.of("Mace with wind-charge launch pairing.")),
            new ScenarioDefinition("trident", "Trident",
                    List.of("trident"), List.of("Ranged trident kit.")),
            new ScenarioDefinition("mobility_heavy", "Mobility-heavy",
                    List.of("windcharge", "skydiver", "hybrid"), List.of("Wind charge, elytra, trident mobility.")),
            new ScenarioDefinition("crystal", "Crystal",
                    List.of("crystalpvp"), List.of("Low-weight explosive specialist kit.")),
            new ScenarioDefinition("anchor", "Anchor",
                    List.of("anchorbomb"), List.of("Low-weight anchor specialist kit.")),
            new ScenarioDefinition("sword_mace_trident", "Sword + mace + trident",
                    List.of("sword", "mace", "trident"), List.of("Mixed melee/mace/trident routing.")),
            new ScenarioDefinition("mace_wind_pearl", "Mace + wind charge + pearl",
                    List.of("mace", "windcharge", "vanilla"), List.of("Uses existing kits; vanilla contributes pearls.")),
            new ScenarioDefinition("trident_elytra_fireworks", "Trident + elytra + fireworks",
                    List.of("trident", "skydiver"), List.of("Skydiver supplies elytra and fireworks.")),
            new ScenarioDefinition("melee_crystal", "Melee + crystal",
                    List.of("sword", "axe", "crystalpvp"), List.of("Melee pressure plus crystal survival."))
    );

    private enum EvaluationVariant {
        LEGACY_HAND_AUTHORED("legacy", "Current hand-authored/legacy movement",
                VariantSupport.PENDING_LIVE_ARENA,
                "Pending: the report initializes this ablation, but forcing legacy movement in an arena runner is not implemented yet.",
                false),
        GENERAL_BRAIN("general_brain", "One general movement brain",
                VariantSupport.REPORT_ONLY,
                "Report-only: probes general_fallback routing and bank metadata; live fight metrics require an arena run.",
                true),
        WEAPON_FAMILY_ROUTING("weapon_family", "Weapon-family routing",
                VariantSupport.UNSUPPORTED,
                "Unsupported: the current architecture routes by CombatDirector branch family, not raw weapon family.",
                false),
        BRANCH_FAMILY_NO_LATCH("branch_family_no_latch", "Branch-family routing without commit latching",
                VariantSupport.UNSUPPORTED,
                "Unsupported: commit latching is built into the runtime router and cannot be disabled by this slice.",
                false),
        BRANCH_FAMILY_LATCHED("branch_family_latched", "Full branch-family routing with commit latching",
                VariantSupport.REPORT_ONLY,
                "Report-only: probes current branch-family route table and fallback state; live fight metrics require an arena run.",
                true);

        private final String id;
        private final String displayName;
        private final VariantSupport supportStatus;
        private final String metricStatus;
        private final boolean probesRoutes;

        EvaluationVariant(String id, String displayName, VariantSupport supportStatus, String metricStatus, boolean probesRoutes) {
            this.id = id;
            this.displayName = displayName;
            this.supportStatus = supportStatus;
            this.metricStatus = metricStatus;
            this.probesRoutes = probesRoutes;
        }

        String id() {
            return id;
        }

        VariantSupport supportStatus() {
            return supportStatus;
        }

        String metricStatus() {
            return metricStatus;
        }

        boolean probesRoutes() {
            return probesRoutes;
        }

        VariantSummary summary() {
            return new VariantSummary(id, displayName, supportStatus.name().toLowerCase(Locale.ROOT), metricStatus);
        }

        static EvaluationVariant fromToken(String token) {
            EvaluationVariant variant = tryFromToken(token);
            return variant == null ? BRANCH_FAMILY_LATCHED : variant;
        }

        static EvaluationVariant tryFromToken(String token) {
            if (token == null || token.isBlank()) return BRANCH_FAMILY_LATCHED;
            String normalized = normalize(token);
            return switch (normalized) {
                case "hand_authored", "legacy", "legacy_hand_authored", "current" -> LEGACY_HAND_AUTHORED;
                case "general", "general_fallback", "general_brain", "one_general" -> GENERAL_BRAIN;
                case "weapon", "weapon_family", "weapon_family_routing" -> WEAPON_FAMILY_ROUTING;
                case "branch_no_latch", "branch_family_no_latch", "branch_family_without_latch" -> BRANCH_FAMILY_NO_LATCH;
                case "branch", "branch_family", "branch_family_latched", "full", "full_branch_family" -> BRANCH_FAMILY_LATCHED;
                default -> null;
            };
        }
    }

    private enum VariantSupport {
        REPORT_ONLY,
        PENDING_LIVE_ARENA,
        UNSUPPORTED
    }

    private static final class AggregateCounts {
        private int samples;
        private int fallbackCount;
        private int missingFallbackCount;
        private int routeSwitches;
        private final Map<String, Integer> loadouts = new LinkedHashMap<>();
        private final Map<String, Integer> requestedFamilies = new LinkedHashMap<>();
        private final Map<String, Integer> activeFamilies = new LinkedHashMap<>();

        void add(SeedResult result) {
            samples += result.sampleCount();
            fallbackCount += result.metrics().fallbackCount() == null ? 0 : result.metrics().fallbackCount().intValue();
            missingFallbackCount += result.metrics().missingIncompatibleBrainFallbackCount() == null
                    ? 0
                    : result.metrics().missingIncompatibleBrainFallbackCount().intValue();
            routeSwitches += result.metrics().routeSwitches() == null ? 0 : result.metrics().routeSwitches().intValue();
            merge(loadouts, result.loadoutDistribution());
            merge(requestedFamilies, result.requestedBranchFamilyDistribution());
            merge(activeFamilies, result.activeBranchFamilyDistribution());
        }

        AggregateSummary summary() {
            double denominator = Math.max(1.0, samples);
            return new AggregateSummary(samples, loadouts, requestedFamilies, activeFamilies, fallbackCount,
                    fallbackCount / denominator, missingFallbackCount, routeSwitches, routeSwitches / denominator);
        }

        private static void merge(Map<String, Integer> target, Map<String, Integer> source) {
            source.forEach((key, value) -> target.merge(key, value, Integer::sum));
        }
    }

    private record ScenarioDefinition(String id, String displayName, List<String> loadouts, List<String> notes) {
    }

    public record ExportResult(EvaluationReport report, Path path) {
    }

    public record EvaluationReport(
            int schemaVersion,
            String generatedAt,
            ReportMetadata metadata,
            VariantSummary variant,
            List<ScenarioResult> scenarios,
            AggregateSummary aggregate,
            Map<String, Map<String, Double>> perFamilyRewardComponents,
            List<String> notes,
            String exportPath
    ) {
    }

    public record ReportMetadata(
            String pluginVersion,
            String paperTargetVersion,
            String serverMinecraftVersion,
            int movementManifestVersion,
            String observationSchemaHash,
            String actionSchemaHash,
            int bankRouteTableVersion,
            List<Long> evaluationSeeds,
            String evaluatedVariant,
            String evaluatedScenario,
            String evaluatedLoadoutMix,
            String movementManifestPath,
            String exportDirectory,
            List<String> movementBranchFamilies
    ) {
    }

    public record VariantSummary(String id, String displayName, String supportStatus, String metricStatus) {
    }

    public record ScenarioResult(
            String id,
            String displayName,
            List<String> loadouts,
            List<String> notes,
            List<SeedResult> seeds
    ) {
    }

    public record SeedResult(
            long seed,
            String scenario,
            int sampleCount,
            Map<String, Integer> loadoutDistribution,
            Map<String, Integer> requestedBranchFamilyDistribution,
            Map<String, Integer> activeBranchFamilyDistribution,
            MetricSet metrics,
            List<RouteProbe> routeProbes
    ) {
    }

    public record AggregateSummary(
            int sampleCount,
            Map<String, Integer> loadoutDistribution,
            Map<String, Integer> requestedBranchFamilyDistribution,
            Map<String, Integer> activeBranchFamilyDistribution,
            int fallbackCount,
            double fallbackRate,
            int missingIncompatibleBrainFallbackCount,
            int routeSwitches,
            double routeThrashRate
    ) {
    }

    public record RouteProbe(
            String loadout,
            String requestedFamily,
            String activeFamily,
            String brainName,
            boolean fallback,
            boolean missingOrIncompatibleFallback,
            String reason
    ) {
    }

    public record MetricSet(
            String supportStatus,
            String metricStatus,
            Double winRate,
            Double damageDelta,
            Double selfDamage,
            Double selfKoRate,
            Double timeInActiveDesiredRangeRate,
            Double routeSwitches,
            Double routeThrashRate,
            Double fallbackCount,
            Double fallbackRate,
            Double missingIncompatibleBrainFallbackCount,
            Double committedPhaseConversionRate,
            Double lockBreakStaleLockCount,
            Double maceSmashConnectRate,
            Double tridentHitRateAfterCharge,
            Double mobilityGapCloseSuccessRate,
            Double safeExplosiveExecutionRate,
            Double projectileHitInterruptSuccessRate
    ) {
        static MetricSet forReportOnly(
                VariantSupport support,
                String metricStatus,
                int routeSwitches,
                double routeThrashRate,
                int fallbackCount,
                double fallbackRate,
                int missingFallbackCount
        ) {
            return new MetricSet(
                    support.name().toLowerCase(Locale.ROOT),
                    metricStatus,
                    null,
                    null,
                    null,
                    null,
                    null,
                    (double) routeSwitches,
                    routeThrashRate,
                    (double) fallbackCount,
                    fallbackRate,
                    (double) missingFallbackCount,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        static MetricSet pending(VariantSupport support, String metricStatus) {
            return new MetricSet(
                    support.name().toLowerCase(Locale.ROOT),
                    metricStatus,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }
    }
}
