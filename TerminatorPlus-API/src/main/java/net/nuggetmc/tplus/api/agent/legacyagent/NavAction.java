package net.nuggetmc.tplus.api.agent.legacyagent;

import java.util.Objects;

record NavAction(Type type, BlockKey block, String reason) {
    NavAction {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(block, "block");
        reason = reason == null ? "" : reason;
    }

    enum Type {
        OPEN,
        BREAK
    }
}
