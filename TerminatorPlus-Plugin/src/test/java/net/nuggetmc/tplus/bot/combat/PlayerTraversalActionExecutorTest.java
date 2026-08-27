package net.nuggetmc.tplus.bot.combat;

import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerTraversalActionExecutorTest {

    @Test
    void interactionPointTargetsThinCollisionShapeInsteadOfBlockCenter() {
        Vector eye = new Vector(11.2, 101.62, 1.4);
        BoundingBox closedTrapdoor = new BoundingBox(12.0, 101.0, 0.0, 13.0, 101.1875, 1.0);

        Vector point = PlayerTraversalActionExecutor.interactionPoint(
                eye, closedTrapdoor, new Vector(12.5, 101.5, 0.5));

        assertEquals(12.0001, point.getX(), 1.0e-9);
        assertEquals(101.1874, point.getY(), 1.0e-9);
        assertEquals(0.9999, point.getZ(), 1.0e-9);
    }
}
