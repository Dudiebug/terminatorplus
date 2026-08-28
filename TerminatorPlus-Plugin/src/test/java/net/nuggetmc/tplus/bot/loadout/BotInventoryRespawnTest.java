package net.nuggetmc.tplus.bot.loadout;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class BotInventoryRespawnTest {

    @Test
    void appliedLoadoutWinsOverInventoryAtDeath() {
        ItemStack[] original = new ItemStack[1];
        ItemStack[] atDeath = new ItemStack[2];

        ItemStack[] restored = BotInventory.respawnContents(true, original, atDeath);

        assertEquals(1, restored.length);
        assertNotSame(original, restored);
        assertEquals(2, BotInventory.respawnContents(false, original, atDeath).length);
    }
}
