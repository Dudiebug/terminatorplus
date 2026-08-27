package net.nuggetmc.tplus.api.agent.legacyagent;

import net.nuggetmc.tplus.api.Terminator;
import net.nuggetmc.tplus.api.agent.BotRuntimeSnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Per-bot runtime owner. This is the top-level tick handoff point for target
 * selection, survival checks, CombatDirector planning/execution, and movement.
 *
 * <p>The first implementation deliberately delegates to the legacy-compatible
 * tick body so behavior stays unchanged while later handoffs move individual
 * responsibilities behind the explicit controller interfaces.</p>
 */
final class BotRuntimeOrchestrator {

    private final Consumer<BotRuntime> tickHandler;
    private final Map<UUID, BotRuntime> runtimes = new HashMap<>();

    BotRuntimeOrchestrator(LegacyAgent legacy) {
        this(legacy::tickBot);
    }

    BotRuntimeOrchestrator(Consumer<BotRuntime> tickHandler) {
        this.tickHandler = tickHandler;
    }

    void add(Terminator bot) {
        runtime(bot);
    }

    void tick(Terminator bot) {
        BotRuntime runtime = runtime(bot);
        runtime.tick();
        tickHandler.accept(runtime);
    }

    BotRuntime runtime(Terminator bot) {
        UUID id = bot.getBukkitEntity().getUniqueId();
        return runtimes.computeIfAbsent(id, ignored -> new BotRuntime(bot, id));
    }

    void remove(Terminator bot) {
        if (bot == null) return;
        runtimes.entrySet().removeIf(entry -> {
            if (entry.getValue().bot() != bot) return false;
            entry.getValue().clearTransient();
            return true;
        });
    }

    void clear() {
        runtimes.values().forEach(BotRuntime::clearTransient);
        runtimes.clear();
    }

    Optional<BotRuntimeSnapshot> snapshot(UUID botId) {
        BotRuntime runtime = runtimes.get(botId);
        return runtime == null ? Optional.empty() : Optional.of(runtime.snapshot());
    }
}
