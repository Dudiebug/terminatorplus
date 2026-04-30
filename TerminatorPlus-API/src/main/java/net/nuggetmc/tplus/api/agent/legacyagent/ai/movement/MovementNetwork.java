package net.nuggetmc.tplus.api.agent.legacyagent.ai.movement;

import java.util.Arrays;
import java.util.Random;

public final class MovementNetwork {
    private final int[] layerSizes;
    private final double[] parameters;

    public MovementNetwork(int[] layerSizes, double[] parameters) {
        this.layerSizes = sanitizeShape(layerSizes);
        int count = parameterCount(this.layerSizes);
        this.parameters = parameters == null ? new double[count] : Arrays.copyOf(parameters, count);
    }

    public static MovementNetwork random(int[] layerSizes, Random random) {
        int[] shape = sanitizeShape(layerSizes);
        Random rng = random == null ? new Random() : random;
        double[] params = new double[parameterCount(shape)];
        for (int i = 0; i < params.length; i++) {
            params[i] = rng.nextDouble(-1.0, 1.0);
        }
        return new MovementNetwork(shape, params);
    }

    public double[] evaluate(double[] input) {
        if (!validate(MovementNetworkShape.INPUT_COUNT, MovementNetworkShape.OUTPUT_COUNT).valid()) {
            return new double[MovementNetworkShape.OUTPUT_COUNT];
        }
        double[] activations = input == null
                ? new double[layerSizes[0]]
                : Arrays.copyOf(input, layerSizes[0]);
        int offset = 0;
        for (int layer = 1; layer < layerSizes.length; layer++) {
            int previous = layerSizes[layer - 1];
            int next = layerSizes[layer];
            double[] out = new double[next];
            for (int n = 0; n < next; n++) {
                double sum = parameters[offset++];
                for (int p = 0; p < previous; p++) {
                    sum += activations[p] * parameters[offset++];
                }
                out[n] = Math.tanh(sum);
            }
            activations = out;
        }
        return activations;
    }

    public Validation validate(int expectedInputs, int expectedOutputs) {
        if (layerSizes.length < 2) return new Validation(false, "too-few-layers");
        if (layerSizes[0] != expectedInputs) return new Validation(false, "input-count");
        if (layerSizes[layerSizes.length - 1] != expectedOutputs) return new Validation(false, "output-count");
        if (parameters.length != parameterCount()) return new Validation(false, "parameter-count");
        for (double parameter : parameters) {
            if (!Double.isFinite(parameter)) return new Validation(false, "non-finite-parameter");
        }
        return new Validation(true, "");
    }

    public int[] layerSizes() {
        return Arrays.copyOf(layerSizes, layerSizes.length);
    }

    public double[] parameters() {
        return Arrays.copyOf(parameters, parameters.length);
    }

    public int parameterCount() {
        return parameterCount(layerSizes);
    }

    private static int[] sanitizeShape(int[] requested) {
        int[] shape = requested == null || requested.length < 2
                ? MovementNetworkShape.DEFAULT_LAYERS
                : requested;
        int[] copy = Arrays.copyOf(shape, shape.length);
        copy[0] = MovementNetworkShape.INPUT_COUNT;
        copy[copy.length - 1] = MovementNetworkShape.OUTPUT_COUNT;
        for (int i = 1; i < copy.length - 1; i++) {
            copy[i] = Math.max(1, copy[i]);
        }
        return copy;
    }

    private static int parameterCount(int[] shape) {
        int count = 0;
        for (int layer = 1; layer < shape.length; layer++) {
            count += shape[layer] * (shape[layer - 1] + 1);
        }
        return count;
    }

    public record Validation(boolean valid, String reason) {
    }
}
