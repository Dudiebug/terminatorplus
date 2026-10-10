package net.nuggetmc.tplus.bot;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.Set;

final class RespawnSafety {

    static final int SEARCH_RADIUS = 4;
    static final int SCAN_UP = 2;
    static final int SCAN_DOWN = 16;
    private static final double HALF_WIDTH = 0.3;
    private static final double HEIGHT = 1.8;
    private static final double EPSILON = 1.0E-4;
    private static final Set<String> HAZARDS = Set.of(
            "CACTUS", "CAMPFIRE", "COBWEB", "END_PORTAL", "FIRE", "MAGMA_BLOCK", "NETHER_PORTAL",
            "POINTED_DRIPSTONE", "POWDER_SNOW", "SOUL_CAMPFIRE", "SOUL_FIRE",
            "SWEET_BERRY_BUSH", "VINE", "WITHER_ROSE"
    );

    private RespawnSafety() {
    }

    static Location captureAnchor(Location existing, Location candidate, boolean grounded,
                                  Predicate<Location> safe) {
        if (existing != null || !grounded || candidate == null || safe == null || !safe.test(candidate)) {
            return existing;
        }
        return candidate.clone();
    }

    static List<Offset> nearestOffsets(int radius) {
        if (radius < 0) return List.of();

        List<Offset> offsets = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                offsets.add(new Offset(x, z));
            }
        }
        offsets.sort(Comparator
                .comparingInt(Offset::distanceSquared)
                .thenComparingInt(RespawnSafety::directionRank)
                .thenComparingInt(Offset::x)
                .thenComparingInt(Offset::z));
        return List.copyOf(offsets);
    }

    static <T> T firstSafe(Iterable<T> candidates, Predicate<T> safe) {
        if (candidates == null || safe == null) return null;
        for (T candidate : candidates) {
            if (safe.test(candidate)) return candidate;
        }
        return null;
    }

    /**
     * Floor-block Y levels to try around {@code anchorY}, nearest first and
     * preferring lower floors on ties, clamped to the world's height range.
     * The floor block for standing at {@code anchorY} is {@code anchorY - 1}.
     */
    static List<Integer> verticalCandidates(int anchorY, int up, int down, int minY, int maxY) {
        int floorY = anchorY - 1;
        List<Integer> candidates = new ArrayList<>();
        for (int distance = 0; distance <= Math.max(up, down); distance++) {
            if (distance <= down) addFloor(candidates, floorY - distance, minY, maxY);
            if (distance > 0 && distance <= up) addFloor(candidates, floorY + distance, minY, maxY);
        }
        return List.copyOf(candidates);
    }

    private static void addFloor(List<Integer> candidates, int y, int minY, int maxY) {
        // Leave room above the floor for the bot's body.
        if (y >= minY && y <= maxY - 3) candidates.add(y);
    }

    static Location findNearestSafe(Location anchor) {
        if (anchor == null || !anchor.isWorldLoaded()) return null;
        requirePrimaryThread();

        if (isSafeGrounded(anchor)) return anchor.clone();

        World world = anchor.getWorld();
        for (Offset offset : nearestOffsets(SEARCH_RADIUS)) {
            Location candidate = floorNear(world, anchor, offset);
            if (candidate != null) return candidate;
        }
        return null;
    }

    /**
     * Respawn location for a {@code /bot setspawn} point. Mid-air points are
     * kept exactly as set while they are still clear with ground below.
     */
    static Location resolvePinned(Location pinned) {
        if (pinned == null || !pinned.isWorldLoaded()) return null;
        requirePrimaryThread();

        if (checkPinnedSpawn(pinned).accepted()) return pinned.clone();
        return findNearestSafe(pinned);
    }

    static SpawnCheck checkPinnedSpawn(Location location) {
        if (location == null || !location.isWorldLoaded()
                || !isFinite(location.getX()) || !isFinite(location.getY()) || !isFinite(location.getZ())) {
            return SpawnCheck.refused(SpawnCheck.Problem.OUTSIDE_WORLD);
        }
        requirePrimaryThread();

        World world = location.getWorld();
        if (location.getY() <= world.getMinHeight() || location.getY() >= world.getMaxHeight()) {
            return SpawnCheck.refused(SpawnCheck.Problem.OUTSIDE_WORLD);
        }
        for (double dx : new double[]{-HALF_WIDTH + EPSILON, HALF_WIDTH - EPSILON}) {
            for (double dz : new double[]{-HALF_WIDTH + EPSILON, HALF_WIDTH - EPSILON}) {
                if (!world.getWorldBorder().isInside(location.clone().add(dx, 0, dz))) {
                    return SpawnCheck.refused(SpawnCheck.Problem.OUTSIDE_WORLD);
                }
            }
        }
        if (!isBodyClear(world, location)) return SpawnCheck.refused(SpawnCheck.Problem.OBSTRUCTED);

        for (int y = (int) Math.floor(location.getY() - EPSILON); y >= world.getMinHeight(); y--) {
            Block block = world.getBlockAt(location.getBlockX(), y, location.getBlockZ());
            if (block.isPassable() && !block.isLiquid()) continue;
            if (isHazard(block) && !isWater(block)) return SpawnCheck.refused(SpawnCheck.Problem.UNSAFE_LANDING);

            double top = block.isLiquid() ? y + 1.0 : block.getBoundingBox().getMaxY();
            return new SpawnCheck(SpawnCheck.Problem.NONE, Math.max(0.0, location.getY() - top));
        }
        return SpawnCheck.refused(SpawnCheck.Problem.NO_GROUND);
    }

    static boolean isSafeGrounded(Location location) {
        if (location == null || location.getWorld() == null
                || !isFinite(location.getX()) || !isFinite(location.getY()) || !isFinite(location.getZ())) {
            return false;
        }
        requirePrimaryThread();

        World world = location.getWorld();
        if (location.getY() <= world.getMinHeight() || location.getY() >= world.getMaxHeight()) return false;

        for (double dx : new double[]{-HALF_WIDTH + EPSILON, HALF_WIDTH - EPSILON}) {
            for (double dz : new double[]{-HALF_WIDTH + EPSILON, HALF_WIDTH - EPSILON}) {
                Location corner = location.clone().add(dx, 0, dz);
                if (!world.getWorldBorder().isInside(corner)) return false;
                Block floor = world.getBlockAt(
                        corner.getBlockX(),
                        (int) Math.floor(location.getY() - EPSILON),
                        corner.getBlockZ());
                BoundingBox floorBox = floor.getBoundingBox();
                if (isHazard(floor) || floor.isPassable() || floorBox.getVolume() <= EPSILON
                        || floorBox.getMaxY() <= floorBox.getMinY()
                        || Math.abs(floorBox.getMaxY() - location.getY()) > 0.03125) {
                    return false;
                }
            }
        }

        return isBodyClear(world, location);
    }

    private static boolean isBodyClear(World world, Location location) {
        BoundingBox body = new BoundingBox(
                location.getX() - HALF_WIDTH,
                location.getY(),
                location.getZ() - HALF_WIDTH,
                location.getX() + HALF_WIDTH,
                location.getY() + HEIGHT,
                location.getZ() + HALF_WIDTH
        );
        int minX = (int) Math.floor(body.getMinX() + EPSILON);
        int maxX = (int) Math.floor(body.getMaxX() - EPSILON);
        int minY = (int) Math.floor(body.getMinY() + EPSILON);
        int maxY = (int) Math.floor(body.getMaxY() - EPSILON);
        int minZ = (int) Math.floor(body.getMinZ() + EPSILON);
        int maxZ = (int) Math.floor(body.getMaxZ() - EPSILON);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (isHazard(block) || (!block.isPassable() && block.getBoundingBox().overlaps(body))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    static void emitPoof(Location location, Consumer<Location> emitter) {
        if (location != null && emitter != null) emitter.accept(location.clone());
    }

    /**
     * Safe standing spot in one column near the anchor's height. Searching
     * from the anchor instead of the world-top heightmap keeps bots off roofs
     * and the Nether ceiling, and finds islands over void.
     */
    private static Location floorNear(World world, Location anchor, Offset offset) {
        int x = anchor.getBlockX() + offset.x();
        int z = anchor.getBlockZ() + offset.z();
        for (int y : verticalCandidates(anchor.getBlockY(), SCAN_UP, SCAN_DOWN,
                world.getMinHeight(), world.getMaxHeight())) {
            Block floor = world.getBlockAt(x, y, z);
            if (floor.isPassable()) continue;
            BoundingBox floorBox = floor.getBoundingBox();
            if (floorBox.getVolume() <= EPSILON) continue;

            Location candidate = new Location(world, x + 0.5, floorBox.getMaxY(), z + 0.5,
                    anchor.getYaw(), anchor.getPitch());
            if (isSafeGrounded(candidate)) return candidate;
        }
        return null;
    }

    private static int directionRank(Offset offset) {
        if (offset.isCenter()) return 0;
        if (offset.z() == 0 && offset.x() > 0) return 1;
        if (offset.z() == 0) return 2;
        if (offset.x() == 0 && offset.z() > 0) return 3;
        if (offset.x() == 0) return 4;
        if (offset.x() > 0 && offset.z() > 0) return 5;
        if (offset.x() > 0) return 6;
        if (offset.z() > 0) return 7;
        return 8;
    }

    private static boolean isFinite(double value) {
        return Double.isFinite(value);
    }

    private static boolean isHazard(Block block) {
        return block.isLiquid()
                || HAZARDS.contains(block.getType().name())
                || (block.getBlockData() instanceof Waterlogged waterlogged && waterlogged.isWaterlogged());
    }

    private static boolean isWater(Block block) {
        return "WATER".equals(block.getType().name())
                || (block.getBlockData() instanceof Waterlogged waterlogged && waterlogged.isWaterlogged());
    }

    private static void requirePrimaryThread() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Respawn safety checks may only run on the server thread");
        }
    }

    record Offset(int x, int z) {
        int distanceSquared() {
            return x * x + z * z;
        }

        boolean isCenter() {
            return x == 0 && z == 0;
        }
    }
}
