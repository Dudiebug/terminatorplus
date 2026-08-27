package net.nuggetmc.tplus.api.agent;

import java.util.UUID;

/**
 * Immutable, read-only view of one bot's live agent state.
 */
public record BotRuntimeSnapshot(
        UUID botId,
        String botName,
        boolean alive,
        UUID targetId,
        String targetName,
        double targetDistance,
        MovementMode movementMode,
        int stuckTicks,
        long tickCount,
        long targetChanges
) {

    public BotRuntimeSnapshot {
        if (botId == null) throw new IllegalArgumentException("botId cannot be null");
        botName = botName == null ? "" : botName;
        targetName = targetName == null ? "" : targetName;
        movementMode = movementMode == null ? MovementMode.LEGACY : movementMode;
        stuckTicks = Math.max(0, stuckTicks);
        tickCount = Math.max(0, tickCount);
        targetChanges = Math.max(0, targetChanges);
    }

    public enum MovementMode {
        LEGACY,
        MOVEMENT_CONTROLLER_NN,
        FULL_REPLACEMENT_NN
    }
}
