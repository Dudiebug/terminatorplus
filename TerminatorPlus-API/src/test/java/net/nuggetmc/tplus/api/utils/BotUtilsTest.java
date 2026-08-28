package net.nuggetmc.tplus.api.utils;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BotUtilsTest {

    @Test
    void generatedBotIdsAreUniqueNpcIdsWithSteveParity() {
        Set<UUID> ids = new HashSet<>();

        for (int i = 0; i < 512; i++) {
            UUID id = BotUtils.randomSteveUUID();
            assertEquals(2, id.version());
            assertEquals(2, id.variant());
            assertEquals(0, id.hashCode() & 1);
            ids.add(id);
        }

        assertEquals(512, ids.size());
    }
}
