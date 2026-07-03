package net.nuggetmc.tplus.api.agent.legacyagent;

import java.util.List;

record NavStep(NavNode node, List<NavAction> actions, String moveType, double cost) {
    NavStep {
        actions = actions == null ? List.of() : List.copyOf(actions);
        moveType = moveType == null ? "walk" : moveType;
    }
}
