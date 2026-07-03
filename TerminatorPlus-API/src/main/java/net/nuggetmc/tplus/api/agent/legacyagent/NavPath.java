package net.nuggetmc.tplus.api.agent.legacyagent;

import java.util.List;

final class NavPath {
    private final TerrainPathfinder.SearchMode mode;
    private final List<NavStep> steps;
    private final int nodesConsidered;
    private final String reason;

    NavPath(TerrainPathfinder.SearchMode mode, List<NavStep> steps, int nodesConsidered, String reason) {
        this.mode = mode;
        this.steps = steps == null ? List.of() : List.copyOf(steps);
        this.nodesConsidered = nodesConsidered;
        this.reason = reason == null ? "" : reason;
    }

    TerrainPathfinder.SearchMode mode() {
        return mode;
    }

    List<NavStep> steps() {
        return steps;
    }

    int nodesConsidered() {
        return nodesConsidered;
    }

    String reason() {
        return reason;
    }

    boolean breakEnabled() {
        return mode == TerrainPathfinder.SearchMode.ALLOW_BREAK;
    }

    boolean empty() {
        return steps.isEmpty();
    }
}
