package net.nuggetmc.tplus.bot.navigation;

import org.bukkit.World;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovementV2PerformanceTest {

    @Test
    void planningIsEvenlyStaggeredAcrossBots() {
        int[] admissions = new int[100];
        for (int tick = 0; tick < 10; tick++) {
            int allowed = 0;
            for (int botId = 0; botId < 100; botId++) {
                if (MovementV2Controller.planningAllowed(tick, botId, 10)) {
                    allowed++;
                    admissions[botId]++;
                }
            }
            assertEquals(10, allowed);
        }
        for (int admissionCount : admissions) assertEquals(1, admissionCount);
        assertTrue(MovementV2Controller.planningAllowed(7, 42, 1));
    }

    @Test
    void chunkLoadedStateIsReadOncePerContextChunk() {
        AtomicInteger reads = new AtomicInteger();
        World world = (World) Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[]{World.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("isChunkLoaded") && method.getParameterCount() == 2) {
                        reads.incrementAndGet();
                        return (int) args[0] == 4 && (int) args[1] == -2;
                    }
                    throw new UnsupportedOperationException(method.toString());
                });
        BukkitNavigationContext.LoadedChunkCache cache =
                new BukkitNavigationContext.LoadedChunkCache(world, 4, -2);

        assertTrue(cache.isLoaded(4, -2));
        assertTrue(cache.isLoaded(4, -2));
        assertFalse(cache.isLoaded(5, -2));
        assertFalse(cache.isLoaded(5, -2));
        assertFalse(cache.isLoaded(6, -2));
        assertEquals(2, reads.get());
    }
}
