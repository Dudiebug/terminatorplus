package net.nuggetmc.tplus.api.agent.legacyagent.ai.movement;

import java.util.Random;

public final class MovementNetworkGenetics {
    private MovementNetworkGenetics() {
    }

    public static MovementNetwork copy(MovementNetwork network) {
        if (network == null) return null;
        return new MovementNetwork(network.layerSizes(), network.parameters());
    }

    public static boolean isValid(MovementNetwork network) {
        return network != null
                && network.validate(MovementNetworkShape.INPUT_COUNT, MovementNetworkShape.OUTPUT_COUNT).valid();
    }

    public static MovementNetwork random(int[] layerSizes, Random random) {
        return MovementNetwork.random(layerSizes, random);
    }

    public static MovementNetwork mutate(MovementNetwork network, double mutationScale, Random random) {
        if (!isValid(network)) return null;
        Random rng = random == null ? new Random() : random;
        double sigma = Double.isFinite(mutationScale) && mutationScale > 0.0 ? mutationScale : 0.08;
        double[] parameters = network.parameters();
        for (int i = 0; i < parameters.length; i++) {
            parameters[i] = clamp(parameters[i] + rng.nextGaussian() * sigma);
        }
        return new MovementNetwork(network.layerSizes(), parameters);
    }

    public static MovementNetwork crossover(MovementNetwork left, MovementNetwork right, Random random) {
        if (!isValid(left)) return copy(right);
        if (!isValid(right)) return copy(left);
        int[] leftShape = left.layerSizes();
        int[] rightShape = right.layerSizes();
        if (leftShape.length != rightShape.length) return copy(left);
        for (int i = 0; i < leftShape.length; i++) {
            if (leftShape[i] != rightShape[i]) return copy(left);
        }

        Random rng = random == null ? new Random() : random;
        double[] a = left.parameters();
        double[] b = right.parameters();
        double[] child = new double[a.length];
        for (int i = 0; i < child.length; i++) {
            child[i] = rng.nextBoolean() ? a[i] : b[i];
        }
        return new MovementNetwork(leftShape, child);
    }

    private static double clamp(double value) {
        if (!Double.isFinite(value)) return 0.0;
        return Math.max(-5.0, Math.min(5.0, value));
    }
}
