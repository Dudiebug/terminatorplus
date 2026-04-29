package net.nuggetmc.tplus.api.agent.legacyagent.ai.movement;

import java.util.Arrays;

/**
 * Stable movement-network dimensions. Input/output order is owned by the
 * movement input/output records in the plugin module.
 */
public final class MovementNetworkShape {

    public static final int INPUT_COUNT = 30;
    public static final int OUTPUT_COUNT = 8;
    public static final int[] DEFAULT_LAYERS = {INPUT_COUNT, 32, 24, OUTPUT_COUNT};

    private MovementNetworkShape() {}

    public static int[] defaultLayers() {
        return Arrays.copyOf(DEFAULT_LAYERS, DEFAULT_LAYERS.length);
    }
}
