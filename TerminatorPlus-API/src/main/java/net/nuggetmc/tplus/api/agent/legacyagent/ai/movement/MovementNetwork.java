package net.nuggetmc.tplus.api.agent.legacyagent.ai.movement;

import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Fully in-JVM feed-forward network for movement only. It exposes weights and
 * biases so the existing GA code can mutate/evaluate parameters later.
 */
public final class MovementNetwork {

    private final int[] layerSizes;
    private final double[][][] weights;
    private final double[][] biases;

    private MovementNetwork(int[] layerSizes, double[][][] weights, double[][] biases) {
        this.layerSizes = Arrays.copyOf(layerSizes, layerSizes.length);
        this.weights = copyWeights(weights);
        this.biases = copyBiases(biases);
    }

    public static MovementNetwork randomDefault() {
        return random(MovementNetworkShape.defaultLayers(), ThreadLocalRandom.current());
    }

    public static MovementNetwork random(int[] layerSizes, Random random) {
        validateLayerSizesOrThrow(layerSizes);
        Objects.requireNonNull(random, "random");

        double[][][] weights = new double[layerSizes.length - 1][][];
        double[][] biases = new double[layerSizes.length - 1][];
        for (int layer = 0; layer < layerSizes.length - 1; layer++) {
            int in = layerSizes[layer];
            int out = layerSizes[layer + 1];
            weights[layer] = new double[out][in];
            biases[layer] = new double[out];
            double scale = Math.sqrt(2.0 / Math.max(1, in));
            for (int node = 0; node < out; node++) {
                biases[layer][node] = sanitize((random.nextDouble() * 2.0 - 1.0) * scale);
                for (int input = 0; input < in; input++) {
                    weights[layer][node][input] = sanitize((random.nextDouble() * 2.0 - 1.0) * scale);
                }
            }
        }
        return new MovementNetwork(layerSizes, weights, biases);
    }

    public static MovementNetwork fromParameters(int[] layerSizes, double[][][] weights, double[][] biases) {
        validateLayerSizesOrThrow(layerSizes);
        MovementNetwork network = new MovementNetwork(layerSizes, weights, biases);
        Validation validation = network.validate(layerSizes[0], layerSizes[layerSizes.length - 1]);
        if (!validation.valid()) {
            throw new IllegalArgumentException(validation.reason());
        }
        return network;
    }

    public double[] evaluate(double[] input) {
        Validation validation = validate(input == null ? -1 : input.length, outputCount());
        if (!validation.valid()) {
            return new double[outputCount()];
        }

        double[] activations = sanitizeVector(input);
        for (int layer = 0; layer < weights.length; layer++) {
            double[] next = new double[layerSizes[layer + 1]];
            for (int node = 0; node < next.length; node++) {
                double sum = biases[layer][node];
                for (int i = 0; i < activations.length; i++) {
                    sum += weights[layer][node][i] * activations[i];
                }
                next[node] = sanitize(Math.tanh(sanitize(sum)));
            }
            activations = next;
        }
        return activations;
    }

    public Validation validate(int expectedInputs, int expectedOutputs) {
        if (layerSizes.length < 2) return Validation.invalid("too-few-layers");
        if (expectedInputs >= 0 && inputCount() != expectedInputs) {
            return Validation.invalid("input-count " + inputCount() + "!=" + expectedInputs);
        }
        if (expectedOutputs >= 0 && outputCount() != expectedOutputs) {
            return Validation.invalid("output-count " + outputCount() + "!=" + expectedOutputs);
        }
        if (weights.length != layerSizes.length - 1 || biases.length != layerSizes.length - 1) {
            return Validation.invalid("layer-array-count");
        }
        for (int layer = 0; layer < layerSizes.length - 1; layer++) {
            int in = layerSizes[layer];
            int out = layerSizes[layer + 1];
            if (weights[layer] == null || weights[layer].length != out) {
                return Validation.invalid("weight-layer-" + layer);
            }
            if (biases[layer] == null || biases[layer].length != out) {
                return Validation.invalid("bias-layer-" + layer);
            }
            for (int node = 0; node < out; node++) {
                if (weights[layer][node] == null || weights[layer][node].length != in) {
                    return Validation.invalid("weight-node-" + layer + "-" + node);
                }
                if (!Double.isFinite(biases[layer][node])) {
                    return Validation.invalid("bias-nonfinite-" + layer + "-" + node);
                }
                for (int i = 0; i < in; i++) {
                    if (!Double.isFinite(weights[layer][node][i])) {
                        return Validation.invalid("weight-nonfinite-" + layer + "-" + node + "-" + i);
                    }
                }
            }
        }
        return Validation.ok();
    }

    public int inputCount() {
        return layerSizes[0];
    }

    public int outputCount() {
        return layerSizes[layerSizes.length - 1];
    }

    public int[] layerSizes() {
        return Arrays.copyOf(layerSizes, layerSizes.length);
    }

    public double[][][] weights() {
        return copyWeights(weights);
    }

    public double[][] biases() {
        return copyBiases(biases);
    }

    public double weight(int layer, int node, int input) {
        return weights[layer][node][input];
    }

    public void setWeight(int layer, int node, int input, double value) {
        weights[layer][node][input] = sanitize(value);
    }

    public double bias(int layer, int node) {
        return biases[layer][node];
    }

    public void setBias(int layer, int node, double value) {
        biases[layer][node] = sanitize(value);
    }

    public int parameterCount() {
        int count = 0;
        for (int layer = 0; layer < weights.length; layer++) {
            count += biases[layer].length;
            for (double[] nodeWeights : weights[layer]) {
                count += nodeWeights.length;
            }
        }
        return count;
    }

    public double[] flattenedWeights() {
        int count = 0;
        for (double[][] layer : weights) {
            for (double[] node : layer) count += node.length;
        }
        double[] flat = new double[count];
        int index = 0;
        for (double[][] layer : weights) {
            for (double[] node : layer) {
                for (double value : node) flat[index++] = value;
            }
        }
        return flat;
    }

    public double[] flattenedBiases() {
        int count = 0;
        for (double[] layer : biases) count += layer.length;
        double[] flat = new double[count];
        int index = 0;
        for (double[] layer : biases) {
            for (double value : layer) flat[index++] = value;
        }
        return flat;
    }

    private static void validateLayerSizesOrThrow(int[] layerSizes) {
        if (layerSizes == null || layerSizes.length < 2) {
            throw new IllegalArgumentException("MovementNetwork needs at least input and output layers");
        }
        for (int size : layerSizes) {
            if (size <= 0) throw new IllegalArgumentException("Layer sizes must be positive");
        }
    }

    private static double[] sanitizeVector(double[] values) {
        double[] result = Arrays.copyOf(values, values.length);
        for (int i = 0; i < result.length; i++) {
            result[i] = sanitize(result[i]);
        }
        return result;
    }

    private static double sanitize(double value) {
        if (!Double.isFinite(value)) return 0.0;
        return Math.max(-1_000.0, Math.min(1_000.0, value));
    }

    private static double[][][] copyWeights(double[][][] source) {
        if (source == null) return new double[0][][];
        double[][][] copy = new double[source.length][][];
        for (int layer = 0; layer < source.length; layer++) {
            if (source[layer] == null) continue;
            copy[layer] = new double[source[layer].length][];
            for (int node = 0; node < source[layer].length; node++) {
                copy[layer][node] = source[layer][node] == null
                        ? null
                        : Arrays.copyOf(source[layer][node], source[layer][node].length);
            }
        }
        return copy;
    }

    private static double[][] copyBiases(double[][] source) {
        if (source == null) return new double[0][];
        double[][] copy = new double[source.length][];
        for (int layer = 0; layer < source.length; layer++) {
            copy[layer] = source[layer] == null ? null : Arrays.copyOf(source[layer], source[layer].length);
        }
        return copy;
    }

    public record Validation(boolean valid, String reason) {
        public static Validation ok() {
            return new Validation(true, "ok");
        }

        public static Validation invalid(String reason) {
            return new Validation(false, reason == null || reason.isBlank() ? "invalid" : reason);
        }
    }
}
