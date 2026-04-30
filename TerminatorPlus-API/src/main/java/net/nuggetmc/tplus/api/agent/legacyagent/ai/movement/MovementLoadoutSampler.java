package net.nuggetmc.tplus.api.agent.legacyagent.ai.movement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class MovementLoadoutSampler {
    private final String mixName;
    private final List<Entry> entries;
    private final int totalWeight;
    private final Random random;

    private MovementLoadoutSampler(String mixName, Map<String, Integer> weights, Random random) {
        this.mixName = mixName == null || mixName.isBlank() ? "movement_balanced" : mixName;
        this.random = random == null ? new Random() : random;
        this.entries = new ArrayList<>();
        int total = 0;
        if (weights != null) {
            for (Map.Entry<String, Integer> entry : weights.entrySet()) {
                if (entry.getKey() == null || entry.getKey().isBlank()
                        || entry.getValue() == null || entry.getValue() <= 0) {
                    continue;
                }
                total += entry.getValue();
                entries.add(new Entry(entry.getKey(), entry.getValue(), total));
            }
        }
        if (entries.isEmpty()) {
            total = 1;
            entries.add(new Entry("sword", 1, 1));
        }
        this.totalWeight = total;
    }

    public static MovementLoadoutSampler fromConfig(MovementTrainingConfig config, Random random) {
        MovementTrainingConfig safe = config == null
                ? new MovementTrainingConfig(true, "movement_controller", MovementNetworkShape.DEFAULT_LAYERS,
                false, true, "ai/brain.json", "ai/movement/manifest.json",
                "ai/movement/brains", MovementBrainBank.FALLBACK_BRAIN_NAME, true,
                "import-compatible-or-reset", false, "movement_balanced",
                MovementBrainBank.FALLBACK_BRAIN_NAME, MovementTrainingConfig.defaultLoadoutMixes())
                : config;
        return new MovementLoadoutSampler(safe.effectiveLoadoutMix(), safe.selectedLoadoutMix(), random);
    }

    public LoadoutSelection sample() {
        int pick = random.nextInt(totalWeight) + 1;
        for (Entry entry : entries) {
            if (pick <= entry.cumulativeWeight()) {
                return new LoadoutSelection(entry.name(), CombatTrainingSnapshot.familyForLoadout(entry.name()));
            }
        }
        Entry fallback = entries.get(entries.size() - 1);
        return new LoadoutSelection(fallback.name(), CombatTrainingSnapshot.familyForLoadout(fallback.name()));
    }

    public String mixName() {
        return mixName;
    }

    public Map<String, Integer> weights() {
        Map<String, Integer> weights = new LinkedHashMap<>();
        for (Entry entry : entries) {
            weights.put(entry.name(), entry.weight());
        }
        return Map.copyOf(weights);
    }

    public String describeWeights() {
        StringBuilder out = new StringBuilder();
        boolean first = true;
        for (Entry entry : entries) {
            if (!first) out.append(", ");
            first = false;
            out.append(entry.name()).append('=').append(entry.weight());
        }
        return out.toString();
    }

    public static String describeCounts(Map<String, Integer> counts) {
        if (counts == null || counts.isEmpty()) return "none";
        StringBuilder out = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (!first) out.append(", ");
            first = false;
            out.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return out.toString();
    }

    public record LoadoutSelection(String name, String family) {
    }

    private record Entry(String name, int weight, int cumulativeWeight) {
    }
}
