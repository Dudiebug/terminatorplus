package net.nuggetmc.tplus.bot;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BotRespawnStateTest {

    @Test
    void preservesInventoryShapeAndEmptySlots() {
        ItemStack[] copied = BotRespawnState.copy(new ItemStack[]{null, null});

        assertEquals(2, copied.length);
        assertNull(copied[0]);
        assertNull(copied[1]);
    }

    @Test
    void ignoresAirborneAndUnsafeCandidates() {
        LocationCandidate candidate = new LocationCandidate(4, 64, 9);

        assertNull(RespawnSafety.captureAnchor(null, candidate.location(), false, ignored -> {
            throw new AssertionError("airborne candidates must not be checked");
        }));
        assertNull(RespawnSafety.captureAnchor(null, candidate.location(), true, ignored -> false));
    }

    @Test
    void retainsTheFirstSafeGroundedLocation() {
        LocationCandidate first = new LocationCandidate(4, 64, 9);
        LocationCandidate later = new LocationCandidate(20, 70, 30);
        org.bukkit.Location firstLocation = first.location();

        org.bukkit.Location anchor = RespawnSafety.captureAnchor(
                null, firstLocation, true, ignored -> true);
        org.bukkit.Location retained = RespawnSafety.captureAnchor(
                anchor, later.location(), true, ignored -> true);

        assertNotSame(firstLocation, anchor);
        assertSame(anchor, retained);
        firstLocation.setX(99);
        assertEquals(4, anchor.getBlockX());
        assertEquals(64, anchor.getBlockY());
        assertEquals(9, anchor.getBlockZ());
    }

    @Test
    void choosesNearestUnblockedFallbackInStableOrder() {
        List<RespawnSafety.Offset> offsets = RespawnSafety.nearestOffsets(1);

        assertEquals(List.of(
                new RespawnSafety.Offset(0, 0),
                new RespawnSafety.Offset(1, 0),
                new RespawnSafety.Offset(-1, 0),
                new RespawnSafety.Offset(0, 1),
                new RespawnSafety.Offset(0, -1),
                new RespawnSafety.Offset(1, 1),
                new RespawnSafety.Offset(1, -1),
                new RespawnSafety.Offset(-1, 1),
                new RespawnSafety.Offset(-1, -1)
        ), offsets);

        Set<RespawnSafety.Offset> blocked = Set.of(
                new RespawnSafety.Offset(0, 0),
                new RespawnSafety.Offset(1, 0));
        RespawnSafety.Offset selected = RespawnSafety.firstSafe(offsets, offset -> !blocked.contains(offset));

        assertEquals(new RespawnSafety.Offset(-1, 0), selected);
        assertNull(RespawnSafety.firstSafe(offsets, ignored -> false));
    }

    @Test
    void prefersPinnedSpawnThenAnchorThenOrigin() {
        org.bukkit.Location pinned = new LocationCandidate(1, 100, 1).location();
        org.bukkit.Location anchor = new LocationCandidate(2, 64, 2).location();
        org.bukkit.Location origin = new LocationCandidate(3, 70, 3).location();

        assertEquals(pinned, BotRespawnState.chooseSpawn(pinned, anchor, origin));
        assertEquals(anchor, BotRespawnState.chooseSpawn(null, anchor, origin));
        assertEquals(origin, BotRespawnState.chooseSpawn(null, null, origin));
        assertNull(BotRespawnState.chooseSpawn(null, null, null));
        assertNotSame(pinned, BotRespawnState.chooseSpawn(pinned, anchor, origin));
    }

    @Test
    void searchesFloorsNearestToTheAnchorPreferringBelow() {
        // Standing at y=64 means the floor block is y=63.
        assertEquals(List.of(63, 62, 64, 61, 65, 60, 59),
                RespawnSafety.verticalCandidates(64, 2, 4, -64, 320));
    }

    @Test
    void clampsFloorSearchToTheWorldHeightRange() {
        assertEquals(List.of(-63, -64),
                RespawnSafety.verticalCandidates(-62, 0, 16, -64, 320));
        // Floor 318 would put the bot's head above the build limit.
        assertEquals(List.of(316, 315, 317),
                RespawnSafety.verticalCandidates(317, 2, 1, -64, 320));
    }

    @Test
    void retriesBeforeFallingBackToWorldSpawn() {
        assertEquals(20L, BotManagerImpl.respawnDelayTicks(1));
        assertEquals(40L, BotManagerImpl.respawnDelayTicks(2));
        for (int attempt = 1; attempt < BotManagerImpl.MAX_RESPAWN_ATTEMPTS; attempt++) {
            assertFalse(BotManagerImpl.isFinalRespawnAttempt(attempt));
        }
        assertTrue(BotManagerImpl.isFinalRespawnAttempt(BotManagerImpl.MAX_RESPAWN_ATTEMPTS));
    }

    @Test
    void emitsPoofThroughTheSmallTestableSeam() {
        org.bukkit.Location location = new LocationCandidate(4, 64, 9).location();
        List<org.bukkit.Location> emitted = new ArrayList<>();

        RespawnSafety.emitPoof(location, emitted::add);

        assertEquals(1, emitted.size());
        assertNotSame(location, emitted.get(0));
        assertEquals(location, emitted.get(0));
    }

    private record LocationCandidate(int x, int y, int z) {
        org.bukkit.Location location() {
            return new org.bukkit.Location(null, x, y, z);
        }
    }
}
