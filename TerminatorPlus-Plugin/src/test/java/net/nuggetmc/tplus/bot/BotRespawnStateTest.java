package net.nuggetmc.tplus.bot;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BotRespawnStateTest {

    @Test
    void preservesInventoryShapeAndEmptySlots() {
        ItemStack[] copied = BotRespawnState.copy(new ItemStack[]{null, null});

        assertEquals(2, copied.length);
        assertNull(copied[0]);
        assertNull(copied[1]);
    }
}
