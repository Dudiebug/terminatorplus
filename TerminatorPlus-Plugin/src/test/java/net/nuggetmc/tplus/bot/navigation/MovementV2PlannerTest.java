package net.nuggetmc.tplus.bot.navigation;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovementV2PlannerTest {

    private static final MovementV2Planner.Pos START = pos(0, 1, 0);
    private final MovementV2Planner planner = new MovementV2Planner();

    @Test
    void routesAroundAClosedWallWithoutBreaking() {
        GridWorld world = GridWorld.withFloor(-5, 5, -5, 5);
        world.solid(pos(1, 1, 0)).solid(pos(1, 2, 0));

        MovementV2Planner.Result result = planner.plan(world, START, pos(3, 1, 0), 0.1,
                MovementV2Planner.Capabilities.NONE, policy(false, false, false, false, false));

        assertTrue(result.complete());
        assertEquals(MovementV2Planner.Phase.NO_BREAK, result.phase());
        assertTrue(result.steps().stream().noneMatch(step -> step.kind() == MovementV2Planner.Kind.BREAK));
        assertTrue(result.steps().stream().anyMatch(step -> step.to().z() != 0));
    }

    @Test
    void diagonalMovementCannotCutThroughAClosedCorner() {
        GridWorld world = GridWorld.withFloor(-3, 3, -3, 3);
        world.solid(pos(1, 1, 0)).solid(pos(1, 2, 0));
        world.solid(pos(0, 1, 1)).solid(pos(0, 2, 1));

        MovementV2Planner.Result result = planner.plan(world, START, pos(1, 1, 1), 0.1,
                MovementV2Planner.Capabilities.NONE, policy(false, false, false, false, false));

        assertTrue(result.steps().stream().noneMatch(step -> step.from().equals(START)
                && step.to().equals(pos(1, 1, 1))));
    }

    @Test
    void ordinaryDropsStopAtThreeBlocks() {
        GridWorld world = GridWorld.empty(-2, 3, -2, 2);
        world.solid(pos(0, 5, 0)).solid(pos(1, 0, 0));
        MovementV2Planner.Pos high = pos(0, 6, 0);
        MovementV2Planner.Pos low = pos(1, 1, 0);

        MovementV2Planner.Result result = planner.plan(world, high, low, 0.1,
                MovementV2Planner.Capabilities.NONE, policy(false, false, false, false, false));

        assertFalse(result.complete());
        assertTrue(result.steps().stream().noneMatch(step -> step.kind() == MovementV2Planner.Kind.DROP));
    }

    @Test
    void clutchDropsMayGoDeeperThanThreeBlocks() {
        GridWorld world = GridWorld.empty(-2, 3, -2, 2);
        world.solid(pos(0, 5, 0)).solid(pos(1, 0, 0));
        MovementV2Planner.Capabilities clutch = new MovementV2Planner.Capabilities(
                false, false, false, false, true, false, 0);

        MovementV2Planner.Result result = planner.plan(world, pos(0, 6, 0), pos(1, 1, 0), 0.1,
                clutch, policy(false, true, false, false, false));

        assertTrue(result.complete());
        MovementV2Planner.Step step = result.steps().getFirst();
        assertEquals(MovementV2Planner.Kind.CLUTCH_DROP, step.kind());
        assertEquals(5, step.from().y() - step.to().y());
    }

    @Test
    void parkourCrossesAThreeBlockGap() {
        GridWorld world = GridWorld.empty(-2, 6, -2, 2);
        world.solid(pos(0, 0, 0)).solid(pos(4, 0, 0));
        MovementV2Planner.Capabilities parkour = new MovementV2Planner.Capabilities(
                false, false, false, true, false, false, 0);

        MovementV2Planner.Result result = planner.plan(world, START, pos(4, 1, 0), 0.1,
                parkour, policy(true, false, false, false, false));

        assertTrue(result.complete());
        assertEquals(MovementV2Planner.Kind.PARKOUR, result.steps().getFirst().kind());
    }

    @Test
    void parkourRejectsASuperhumanDiagonalLaunch() {
        GridWorld world = GridWorld.empty(-2, 6, -2, 6);
        world.solid(pos(0, 0, 0)).solid(pos(4, 0, 4));
        MovementV2Planner.Capabilities parkour = new MovementV2Planner.Capabilities(
                false, false, false, true, false, false, 0);

        MovementV2Planner.Result result = planner.plan(world, START, pos(4, 1, 4), 0.1,
                parkour, policy(true, false, false, false, false));

        assertFalse(result.complete());
        assertTrue(result.steps().stream().noneMatch(step -> step.kind() == MovementV2Planner.Kind.PARKOUR));
    }

    @Test
    void bridgePlacementIsADeclaredAction() {
        GridWorld world = GridWorld.empty(-2, 3, -2, 2);
        world.solid(pos(0, 0, 0));
        MovementV2Planner.Capabilities blocks = new MovementV2Planner.Capabilities(
                false, true, false, false, false, false, 8);

        MovementV2Planner.Result result = planner.plan(world, START, pos(1, 1, 0), 0.1,
                blocks, policy(false, false, true, false, false));

        assertTrue(result.complete());
        MovementV2Planner.Step step = result.steps().getFirst();
        assertEquals(MovementV2Planner.Kind.PLACE_BRIDGE, step.kind());
        assertEquals(pos(1, 0, 0), step.actionPos());
    }

    @Test
    void pillarPlacementIsADeclaredAction() {
        GridWorld world = GridWorld.empty(-2, 2, -2, 2);
        world.solid(pos(0, 0, 0));
        MovementV2Planner.Capabilities blocks = new MovementV2Planner.Capabilities(
                false, false, true, false, false, false, 8);

        MovementV2Planner.Result result = planner.plan(world, START, pos(0, 2, 0), 0.1,
                blocks, policy(false, false, false, true, false));

        assertTrue(result.complete());
        MovementV2Planner.Step step = result.steps().getFirst();
        assertEquals(MovementV2Planner.Kind.PILLAR, step.kind());
        assertEquals(START, step.actionPos());
    }

    @Test
    void aTallPillarStartsWithOnePlacementAndAReplan() {
        GridWorld world = GridWorld.empty(-2, 2, -2, 2);
        world.solid(pos(0, 0, 0));
        MovementV2Planner.Capabilities blocks = new MovementV2Planner.Capabilities(
                false, false, true, false, false, false, 8);

        MovementV2Planner.Result result = planner.plan(world, START, pos(0, 4, 0), 0.1,
                blocks, policy(false, false, false, true, false));

        assertFalse(result.complete());
        assertEquals(MovementV2Planner.Kind.PILLAR, result.steps().getLast().kind());
        assertEquals(1, result.steps().stream().filter(step -> step.kind().changesWorld()).count());
    }

    @Test
    void handOpenedDoorIsADeclaredAction() {
        GridWorld world = GridWorld.corridor(0, 2);
        MovementV2Planner.Cell door = new MovementV2Planner.Cell(
                true, false, false, false, true, true, false);
        world.set(pos(1, 1, 0), door);
        world.set(pos(1, 2, 0), door);

        MovementV2Planner.Capabilities open = new MovementV2Planner.Capabilities(
                true, false, false, false, false, false, 0);
        MovementV2Planner.Result result = planner.plan(world, START, pos(1, 1, 0), 0.1,
                open, policy(false, false, false, false, false));

        assertTrue(result.complete());
        assertEquals(MovementV2Planner.Kind.OPEN, result.steps().getFirst().kind());
    }

    @Test
    void breakingRunsOnlyAfterNoBreakSearchProvesNoRoute() {
        GridWorld world = GridWorld.corridor(0, 2);
        world.solid(pos(1, 1, 0)).solid(pos(1, 2, 0));
        MovementV2Planner.Capabilities breakable = new MovementV2Planner.Capabilities(
                false, false, false, false, false, true, 0);

        MovementV2Planner.Result result = planner.plan(world, START, pos(2, 1, 0), 0.1,
                breakable, policy(false, false, false, false, true));

        assertTrue(result.complete());
        assertEquals(MovementV2Planner.Phase.BREACH, result.phase());
        assertEquals(MovementV2Planner.Kind.BREAK, result.steps().getFirst().kind());
    }

    @Test
    void aSearchLimitNeverEscalatesIntoBreaking() {
        GridWorld world = GridWorld.corridor(0, 3);
        world.solid(pos(1, 1, 0));
        MovementV2Planner.Capabilities breakable = new MovementV2Planner.Capabilities(
                false, false, false, false, false, true, 0);
        MovementV2Planner.Policy oneNode = new MovementV2Planner.Policy(
                1, 1_000_000_000L, 1, 3, 48, 4,
                false, false, false, false, true);

        MovementV2Planner.Result result = planner.plan(world, START, pos(3, 1, 0), 0.1,
                breakable, oneNode);

        assertEquals(MovementV2Planner.Status.NODE_BUDGET, result.status());
        assertEquals(MovementV2Planner.Phase.NO_BREAK, result.phase());
        assertTrue(result.steps().stream().noneMatch(step -> step.kind() == MovementV2Planner.Kind.BREAK));
    }

    @Test
    void partialRouteMayEndWithOneCheckedNonBreakingAction() {
        GridWorld world = GridWorld.empty(0, 4, 0, 0);
        world.solid(pos(0, 0, 0)).solid(pos(1, 0, 0));
        MovementV2Planner.Capabilities blocks = new MovementV2Planner.Capabilities(
                false, true, false, false, false, false, 8);

        MovementV2Planner.Result result = planner.plan(world, START, pos(4, 1, 0), 0.1,
                blocks, policy(false, false, true, false, false));

        assertFalse(result.complete());
        assertFalse(result.steps().isEmpty());
        assertEquals(MovementV2Planner.Kind.PLACE_BRIDGE, result.steps().getLast().kind());
        assertEquals(1, result.steps().stream().filter(step -> step.kind().changesWorld()).count());
    }

    @Test
    void aSearchLimitNeverSpendsABuildingBlock() {
        GridWorld world = GridWorld.empty(0, 4, 0, 0);
        world.solid(pos(0, 0, 0)).solid(pos(1, 0, 0));
        MovementV2Planner.Capabilities blocks = new MovementV2Planner.Capabilities(
                false, true, false, false, false, false, 8);
        MovementV2Planner.Policy oneNode = new MovementV2Planner.Policy(
                1, 1_000_000_000L, 1, 3, 48, 4,
                false, false, true, false, false);

        MovementV2Planner.Result result = planner.plan(world, START, pos(4, 1, 0), 0.1,
                blocks, oneNode);

        assertEquals(MovementV2Planner.Status.NODE_BUDGET, result.status());
        assertTrue(result.steps().stream().noneMatch(step -> step.kind().changesWorld()));
    }

    @Test
    void contextContractIsExactlyThreeByThreeChunks() {
        assertEquals(1, MovementV2Planner.CONTEXT_CHUNK_RADIUS);
        assertEquals(9, MovementV2Planner.CONTEXT_CHUNK_COUNT);
    }

    @Test
    void policyCannotTurnAnOrdinaryDropIntoAnUnboundedClutchShortcut() {
        MovementV2Planner.Policy policy = new MovementV2Planner.Policy(
                Integer.MAX_VALUE, Long.MAX_VALUE, 9, 99, 99_999, 99,
                true, true, true, true, true);

        assertEquals(3, policy.maxNormalDrop());
        assertEquals(512, policy.maxClutchDrop());
        assertEquals(6, policy.maxParkourDistance());
        assertEquals(65_536, policy.maxNodes());
        assertEquals(50_000_000L, policy.maxNanosPerPhase());
    }

    private static MovementV2Planner.Policy policy(
            boolean parkour,
            boolean clutch,
            boolean place,
            boolean pillar,
            boolean breaking
    ) {
        return new MovementV2Planner.Policy(
                4096, 1_000_000_000L, 1, 3, 48, 4,
                parkour, clutch, place, pillar, breaking);
    }

    private static MovementV2Planner.Pos pos(int x, int y, int z) {
        return new MovementV2Planner.Pos(x, y, z);
    }

    private static final class GridWorld implements MovementV2Planner.WorldView {
        private final int minX;
        private final int maxX;
        private final int minZ;
        private final int maxZ;
        private final Map<MovementV2Planner.Pos, MovementV2Planner.Cell> cells = new HashMap<>();
        private final Set<MovementV2Planner.Pos> floor = new HashSet<>();

        private GridWorld(int minX, int maxX, int minZ, int maxZ) {
            this.minX = minX;
            this.maxX = maxX;
            this.minZ = minZ;
            this.maxZ = maxZ;
        }

        static GridWorld empty(int minX, int maxX, int minZ, int maxZ) {
            return new GridWorld(minX, maxX, minZ, maxZ);
        }

        static GridWorld withFloor(int minX, int maxX, int minZ, int maxZ) {
            GridWorld world = empty(minX, maxX, minZ, maxZ);
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) world.floor.add(pos(x, 0, z));
            }
            return world;
        }

        static GridWorld corridor(int minX, int maxX) {
            GridWorld world = empty(minX, maxX, 0, 0);
            for (int x = minX; x <= maxX; x++) world.floor.add(pos(x, 0, 0));
            return world;
        }

        GridWorld solid(MovementV2Planner.Pos at) {
            cells.put(at, MovementV2Planner.Cell.SOLID);
            return this;
        }

        void set(MovementV2Planner.Pos at, MovementV2Planner.Cell cell) {
            cells.put(at, cell);
        }

        @Override
        public boolean inBounds(MovementV2Planner.Pos at) {
            return at.x() >= minX && at.x() <= maxX
                    && at.z() >= minZ && at.z() <= maxZ
                    && at.y() >= 0 && at.y() <= 16;
        }

        @Override
        public MovementV2Planner.Cell cell(MovementV2Planner.Pos at) {
            if (!inBounds(at)) return MovementV2Planner.Cell.UNAVAILABLE;
            if (floor.contains(at)) return MovementV2Planner.Cell.SOLID;
            return cells.getOrDefault(at, MovementV2Planner.Cell.AIR);
        }
    }
}
