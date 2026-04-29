package net.nuggetmc.tplus.api.agent.legacyagent.ai.movement;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

/**
 * Safe config facade for movement-controller training. Missing or bad values
 * clamp to defaults so existing servers can start without a config migration.
 */
public final class MovementTrainingConfig {

    private static final List<String> DEFAULT_LOADOUTS = List.of(
            "sword", "axe", "smp", "mace", "trident", "spear", "pot",
            "windcharge", "skydiver", "hybrid", "crystalpvp", "anchorbomb", "pvp"
    );

    private final boolean enabled;
    private final String mode;
    private final int tickRate;
    private final int[] hiddenLayers;
    private final boolean fallbackToLegacyMovement;
    private final boolean legacyFullReplacementMode;
    private final double movementOutputScale;
    private final boolean holdPositionRespected;
    private final boolean debug;
    private final String brainPath;
    private final boolean autosaveBestBrain;
    private final boolean saveOnlyImprovedBrain;
    private final int populationSize;
    private final int generations;
    private final int tournamentSize;
    private final int eliteCount;
    private final double crossoverRate;
    private final double mutationRate;
    private final double mutationStrength;
    private final boolean adaptiveMutation;
    private final long randomSeed;
    private final int maxTrainingTicks;
    private final FitnessWeights fitnessWeights;
    private final List<WeightedLoadout> loadouts;

    private MovementTrainingConfig(
            boolean enabled,
            String mode,
            int tickRate,
            int[] hiddenLayers,
            boolean fallbackToLegacyMovement,
            boolean legacyFullReplacementMode,
            double movementOutputScale,
            boolean holdPositionRespected,
            boolean debug,
            String brainPath,
            boolean autosaveBestBrain,
            boolean saveOnlyImprovedBrain,
            int populationSize,
            int generations,
            int tournamentSize,
            int eliteCount,
            double crossoverRate,
            double mutationRate,
            double mutationStrength,
            boolean adaptiveMutation,
            long randomSeed,
            int maxTrainingTicks,
            FitnessWeights fitnessWeights,
            List<WeightedLoadout> loadouts
    ) {
        this.enabled = enabled;
        this.mode = mode;
        this.tickRate = tickRate;
        this.hiddenLayers = hiddenLayers.clone();
        this.fallbackToLegacyMovement = fallbackToLegacyMovement;
        this.legacyFullReplacementMode = legacyFullReplacementMode;
        this.movementOutputScale = movementOutputScale;
        this.holdPositionRespected = holdPositionRespected;
        this.debug = debug;
        this.brainPath = brainPath;
        this.autosaveBestBrain = autosaveBestBrain;
        this.saveOnlyImprovedBrain = saveOnlyImprovedBrain;
        this.populationSize = populationSize;
        this.generations = generations;
        this.tournamentSize = tournamentSize;
        this.eliteCount = eliteCount;
        this.crossoverRate = crossoverRate;
        this.mutationRate = mutationRate;
        this.mutationStrength = mutationStrength;
        this.adaptiveMutation = adaptiveMutation;
        this.randomSeed = randomSeed;
        this.maxTrainingTicks = maxTrainingTicks;
        this.fitnessWeights = fitnessWeights;
        this.loadouts = List.copyOf(loadouts);
    }

    public static MovementTrainingConfig load(Plugin plugin) {
        ConfigurationSection root = null;
        if (plugin instanceof JavaPlugin javaPlugin) {
            root = javaPlugin.getConfig().getConfigurationSection("ai");
        }
        ConfigurationSection network = section(root, "movement-network");
        ConfigurationSection training = section(root, "training");
        ConfigurationSection fitness = section(training, "fitness-weights");
        ConfigurationSection loadouts = section(training, "loadouts");

        FitnessWeights weights = new FitnessWeights(
                getDouble(fitness, "range-control", 300.0, 0.0, 2_000.0),
                getDouble(fitness, "range-urgency", 220.0, 0.0, 2_000.0),
                getDouble(fitness, "crit-setup", 75.0, 0.0, 1_000.0),
                getDouble(fitness, "sprint-hit", 45.0, 0.0, 1_000.0),
                getDouble(fitness, "hold-position", 35.0, 0.0, 1_000.0),
                getDouble(fitness, "circling", 90.0, 0.0, 1_000.0),
                getDouble(fitness, "retreat", 70.0, 0.0, 1_000.0),
                getDouble(fitness, "survival", 0.25, 0.0, 20.0),
                getDouble(fitness, "damage-dealt", 275.0, 0.0, 2_000.0),
                getDouble(fitness, "damage-taken-penalty", 4.0, 0.0, 500.0),
                getDouble(fitness, "stuck-penalty", 45.0, 0.0, 1_000.0),
                getDouble(fitness, "fallback-penalty", 35.0, 0.0, 1_000.0),
                getDouble(fitness, "oscillation-penalty", 12.0, 0.0, 500.0),
                getDouble(fitness, "jump-spam-penalty", 180.0, 0.0, 1_000.0),
                getDouble(fitness, "sprint-spam-penalty", 120.0, 0.0, 1_000.0),
                getDouble(fitness, "hold-violation-penalty", 85.0, 0.0, 1_000.0)
        );

        return new MovementTrainingConfig(
                getBoolean(network, "enabled", false),
                getString(network, "mode", "movement-controller"),
                getInt(network, "tick-rate", 1, 1, 20),
                getHiddenLayers(network),
                getBoolean(network, "fallback-to-legacy-movement", true),
                getBoolean(network, "legacy-full-replacement-mode", false),
                getDouble(network, "movement-output-scaling", 1.0, 0.1, 4.0),
                getBoolean(network, "hold-position-behavior", true),
                getBoolean(network, "debug", false),
                getString(network, "brain-path", "ai/brain.json"),
                getBoolean(network, "autosave-best-brain", true),
                getBoolean(network, "save-only-improved-brain", true),
                getInt(training, "population-size", MovementNetworkGenetics.DEFAULT_POPULATION,
                        MovementNetworkGenetics.MIN_POPULATION, MovementNetworkGenetics.MAX_POPULATION),
                getInt(training, "generations", 0, 0, 10_000),
                getInt(training, "tournament-size", MovementNetworkGenetics.TOURNAMENT_SIZE, 2, 32),
                getInt(training, "elite-count", MovementNetworkGenetics.ELITE_COUNT, 0, 64),
                getDouble(training, "crossover-rate", 1.0, 0.0, 1.0),
                getDouble(training, "mutation-rate", MovementNetworkGenetics.BASE_MUTATION_RATE, 0.0, 1.0),
                getDouble(training, "mutation-strength", MovementNetworkGenetics.BASE_MUTATION_STRENGTH, 0.0, 2.0),
                getBoolean(training, "adaptive-mutation", true),
                getLong(training, "random-seed", 0L),
                getInt(training, "max-training-ticks", 6_000, 0, 72_000),
                weights,
                getLoadouts(loadouts)
        );
    }

    public int[] movementLayerShape() {
        int[] shape = new int[hiddenLayers.length + 2];
        shape[0] = MovementNetworkShape.INPUT_COUNT;
        System.arraycopy(hiddenLayers, 0, shape, 1, hiddenLayers.length);
        shape[shape.length - 1] = MovementNetworkShape.OUTPUT_COUNT;
        return shape;
    }

    public String pickLoadout(Random random) {
        if (loadouts.isEmpty()) return "sword";
        int total = loadouts.stream().mapToInt(WeightedLoadout::weight).sum();
        int roll = random.nextInt(Math.max(1, total));
        int cursor = 0;
        for (WeightedLoadout loadout : loadouts) {
            cursor += loadout.weight();
            if (roll < cursor) return loadout.name();
        }
        return loadouts.get(0).name();
    }

    public String loadoutSummary() {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < loadouts.size(); i++) {
            if (i > 0) out.append(", ");
            WeightedLoadout loadout = loadouts.get(i);
            out.append(loadout.name()).append('=').append(loadout.weight());
        }
        return out.toString();
    }

    public Path brainPath(Plugin plugin) {
        Path configured = Path.of(brainPath);
        if (configured.isAbsolute()) return configured;
        if (plugin == null) return configured;
        return plugin.getDataFolder().toPath().resolve(configured).normalize();
    }

    public String configHash() {
        return Integer.toHexString(Objects.hash(
                enabled,
                mode,
                tickRate,
                Arrays.hashCode(movementLayerShape()),
                fallbackToLegacyMovement,
                legacyFullReplacementMode,
                movementOutputScale,
                holdPositionRespected,
                populationSize,
                generations,
                tournamentSize,
                eliteCount,
                crossoverRate,
                mutationRate,
                mutationStrength,
                adaptiveMutation,
                maxTrainingTicks,
                fitnessWeights,
                loadouts
        ));
    }

    public boolean enabled() { return enabled; }
    public String mode() { return mode; }
    public int tickRate() { return tickRate; }
    public boolean fallbackToLegacyMovement() { return fallbackToLegacyMovement; }
    public boolean legacyFullReplacementMode() { return legacyFullReplacementMode; }
    public double movementOutputScale() { return movementOutputScale; }
    public boolean holdPositionRespected() { return holdPositionRespected; }
    public boolean debug() { return debug; }
    public String brainPath() { return brainPath; }
    public boolean autosaveBestBrain() { return autosaveBestBrain; }
    public boolean saveOnlyImprovedBrain() { return saveOnlyImprovedBrain; }
    public int populationSize() { return populationSize; }
    public int generations() { return generations; }
    public int tournamentSize() { return tournamentSize; }
    public int eliteCount() { return eliteCount; }
    public double crossoverRate() { return crossoverRate; }
    public double mutationRate() { return mutationRate; }
    public double mutationStrength() { return mutationStrength; }
    public boolean adaptiveMutation() { return adaptiveMutation; }
    public long randomSeed() { return randomSeed; }
    public int maxTrainingTicks() { return maxTrainingTicks; }
    public FitnessWeights fitnessWeights() { return fitnessWeights; }
    public List<WeightedLoadout> loadouts() { return loadouts; }

    private static ConfigurationSection section(ConfigurationSection parent, String path) {
        return parent == null ? null : parent.getConfigurationSection(path);
    }

    private static boolean getBoolean(ConfigurationSection section, String path, boolean fallback) {
        return section == null ? fallback : section.getBoolean(path, fallback);
    }

    private static String getString(ConfigurationSection section, String path, String fallback) {
        String value = section == null ? null : section.getString(path);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static int getInt(ConfigurationSection section, String path, int fallback, int min, int max) {
        int value = section == null ? fallback : section.getInt(path, fallback);
        return Math.max(min, Math.min(max, value));
    }

    private static long getLong(ConfigurationSection section, String path, long fallback) {
        return section == null ? fallback : section.getLong(path, fallback);
    }

    private static double getDouble(ConfigurationSection section, String path, double fallback, double min, double max) {
        double value = section == null ? fallback : section.getDouble(path, fallback);
        if (!Double.isFinite(value)) value = fallback;
        return Math.max(min, Math.min(max, value));
    }

    private static int[] getHiddenLayers(ConfigurationSection section) {
        List<Integer> raw = section == null ? List.of(32, 24) : section.getIntegerList("hidden-layers");
        if (raw.isEmpty()) raw = List.of(32, 24);
        List<Integer> clamped = new ArrayList<>();
        for (int size : raw) {
            clamped.add(Math.max(4, Math.min(256, size)));
            if (clamped.size() >= 6) break;
        }
        return clamped.stream().mapToInt(Integer::intValue).toArray();
    }

    private static List<WeightedLoadout> getLoadouts(ConfigurationSection section) {
        List<WeightedLoadout> out = new ArrayList<>();
        if (section != null) {
            List<String> names = section.getStringList("pool");
            if (!names.isEmpty()) {
                for (String name : names) {
                    String normalized = normalizeLoadout(name);
                    if (!normalized.isBlank()) {
                        out.add(new WeightedLoadout(normalized, getInt(section, "weights." + normalized, 1, 1, 100)));
                    }
                }
            }
        }
        if (out.isEmpty()) {
            for (String name : DEFAULT_LOADOUTS) out.add(new WeightedLoadout(name, 1));
        }
        return out;
    }

    private static String normalizeLoadout(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
    }

    public record FitnessWeights(
            double rangeControl,
            double rangeUrgency,
            double critSetup,
            double sprintHit,
            double holdPosition,
            double circling,
            double retreat,
            double survival,
            double damageDealt,
            double damageTakenPenalty,
            double stuckPenalty,
            double fallbackPenalty,
            double oscillationPenalty,
            double jumpSpamPenalty,
            double sprintSpamPenalty,
            double holdViolationPenalty
    ) {}

    public record WeightedLoadout(String name, int weight) {}
}
