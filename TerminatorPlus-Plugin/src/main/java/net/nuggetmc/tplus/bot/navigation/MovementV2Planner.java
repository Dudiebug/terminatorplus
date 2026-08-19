package net.nuggetmc.tplus.bot.navigation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Bounded, side-effect-free combat route planner.
 *
 * <p>The planner reads a {@link WorldView} and returns declarative steps. It
 * cannot open, place, break, equip, or use anything. Runtime code must validate
 * and execute action steps through the player-like action owner.</p>
 */
public final class MovementV2Planner {

    public static final int CONTEXT_CHUNK_RADIUS = 1;
    public static final int CONTEXT_CHUNK_COUNT = 9;

    private static final int[][] DIRECTIONS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    public Result plan(
            WorldView world,
            Pos start,
            Pos goal,
            double goalRadius,
            Capabilities capabilities,
            Policy policy
    ) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(goal, "goal");
        Capabilities caps = capabilities == null ? Capabilities.NONE : capabilities;
        Policy safePolicy = policy == null ? Policy.DEFAULT : policy;

        long started = System.nanoTime();
        Search natural = search(world, start, goal, Math.max(0.0, goalRadius), caps, safePolicy, false, started);
        if (natural.complete()) {
            return natural.toResult(Phase.NO_BREAK);
        }

        // A node/time limit means "we stopped looking", not "there is no route".
        // Never escalate that uncertainty into block breaking.
        if (natural.limitReached() || !safePolicy.allowBreak() || !caps.canBreak()) {
            return natural.toResult(Phase.NO_BREAK);
        }

        // A placement, pillar, door, or clutch can reveal the next local route
        // only after the live world changes. Execute one checked non-breaking
        // action and replan before considering a destructive breach.
        if (natural.endsWithAction()) {
            return natural.toResult(Phase.NO_BREAK);
        }

        Search breach = search(world, start, goal, Math.max(0.0, goalRadius), caps, safePolicy, true, started);
        if (breach.complete()) {
            return breach.toResult(Phase.BREACH);
        }

        // Partial breach routes are deliberately not executable. Return only
        // safe natural progress found by the first phase.
        return natural.toResult(Phase.NO_BREAK);
    }

    private Search search(
            WorldView world,
            Pos start,
            Pos goal,
            double goalRadius,
            Capabilities caps,
            Policy policy,
            boolean allowBreak,
            long overallStarted
    ) {
        long phaseStarted = System.nanoTime();
        PriorityQueue<Node> open = new PriorityQueue<>(Comparator
                .comparingDouble(Node::estimatedTotal)
                .thenComparingDouble(Node::heuristic)
                .thenComparingLong(Node::order));
        Map<Pos, Double> bestCost = new HashMap<>();
        Set<Pos> closed = new HashSet<>();
        long order = 0;

        Node first = new Node(start, null, null, 0.0, heuristic(start, goal, goalRadius), order++);
        open.add(first);
        bestCost.put(start, 0.0);
        Node bestPartial = first;
        int expanded = 0;

        while (!open.isEmpty()) {
            long elapsed = System.nanoTime() - phaseStarted;
            long totalElapsed = System.nanoTime() - overallStarted;
            if (expanded >= policy.maxNodes()) {
                return limited(bestPartial, expanded, totalElapsed, Status.NODE_BUDGET);
            }
            if (elapsed >= policy.maxNanosPerPhase()) {
                return limited(bestPartial, expanded, totalElapsed, Status.TIME_BUDGET);
            }

            Node current = open.poll();
            if (!closed.add(current.pos())) continue;
            expanded++;

            if (isGoal(world, current.pos(), goal, goalRadius)) {
                return new Search(Status.COMPLETE, reconstruct(current), expanded, totalElapsed, true);
            }

            if (betterPartial(current, bestPartial)) {
                bestPartial = current;
            }

            for (Step nextStep : neighbours(world, current.pos(), goal, caps, policy, allowBreak)) {
                Pos next = nextStep.to();
                if (closed.contains(next)) continue;
                double nextCost = current.cost() + nextStep.cost();
                if (nextCost >= bestCost.getOrDefault(next, Double.POSITIVE_INFINITY)) continue;
                bestCost.put(next, nextCost);
                open.add(new Node(next, current, nextStep, nextCost,
                        heuristic(next, goal, goalRadius), order++));
            }
        }

        List<Step> partial = allowBreak
                ? safePartial(bestPartial)
                : oneActionPartial(bestPartial);
        return new Search(Status.NO_ROUTE, partial, expanded,
                System.nanoTime() - overallStarted, false);
    }

    private static Search limited(Node bestPartial, int expanded, long elapsed, Status status) {
        return new Search(status, safePartial(bestPartial), expanded, elapsed, false);
    }

    private static boolean isGoal(WorldView world, Pos pos, Pos goal, double radius) {
        if (pos.distance(goal) > radius) return false;
        return world.clearLine(pos.above(), goal.above());
    }

    private static boolean betterPartial(Node candidate, Node previous) {
        if (candidate == null) return false;
        if (previous == null) return true;
        if (candidate.heuristic() + 1.0e-6 < previous.heuristic()) return true;
        return Math.abs(candidate.heuristic() - previous.heuristic()) < 1.0e-6
                && candidate.cost() < previous.cost();
    }

    private List<Step> neighbours(
            WorldView world,
            Pos from,
            Pos goal,
            Capabilities caps,
            Policy policy,
            boolean allowBreak
    ) {
        List<Step> result = new ArrayList<>(24);
        for (int[] direction : DIRECTIONS) {
            int dx = direction[0];
            int dz = direction[1];
            boolean diagonal = dx != 0 && dz != 0;
            Pos same = from.add(dx, 0, dz);
            if (!world.inBounds(same)) continue;

            if (world.bodyClear(same) && world.standable(same.below())
                    && (!diagonal || world.diagonalClear(from, same))) {
                result.add(new Step(from, same, diagonal ? Kind.DIAGONAL : Kind.WALK,
                        null, diagonal ? 1.414 : 1.0));
                continue;
            }

            Pos up = same.above();
            if (policy.maxStepUp() >= 1
                    && world.inBounds(up)
                    && world.bodyClear(up)
                    && world.standable(up.below())
                    && world.cell(from.above(2)).passable()
                    && (!diagonal || world.diagonalClear(from.above(), up))) {
                result.add(new Step(from, up, Kind.STEP_UP, null, diagonal ? 1.9 : 1.5));
                continue;
            }

            Step open = openStep(world, from, same, diagonal, caps);
            if (open != null) {
                result.add(open);
                continue;
            }

            Step drop = dropStep(world, from, same, diagonal, caps, policy);
            if (drop != null) {
                result.add(drop);
                continue;
            }

            Step bridge = bridgeStep(world, from, same, diagonal, caps, policy);
            if (bridge != null) {
                result.add(bridge);
                continue;
            }

            if (allowBreak) {
                Step breach = breakStep(world, from, same, diagonal, caps);
                if (breach != null) result.add(breach);
            }
        }

        if (policy.allowParkour() && caps.canParkour()) {
            addParkour(world, from, result, policy);
        }

        if (policy.allowPillar() && caps.canPillar() && caps.placeableBlocks() > 0
                && goal.y() > from.y()) {
            Pos destination = from.above();
            Pos placed = from;
            if (world.inBounds(destination)
                    && world.bodyClear(destination)
                    && world.cell(placed).replaceable()
                    && world.hasPlacementAnchor(placed)) {
                result.add(new Step(from, destination, Kind.PILLAR, placed, 7.0));
            }
        }
        return result;
    }

    private static Step openStep(WorldView world, Pos from, Pos to, boolean diagonal, Capabilities caps) {
        if (!caps.canOpen()) return null;
        Cell feet = world.cell(to);
        Cell head = world.cell(to.above());
        Pos action;
        if (feet.handOpenable() && (head.passable() || head.handOpenable())) {
            action = to;
        } else if (head.handOpenable() && (feet.passable() || feet.handOpenable())) {
            action = to.above();
        } else {
            return null;
        }
        if (!world.standable(to.below()) || (diagonal && !world.diagonalClear(from, to))) return null;
        return new Step(from, to, Kind.OPEN, action, diagonal ? 3.0 : 2.5);
    }

    private static Step dropStep(
            WorldView world,
            Pos from,
            Pos horizontal,
            boolean diagonal,
            Capabilities caps,
            Policy policy
    ) {
        if (!world.cell(horizontal).passable() || !world.cell(horizontal.above()).passable()) return null;
        boolean mayClutch = policy.allowClutchDrop() && caps.canClutch();
        int max = mayClutch ? policy.maxClutchDrop() : policy.maxNormalDrop();
        for (int depth = 1; depth <= max; depth++) {
            Pos landing = horizontal.add(0, -depth, 0);
            if (!world.inBounds(landing)) return null;
            if (!world.bodyClear(landing)) return null;
            if (!world.standable(landing.below())) continue;
            if (diagonal && !world.diagonalClear(from, horizontal)) return null;
            if (depth <= policy.maxNormalDrop()) {
                return new Step(from, landing, Kind.DROP, null, 1.0 + depth * 0.35);
            }
            if (mayClutch
                    && depth <= policy.maxClutchDrop()
                    && world.cell(landing).replaceable()
                    && world.hasPlacementAnchor(landing)) {
                return new Step(from, landing, Kind.CLUTCH_DROP, landing, 8.0 + depth * 0.25);
            }
            return null;
        }
        return null;
    }

    private static Step bridgeStep(
            WorldView world,
            Pos from,
            Pos to,
            boolean diagonal,
            Capabilities caps,
            Policy policy
    ) {
        if (!policy.allowPlace() || !caps.canPlace() || caps.placeableBlocks() <= 0) return null;
        if (!world.bodyClear(to) || world.standable(to.below())) return null;
        Pos placed = to.below();
        if (!world.cell(placed).replaceable() || !world.hasPlacementAnchor(placed)) return null;
        if (diagonal && !world.diagonalClear(from, to)) return null;
        return new Step(from, to, Kind.PLACE_BRIDGE, placed, diagonal ? 6.75 : 6.25);
    }

    private static Step breakStep(
            WorldView world,
            Pos from,
            Pos to,
            boolean diagonal,
            Capabilities caps
    ) {
        if (!caps.canBreak() || !world.standable(to.below())) return null;
        Cell feet = world.cell(to);
        Cell head = world.cell(to.above());
        Pos action;
        if (!feet.passable() && feet.breakable()) {
            action = to;
        } else if (!head.passable() && head.breakable()) {
            action = to.above();
        } else {
            return null;
        }
        if (diagonal && !world.diagonalClearIgnoring(from, to, action)) return null;
        return new Step(from, to, Kind.BREAK, action, 12.0);
    }

    private static void addParkour(WorldView world, Pos from, List<Step> out, Policy policy) {
        if (!world.standable(from.below())) return;
        for (int[] direction : DIRECTIONS) {
            int dx = direction[0];
            int dz = direction[1];
            Pos firstGap = from.add(dx, 0, dz);
            if (world.standable(firstGap.below())) continue;
            for (int distance = 2; distance <= policy.maxParkourDistance(); distance++) {
                for (int dy = 1; dy >= -2; dy--) {
                    Pos landing = from.add(dx * distance, dy, dz * distance);
                    if (!world.inBounds(landing)
                            || !world.bodyClear(landing)
                            || !world.standable(landing.below())) continue;
                    if (!world.parkourClear(from, landing)) continue;
                    double horizontal = Math.hypot(dx * distance, dz * distance);
                    out.add(new Step(from, landing, Kind.PARKOUR, null,
                            2.5 + horizontal * 0.7 + Math.max(0, dy) * 0.8));
                }
            }
        }
    }

    private static List<Step> reconstruct(Node end) {
        if (end == null || end.parent() == null) return List.of();
        List<Step> reversed = new ArrayList<>();
        Node cursor = end;
        while (cursor != null && cursor.via() != null) {
            reversed.add(cursor.via());
            cursor = cursor.parent();
        }
        Collections.reverse(reversed);
        return List.copyOf(reversed);
    }

    private static List<Step> safePartial(Node end) {
        List<Step> full = reconstruct(end);
        if (full.isEmpty()) return full;
        List<Step> safe = new ArrayList<>(full.size());
        for (Step step : full) {
            if (step.kind().changesWorld()) break;
            safe.add(step);
        }
        return List.copyOf(safe);
    }

    private static List<Step> oneActionPartial(Node end) {
        List<Step> full = reconstruct(end);
        if (full.isEmpty()) return full;
        List<Step> progress = new ArrayList<>(full.size());
        for (Step step : full) {
            progress.add(step);
            if (step.kind().changesWorld()) break;
        }
        return List.copyOf(progress);
    }

    private static double heuristic(Pos from, Pos goal, double radius) {
        return Math.max(0.0, from.distance(goal) - radius);
    }

    public enum Kind {
        WALK(false),
        DIAGONAL(false),
        STEP_UP(false),
        DROP(false),
        PARKOUR(false),
        CLUTCH_DROP(true),
        OPEN(true),
        PLACE_BRIDGE(true),
        PILLAR(true),
        BREAK(true);

        private final boolean changesWorld;

        Kind(boolean changesWorld) {
            this.changesWorld = changesWorld;
        }

        public boolean changesWorld() {
            return changesWorld;
        }
    }

    public enum Status {
        COMPLETE,
        PARTIAL,
        NO_ROUTE,
        NODE_BUDGET,
        TIME_BUDGET
    }

    public enum Phase {
        NO_BREAK,
        BREACH
    }

    public record Pos(int x, int y, int z) {
        public Pos add(int dx, int dy, int dz) {
            return new Pos(x + dx, y + dy, z + dz);
        }

        public Pos above() {
            return above(1);
        }

        public Pos above(int amount) {
            return add(0, amount, 0);
        }

        public Pos below() {
            return add(0, -1, 0);
        }

        public double distance(Pos other) {
            long dx = (long) x - other.x;
            long dy = (long) y - other.y;
            long dz = (long) z - other.z;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
    }

    public record Cell(
            boolean available,
            boolean passable,
            boolean standable,
            boolean hazard,
            boolean handOpenable,
            boolean breakable,
            boolean replaceable
    ) {
        public static final Cell UNAVAILABLE = new Cell(false, false, false, true,
                false, false, false);
        public static final Cell AIR = new Cell(true, true, false, false,
                false, false, true);
        public static final Cell SOLID = new Cell(true, false, true, false,
                false, true, false);
    }

    public interface WorldView {
        boolean inBounds(Pos pos);

        Cell cell(Pos pos);

        default boolean bodyClear(Pos feet) {
            Cell lower = cell(feet);
            Cell upper = cell(feet.above());
            return lower.available() && upper.available()
                    && lower.passable() && upper.passable()
                    && !lower.hazard() && !upper.hazard();
        }

        default boolean standable(Pos support) {
            Cell cell = cell(support);
            return cell.available() && cell.standable() && !cell.hazard();
        }

        default boolean hasPlacementAnchor(Pos pos) {
            return standable(pos.below())
                    || standable(pos.add(1, 0, 0))
                    || standable(pos.add(-1, 0, 0))
                    || standable(pos.add(0, 0, 1))
                    || standable(pos.add(0, 0, -1));
        }

        default boolean diagonalClear(Pos from, Pos to) {
            int dx = Integer.signum(to.x() - from.x());
            int dz = Integer.signum(to.z() - from.z());
            if (dx == 0 || dz == 0) return true;
            Pos xSide = new Pos(from.x() + dx, to.y(), from.z());
            Pos zSide = new Pos(from.x(), to.y(), from.z() + dz);
            return bodyClear(xSide) && bodyClear(zSide);
        }

        default boolean diagonalClearIgnoring(Pos from, Pos to, Pos ignored) {
            int dx = Integer.signum(to.x() - from.x());
            int dz = Integer.signum(to.z() - from.z());
            if (dx == 0 || dz == 0) return true;
            Pos xSide = new Pos(from.x() + dx, to.y(), from.z());
            Pos zSide = new Pos(from.x(), to.y(), from.z() + dz);
            return bodyClearIgnoring(xSide, ignored) && bodyClearIgnoring(zSide, ignored);
        }

        default boolean bodyClearIgnoring(Pos feet, Pos ignored) {
            return passableIgnoring(feet, ignored) && passableIgnoring(feet.above(), ignored);
        }

        private boolean passableIgnoring(Pos pos, Pos ignored) {
            if (pos.equals(ignored)) return true;
            Cell cell = cell(pos);
            return cell.available() && cell.passable() && !cell.hazard();
        }

        default boolean clearLine(Pos from, Pos to) {
            int samples = Math.max(1, (int) Math.ceil(from.distance(to) * 4.0));
            for (int i = 1; i < samples; i++) {
                double t = i / (double) samples;
                Pos at = new Pos(
                        (int) Math.floor(from.x() + 0.5 + (to.x() - from.x()) * t),
                        (int) Math.floor(from.y() + 0.5 + (to.y() - from.y()) * t),
                        (int) Math.floor(from.z() + 0.5 + (to.z() - from.z()) * t));
                Cell cell = cell(at);
                if (!cell.available() || !cell.passable()) return false;
            }
            return true;
        }

        default boolean parkourClear(Pos from, Pos to) {
            double horizontal = Math.hypot(to.x() - from.x(), to.z() - from.z());
            int ticks = Math.max(4, (int) Math.ceil(horizontal / 0.42));
            double vertical = (to.y() - from.y() + 0.04 * ticks * (ticks - 1)) / ticks;
            // This bot's normal jump impulse is 0.42. Reject routes that only
            // work by silently giving parkour a stronger-than-player launch.
            if (vertical < 0.30 || vertical > 0.42 + 1.0e-9) return false;
            for (int tick = 1; tick < ticks; tick++) {
                double t = tick / (double) ticks;
                double y = from.y() + vertical * tick - 0.04 * tick * (tick - 1);
                Pos feet = new Pos(
                        (int) Math.floor(from.x() + 0.5 + (to.x() - from.x()) * t),
                        (int) Math.floor(y),
                        (int) Math.floor(from.z() + 0.5 + (to.z() - from.z()) * t));
                if (!bodyClear(feet)) return false;
            }
            return true;
        }
    }

    public record Step(Pos from, Pos to, Kind kind, Pos actionPos, double cost) {
        public Step {
            Objects.requireNonNull(from, "from");
            Objects.requireNonNull(to, "to");
            Objects.requireNonNull(kind, "kind");
            if (kind.changesWorld() && actionPos == null) {
                throw new IllegalArgumentException("Action step requires an action position");
            }
            if (!Double.isFinite(cost) || cost <= 0.0) {
                throw new IllegalArgumentException("Step cost must be finite and positive");
            }
        }
    }

    public record Capabilities(
            boolean canOpen,
            boolean canPlace,
            boolean canPillar,
            boolean canParkour,
            boolean canClutch,
            boolean canBreak,
            int placeableBlocks
    ) {
        public static final Capabilities NONE = new Capabilities(false, false, false,
                false, false, false, 0);

        public Capabilities {
            placeableBlocks = Math.max(0, placeableBlocks);
        }
    }

    public record Policy(
            int maxNodes,
            long maxNanosPerPhase,
            int maxStepUp,
            int maxNormalDrop,
            int maxClutchDrop,
            int maxParkourDistance,
            boolean allowParkour,
            boolean allowClutchDrop,
            boolean allowPlace,
            boolean allowPillar,
            boolean allowBreak
    ) {
        public static final Policy DEFAULT = new Policy(
                1024,
                2_000_000L,
                1,
                3,
                48,
                4,
                true,
                true,
                false,
                false,
                false
        );

        public Policy {
            maxNodes = Math.max(1, Math.min(65_536, maxNodes));
            maxNanosPerPhase = Math.max(1L, Math.min(50_000_000L, maxNanosPerPhase));
            maxStepUp = Math.max(0, Math.min(1, maxStepUp));
            maxNormalDrop = Math.max(0, Math.min(3, maxNormalDrop));
            maxClutchDrop = Math.max(maxNormalDrop, Math.min(512, maxClutchDrop));
            maxParkourDistance = Math.max(2, Math.min(6, maxParkourDistance));
        }
    }

    public record Result(
            Status status,
            Phase phase,
            List<Step> steps,
            int expandedNodes,
            long elapsedNanos,
            boolean complete
    ) {
        public Result {
            status = status == null ? Status.NO_ROUTE : status;
            phase = phase == null ? Phase.NO_BREAK : phase;
            steps = steps == null ? List.of() : List.copyOf(steps);
            expandedNodes = Math.max(0, expandedNodes);
            elapsedNanos = Math.max(0L, elapsedNanos);
        }

        public boolean usable() {
            return !steps.isEmpty();
        }
    }

    private record Node(Pos pos, Node parent, Step via, double cost, double heuristic, long order) {
        double estimatedTotal() {
            return cost + heuristic;
        }
    }

    private record Search(Status status, List<Step> steps, int expanded, long elapsed, boolean complete) {
        boolean limitReached() {
            return status == Status.NODE_BUDGET || status == Status.TIME_BUDGET;
        }

        boolean endsWithAction() {
            return !steps.isEmpty() && steps.getLast().kind().changesWorld();
        }

        Result toResult(Phase phase) {
            Status exposed = status;
            if (!complete && !steps.isEmpty() && status == Status.NO_ROUTE) {
                exposed = Status.PARTIAL;
            }
            return new Result(exposed, phase, steps, expanded, elapsed, complete);
        }
    }
}
