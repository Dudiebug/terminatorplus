package net.nuggetmc.tplus.api.agent.legacyagent;

import net.nuggetmc.tplus.api.Terminator;
import net.nuggetmc.tplus.api.agent.BotRuntimeSnapshot;
import org.bukkit.entity.LivingEntity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BotRuntimeOrchestratorTest {

    @Test
    void ownsOneRuntimePerBotAndRemovesItCleanly() {
        UUID id = UUID.randomUUID();
        Terminator bot = bot(id, "runtime-test");
        AtomicInteger handledTicks = new AtomicInteger();
        BotRuntimeOrchestrator orchestrator = new BotRuntimeOrchestrator(runtime -> handledTicks.incrementAndGet());

        orchestrator.add(bot);
        orchestrator.add(bot);
        orchestrator.tick(bot);
        orchestrator.tick(bot);

        BotRuntimeSnapshot snapshot = orchestrator.snapshot(id).orElseThrow();
        assertEquals("runtime-test", snapshot.botName());
        assertEquals(2, snapshot.tickCount());
        assertEquals(2, handledTicks.get());

        orchestrator.remove(bot);
        assertFalse(orchestrator.snapshot(id).isPresent());
    }

    @Test
    void clearDropsEveryRuntime() {
        BotRuntimeOrchestrator orchestrator = new BotRuntimeOrchestrator(runtime -> { });
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        orchestrator.add(bot(first, "first"));
        orchestrator.add(bot(second, "second"));

        orchestrator.clear();

        assertTrue(orchestrator.snapshot(first).isEmpty());
        assertTrue(orchestrator.snapshot(second).isEmpty());
    }

    private static Terminator bot(UUID id, String name) {
        LivingEntity entity = (LivingEntity) Proxy.newProxyInstance(
                LivingEntity.class.getClassLoader(),
                new Class<?>[]{LivingEntity.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> id;
                    case "getName" -> name;
                    default -> defaultValue(method.getReturnType());
                });

        return (Terminator) Proxy.newProxyInstance(
                Terminator.class.getClassLoader(),
                new Class<?>[]{Terminator.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getBukkitEntity" -> entity;
                    case "getBotName" -> name;
                    case "isBotAlive" -> true;
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        if (type == char.class) return '\0';
        throw new IllegalArgumentException("Unsupported primitive " + type);
    }
}
