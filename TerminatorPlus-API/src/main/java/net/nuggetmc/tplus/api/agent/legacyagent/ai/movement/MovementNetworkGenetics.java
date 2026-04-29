package net.nuggetmc.tplus.api.agent.legacyagent.ai.movement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Tournament selection, uniform crossover, and adaptive mutation for movement
 * networks. Operates only on movement weights/biases; combat logic is not part
 * of the genome.
 */
public final class MovementNetworkGenetics {

    public static final int DEFAULT_POPULATION = 120;
    public static final int MIN_POPULATION = 10;
    public static final int MAX_POPULATION = 240;
    public static final int ELITE_COUNT = 6;
    public static final int TOURNAMENT_SIZE = 5;
    public static final double BASE_MUTATION_RATE = 0.035;
    public static final double BASE_MUTATION_STRENGTH = 0.12;
    private static final double PARAMETER_LIMIT = 4.0;

    private MovementNetworkGenetics() {}

    public static int normalizePopulationSize(int requested) {
        if (requested <= 0) return DEFAULT_POPULATION;
        return Math.max(MIN_POPULATION, Math.min(MAX_POPULATION, requested));
    }

    public static List<MovementNetwork> randomPopulation(int size, Random random) {
        Objects.requireNonNull(random, "random");
        int count = normalizePopulationSize(size);
        List<MovementNetwork> networks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            networks.add(MovementNetwork.random(MovementNetworkShape.defaultLayers(), random));
        }
        return networks;
    }

    public static List<MovementNetwork> nextGeneration(
            List<ScoredNetwork> scored,
            int targetSize,
            int generation,
            Random random
    ) {
        Objects.requireNonNull(scored, "scored");
        Objects.requireNonNull(random, "random");
        int size = normalizePopulationSize(targetSize);
        if (scored.isEmpty()) return randomPopulation(size, random);

        List<ScoredNetwork> ranked = scored.stream()
                .filter(entry -> isValid(entry.network()))
                .sorted(Comparator.comparingDouble(ScoredNetwork::fitness).reversed())
                .toList();
        if (ranked.isEmpty()) return randomPopulation(size, random);

        List<MovementNetwork> next = new ArrayList<>(size);
        int elites = Math.min(Math.min(ELITE_COUNT, ranked.size()), size);
        for (int i = 0; i < elites; i++) {
            next.add(copy(ranked.get(i).network()));
        }

        double mutationRate = mutationRate(generation);
        double mutationStrength = mutationStrength(generation);
        while (next.size() < size) {
            MovementNetwork parentA = tournament(ranked, random).network();
            MovementNetwork parentB = tournament(ranked, random).network();
            MovementNetwork child = crossover(parentA, parentB, random);
            child = mutate(child, mutationRate, mutationStrength, random);
            if (isValid(child)) {
                next.add(child);
            } else {
                next.add(MovementNetwork.random(MovementNetworkShape.defaultLayers(), random));
            }
        }
        return next;
    }

    public static MovementNetwork copy(MovementNetwork network) {
        if (!isValid(network)) {
            return MovementNetwork.randomDefault();
        }
        return MovementNetwork.fromParameters(network.layerSizes(), network.weights(), network.biases());
    }

    public static boolean isValid(MovementNetwork network) {
        return network != null
                && network.validate(MovementNetworkShape.INPUT_COUNT, MovementNetworkShape.OUTPUT_COUNT).valid();
    }

    private static ScoredNetwork tournament(List<ScoredNetwork> ranked, Random random) {
        ScoredNetwork best = null;
        for (int i = 0; i < Math.min(TOURNAMENT_SIZE, ranked.size()); i++) {
            ScoredNetwork candidate = ranked.get(random.nextInt(ranked.size()));
            if (best == null || candidate.fitness() > best.fitness()) {
                best = candidate;
            }
        }
        return best == null ? ranked.get(0) : best;
    }

    private static MovementNetwork crossover(MovementNetwork a, MovementNetwork b, Random random) {
        MovementNetwork safeA = copy(a);
        MovementNetwork safeB = isCompatible(a, b) ? b : safeA;
        int[] shape = safeA.layerSizes();
        double[][][] weightsA = safeA.weights();
        double[][][] weightsB = safeB.weights();
        double[][] biasesA = safeA.biases();
        double[][] biasesB = safeB.biases();

        double[][][] childWeights = new double[weightsA.length][][];
        double[][] childBiases = new double[biasesA.length][];
        for (int layer = 0; layer < weightsA.length; layer++) {
            childWeights[layer] = new double[weightsA[layer].length][];
            childBiases[layer] = new double[biasesA[layer].length];
            for (int node = 0; node < weightsA[layer].length; node++) {
                childBiases[layer][node] = random.nextBoolean() ? biasesA[layer][node] : biasesB[layer][node];
                childWeights[layer][node] = new double[weightsA[layer][node].length];
                for (int input = 0; input < weightsA[layer][node].length; input++) {
                    childWeights[layer][node][input] = random.nextBoolean()
                            ? weightsA[layer][node][input]
                            : weightsB[layer][node][input];
                }
            }
        }
        return MovementNetwork.fromParameters(shape, childWeights, childBiases);
    }

    private static MovementNetwork mutate(MovementNetwork network, double rate, double strength, Random random) {
        int[] shape = network.layerSizes();
        double[][][] weights = network.weights();
        double[][] biases = network.biases();
        for (int layer = 0; layer < weights.length; layer++) {
            for (int node = 0; node < weights[layer].length; node++) {
                if (random.nextDouble() < rate) {
                    biases[layer][node] = mutateValue(biases[layer][node], strength, random);
                }
                for (int input = 0; input < weights[layer][node].length; input++) {
                    if (random.nextDouble() < rate) {
                        weights[layer][node][input] = mutateValue(weights[layer][node][input], strength, random);
                    }
                }
            }
        }
        return MovementNetwork.fromParameters(shape, weights, biases);
    }

    private static double mutateValue(double value, double strength, Random random) {
        if (!Double.isFinite(value)) value = 0.0;
        double mutated = value + random.nextGaussian() * strength;
        if (!Double.isFinite(mutated)) return 0.0;
        return Math.max(-PARAMETER_LIMIT, Math.min(PARAMETER_LIMIT, mutated));
    }

    private static double mutationRate(int generation) {
        double cooling = Math.max(0.55, 1.0 - Math.min(0.45, generation * 0.01));
        return BASE_MUTATION_RATE * cooling;
    }

    private static double mutationStrength(int generation) {
        double cooling = Math.max(0.45, 1.0 - Math.min(0.55, generation * 0.012));
        return BASE_MUTATION_STRENGTH * cooling;
    }

    private static boolean isCompatible(MovementNetwork a, MovementNetwork b) {
        return isValid(a) && isValid(b) && java.util.Arrays.equals(a.layerSizes(), b.layerSizes());
    }

    public record ScoredNetwork(MovementNetwork network, double fitness) {}
}
