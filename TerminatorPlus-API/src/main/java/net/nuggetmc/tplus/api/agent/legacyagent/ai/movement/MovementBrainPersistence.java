package net.nuggetmc.tplus.api.agent.legacyagent.ai.movement;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

/**
 * JSON persistence for movement-controller brains only. Legacy full-replacement
 * neural networks use their existing profile path and are not read here.
 */
public final class MovementBrainPersistence {

    public static final int SCHEMA_VERSION = 1;
    public static final String MODE = "movement-controller";
    public static final String INPUT_SCHEMA = "MovementInput:v1:30";
    public static final String OUTPUT_SCHEMA = "MovementOutput:v1:8";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(java.time.ZoneOffset.UTC);

    private MovementBrainPersistence() {}

    public static LoadResult load(Plugin plugin, MovementTrainingConfig config) {
        MovementTrainingConfig safeConfig = config == null ? MovementTrainingConfig.load(plugin) : config;
        return load(safeConfig.brainPath(plugin), safeConfig.movementLayerShape());
    }

    public static LoadResult load(Path path, int[] expectedShape) {
        Objects.requireNonNull(path, "path");
        int[] shape = expectedShape == null ? MovementNetworkShape.defaultLayers() : expectedShape.clone();
        if (!Files.exists(path)) {
            return LoadResult.missing(path);
        }

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
            int schemaVersion = getInt(root, "schemaVersion", -1);
            if (schemaVersion != SCHEMA_VERSION) {
                return LoadResult.invalid(path, "schema-version " + schemaVersion + "!=" + SCHEMA_VERSION, null);
            }
            String mode = getString(root, "mode", "");
            if (!MODE.equals(mode)) {
                return LoadResult.invalid(path, "mode " + mode + "!=" + MODE, null);
            }
            int[] fileShape = readIntArray(root.get("shape"));
            if (!Arrays.equals(fileShape, shape)) {
                return LoadResult.invalid(path, "shape " + Arrays.toString(fileShape)
                        + "!=" + Arrays.toString(shape), metadata(root, path));
            }
            double[][][] weights = readWeights(root.get("weights"), fileShape);
            double[][] biases = readBiases(root.get("biases"), fileShape);
            MovementNetwork network = MovementNetwork.fromParameters(fileShape, weights, biases);
            MovementNetwork.Validation validation = network.validate(
                    MovementNetworkShape.INPUT_COUNT,
                    MovementNetworkShape.OUTPUT_COUNT
            );
            if (!validation.valid()) {
                return LoadResult.invalid(path, validation.reason(), metadata(root, path));
            }
            return LoadResult.loaded(path, network, metadata(root, path));
        } catch (IllegalArgumentException | IllegalStateException | JsonSyntaxException e) {
            Path backup = backup(path, "corrupt");
            return LoadResult.corrupt(path, e.getMessage(), backup);
        } catch (IOException e) {
            return LoadResult.invalid(path, e.getMessage(), null);
        }
    }

    public static SaveResult save(
            Plugin plugin,
            MovementTrainingConfig config,
            MovementNetwork network,
            TrainingMetadata metadata
    ) {
        MovementTrainingConfig safeConfig = config == null ? MovementTrainingConfig.load(plugin) : config;
        return save(safeConfig.brainPath(plugin), network, withConfig(metadata, safeConfig));
    }

    public static SaveResult save(Path path, MovementNetwork network, TrainingMetadata metadata) {
        Objects.requireNonNull(path, "path");
        if (!MovementNetworkGenetics.isValid(network)) {
            return SaveResult.failed(path, "invalid movement network");
        }

        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Path temp = path.resolveSibling(path.getFileName() + ".tmp");
            try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                GSON.toJson(toJson(network, metadata), writer);
            }

            try {
                Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
            return SaveResult.saved(path);
        } catch (IOException e) {
            return SaveResult.failed(path, e.getMessage());
        }
    }

    public static ResetResult reset(Plugin plugin, MovementTrainingConfig config, Random random) {
        MovementTrainingConfig safeConfig = config == null ? MovementTrainingConfig.load(plugin) : config;
        Path path = safeConfig.brainPath(plugin);
        Path backup = null;
        if (Files.exists(path)) {
            backup = backup(path, "bak");
            if (backup == null) {
                return ResetResult.failed(path, null, "failed to back up existing brain");
            }
        }

        MovementNetwork fresh = MovementNetwork.random(safeConfig.movementLayerShape(), random);
        TrainingMetadata metadata = TrainingMetadata.reset(safeConfig);
        SaveResult save = save(path, fresh, metadata);
        if (!save.saved()) {
            if (backup != null && Files.exists(backup) && !Files.exists(path)) {
                try {
                    Files.move(backup, path);
                    backup = null;
                } catch (IOException ignored) {
                    // Keep the backup path in the result so admins know where
                    // the previous brain was preserved.
                }
            }
            return ResetResult.failed(path, backup, save.message());
        }
        return ResetResult.reset(path, backup, fresh, metadata);
    }

    private static JsonObject toJson(MovementNetwork network, TrainingMetadata metadata) {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", SCHEMA_VERSION);
        root.addProperty("mode", MODE);
        root.addProperty("inputSchema", INPUT_SCHEMA);
        root.addProperty("outputSchema", OUTPUT_SCHEMA);
        root.add("shape", intArray(network.layerSizes()));
        root.add("weights", weights(network.weights()));
        root.add("biases", biases(network.biases()));
        root.add("training", metadata == null ? TrainingMetadata.manual().toJson() : metadata.toJson());
        return root;
    }

    private static TrainingMetadata metadata(JsonObject root, Path path) {
        JsonObject training = root.has("training") && root.get("training").isJsonObject()
                ? root.getAsJsonObject("training")
                : new JsonObject();
        return TrainingMetadata.fromJson(training, path);
    }

    private static TrainingMetadata withConfig(TrainingMetadata metadata, MovementTrainingConfig config) {
        TrainingMetadata base = metadata == null ? TrainingMetadata.manual() : metadata;
        return new TrainingMetadata(
                base.generation(),
                base.bestFitness(),
                base.averageFitness(),
                base.timestamp(),
                config == null ? base.configHash() : config.configHash(),
                config == null ? base.loadoutMix() : config.loadoutSummary(),
                base.source()
        );
    }

    private static JsonArray intArray(int[] values) {
        JsonArray array = new JsonArray();
        for (int value : values) array.add(value);
        return array;
    }

    private static JsonArray weights(double[][][] values) {
        JsonArray layers = new JsonArray();
        for (double[][] layer : values) {
            JsonArray layerArray = new JsonArray();
            for (double[] node : layer) {
                JsonArray nodeArray = new JsonArray();
                for (double value : node) nodeArray.add(safe(value));
                layerArray.add(nodeArray);
            }
            layers.add(layerArray);
        }
        return layers;
    }

    private static JsonArray biases(double[][] values) {
        JsonArray layers = new JsonArray();
        for (double[] layer : values) {
            JsonArray layerArray = new JsonArray();
            for (double value : layer) layerArray.add(safe(value));
            layers.add(layerArray);
        }
        return layers;
    }

    private static int[] readIntArray(JsonElement element) {
        JsonArray array = element == null ? null : element.getAsJsonArray();
        if (array == null) throw new IllegalArgumentException("missing shape");
        int[] values = new int[array.size()];
        for (int i = 0; i < array.size(); i++) values[i] = array.get(i).getAsInt();
        return values;
    }

    private static double[][][] readWeights(JsonElement element, int[] shape) {
        JsonArray layers = element == null ? null : element.getAsJsonArray();
        if (layers == null || layers.size() != shape.length - 1) {
            throw new IllegalArgumentException("weight layer count");
        }
        double[][][] weights = new double[shape.length - 1][][];
        for (int layer = 0; layer < shape.length - 1; layer++) {
            JsonArray layerArray = layers.get(layer).getAsJsonArray();
            if (layerArray.size() != shape[layer + 1]) {
                throw new IllegalArgumentException("weight layer " + layer + " size");
            }
            weights[layer] = new double[shape[layer + 1]][shape[layer]];
            for (int node = 0; node < shape[layer + 1]; node++) {
                JsonArray nodeArray = layerArray.get(node).getAsJsonArray();
                if (nodeArray.size() != shape[layer]) {
                    throw new IllegalArgumentException("weight node " + layer + "-" + node + " size");
                }
                for (int input = 0; input < shape[layer]; input++) {
                    weights[layer][node][input] = safe(nodeArray.get(input).getAsDouble());
                }
            }
        }
        return weights;
    }

    private static double[][] readBiases(JsonElement element, int[] shape) {
        JsonArray layers = element == null ? null : element.getAsJsonArray();
        if (layers == null || layers.size() != shape.length - 1) {
            throw new IllegalArgumentException("bias layer count");
        }
        double[][] biases = new double[shape.length - 1][];
        for (int layer = 0; layer < shape.length - 1; layer++) {
            JsonArray layerArray = layers.get(layer).getAsJsonArray();
            if (layerArray.size() != shape[layer + 1]) {
                throw new IllegalArgumentException("bias layer " + layer + " size");
            }
            biases[layer] = new double[shape[layer + 1]];
            for (int node = 0; node < shape[layer + 1]; node++) {
                biases[layer][node] = safe(layerArray.get(node).getAsDouble());
            }
        }
        return biases;
    }

    private static double safe(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("non-finite brain value");
        return value;
    }

    private static String getString(JsonObject object, String key, String fallback) {
        return object.has(key) ? object.get(key).getAsString() : fallback;
    }

    private static int getInt(JsonObject object, String key, int fallback) {
        return object.has(key) ? object.get(key).getAsInt() : fallback;
    }

    private static double getDouble(JsonObject object, String key, double fallback) {
        if (!object.has(key) || object.get(key).isJsonNull()) return fallback;
        double value = object.get(key).getAsDouble();
        return Double.isFinite(value) ? value : fallback;
    }

    private static Path backup(Path path, String marker) {
        try {
            if (!Files.exists(path)) return null;
            Path candidate = path.resolveSibling(path.getFileName() + "." + marker + "-" + FILE_TIMESTAMP.format(Instant.now()));
            int suffix = 1;
            while (Files.exists(candidate)) {
                candidate = path.resolveSibling(path.getFileName() + "." + marker + "-" + FILE_TIMESTAMP.format(Instant.now()) + "-" + suffix++);
            }
            Files.move(path, candidate);
            return candidate;
        } catch (IOException e) {
            return null;
        }
    }

    public record TrainingMetadata(
            int generation,
            double bestFitness,
            double averageFitness,
            String timestamp,
            String configHash,
            String loadoutMix,
            String source
    ) {
        public static TrainingMetadata training(int generation, double bestFitness, double averageFitness, MovementTrainingConfig config) {
            return new TrainingMetadata(
                    generation,
                    safe(bestFitness),
                    safe(averageFitness),
                    Instant.now().toString(),
                    config == null ? "" : config.configHash(),
                    config == null ? "" : config.loadoutSummary(),
                    "movement-training"
            );
        }

        public static TrainingMetadata reset(MovementTrainingConfig config) {
            return new TrainingMetadata(
                    0,
                    0.0,
                    0.0,
                    Instant.now().toString(),
                    config == null ? "" : config.configHash(),
                    config == null ? "" : config.loadoutSummary(),
                    "reset"
            );
        }

        public static TrainingMetadata manual() {
            return new TrainingMetadata(0, 0.0, 0.0, Instant.now().toString(), "", "", "manual");
        }

        private static TrainingMetadata fromJson(JsonObject object, Path path) {
            return new TrainingMetadata(
                    getInt(object, "generation", 0),
                    getDouble(object, "bestFitness", 0.0),
                    getDouble(object, "averageFitness", 0.0),
                    getString(object, "timestamp", ""),
                    getString(object, "configHash", ""),
                    getString(object, "loadoutMix", ""),
                    getString(object, "source", path == null ? "" : path.toString())
            );
        }

        private JsonObject toJson() {
            JsonObject object = new JsonObject();
            object.addProperty("generation", generation);
            object.addProperty("bestFitness", safe(bestFitness));
            object.addProperty("averageFitness", safe(averageFitness));
            object.addProperty("timestamp", timestamp);
            object.addProperty("configHash", configHash);
            object.addProperty("loadoutMix", loadoutMix);
            object.addProperty("source", source);
            return object;
        }
    }

    public record LoadResult(
            boolean loaded,
            boolean missing,
            boolean corrupt,
            Path path,
            MovementNetwork network,
            TrainingMetadata metadata,
            String message,
            Path backupPath
    ) {
        static LoadResult loaded(Path path, MovementNetwork network, TrainingMetadata metadata) {
            return new LoadResult(true, false, false, path, network, metadata, "loaded", null);
        }

        static LoadResult missing(Path path) {
            return new LoadResult(false, true, false, path, null, null, "missing", null);
        }

        static LoadResult invalid(Path path, String reason, TrainingMetadata metadata) {
            return new LoadResult(false, false, false, path, null, metadata, reason, null);
        }

        static LoadResult corrupt(Path path, String reason, Path backupPath) {
            String message = backupPath == null ? reason : reason + "; backed up to " + backupPath;
            return new LoadResult(false, false, true, path, null, null, message, backupPath);
        }
    }

    public record SaveResult(boolean saved, Path path, String message) {
        static SaveResult saved(Path path) {
            return new SaveResult(true, path, "saved");
        }

        static SaveResult failed(Path path, String message) {
            return new SaveResult(false, path, message == null || message.isBlank() ? "save failed" : message);
        }
    }

    public record ResetResult(
            boolean reset,
            Path path,
            Path backupPath,
            MovementNetwork network,
            TrainingMetadata metadata,
            String message
    ) {
        static ResetResult reset(Path path, Path backupPath, MovementNetwork network, TrainingMetadata metadata) {
            return new ResetResult(true, path, backupPath, network, metadata, "reset");
        }

        static ResetResult failed(Path path, Path backupPath, String message) {
            return new ResetResult(false, path, backupPath, null, null,
                    message == null || message.isBlank() ? "reset failed" : message);
        }
    }

    public record SaveFeedback(boolean saved, String message) {}
}
