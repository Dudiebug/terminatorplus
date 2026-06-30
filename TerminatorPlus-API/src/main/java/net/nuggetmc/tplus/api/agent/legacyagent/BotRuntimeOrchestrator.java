package net.nuggetmc.tplus.api.agent.legacyagent;

import net.nuggetmc.tplus.api.Terminator;

/**
 * Per-bot runtime owner. This is the top-level tick handoff point for target
 * selection, survival checks, CombatDirector planning/execution, and movement.
 *
 * <p>The first implementation deliberately delegates to the legacy-compatible
 * tick body so behavior stays unchanged while later handoffs move individual
 * responsibilities behind the explicit controller interfaces.</p>
 */
final class BotRuntimeOrchestrator {

    private final LegacyAgent legacy;

    BotRuntimeOrchestrator(LegacyAgent legacy) {
        this.legacy = legacy;
    }

    void tick(Terminator bot) {
        legacy.tickBot(bot);
    }
}
