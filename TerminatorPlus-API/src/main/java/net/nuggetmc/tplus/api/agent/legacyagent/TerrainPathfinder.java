package net.nuggetmc.tplus.api.agent.legacyagent;

import net.nuggetmc.tplus.api.Terminator;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;

final class TerrainPathfinder {
    private static final int[][] CARDINAL_AND_DIAGONAL = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    private final TerrainReader terrain;
    private final NavigationConfig config;

    TerrainPathfinder(TerrainReader terrain, NavigationConfig config) {
        this.terrain = terrain;
        this.config = config;
    }

    Optional<NavPath> findBestPath(Terminator bot, LivingEntity target) {
        World world = bot.getBukkitEntity().getWorld();
        NavNode start = NavNode.from(bot.getLocation());
        GoalRegion goal = GoalRegion.from(target, config);

        Optional<NavPath> noBreak = search(world, start, goal, SearchMode.NO_BREAK);
        if (noBreak.isPresent()) return noBreak;
        if (!config.allowBreakFallback()) return Optional.empty();
        return search(world, start, goal, SearchMode.ALLOW_BREAK);
    }

    private Optional<NavPath> search(World world, NavNode start, GoalRegion goal, SearchMode mode) {
        PriorityQueue<SearchNode> open = new PriorityQueue<>(Comparator.comparingDouble(SearchNode::estimatedTotal));
        Map<NavNode, SearchNode> best = new HashMap<>();
        SearchNode root = new SearchNode(start, null, null, 0.0, heuristic(start, goal), 0);
        open.add(root);
        best.put(start, root);

        int considered = 0;
        while (!open.isEmpty() && considered < config.maxNodes()) {
            SearchNode current = open.poll();
            SearchNode bestKnown = best.get(current.node());
            if (bestKnown != current) continue;
            considered++;

            if (goal.satisfiedBy(world, terrain, current.node())) {
                return Optional.of(reconstruct(current, mode, considered, "goal"));
            }

            for (Edge edge : neighbors(world, current.node(), start, goal, mode)) {
                double nextCost = current.cost() + edge.cost();
                SearchNode previous = best.get(edge.to());
                if (previous != null && previous.cost() <= nextCost) continue;
                SearchNode next = new SearchNode(edge.to(), current, edge.step(), nextCost,
                        nextCost + heuristic(edge.to(), goal), current.depth() + 1);
                best.put(edge.to(), next);
                open.add(next);
            }
        }
        return Optional.empty();
    }

    private List<Edge> neighbors(World world, NavNode node, NavNode start, GoalRegion goal, SearchMode mode) {
        List<Edge> out = new ArrayList<>();
        for (int[] dir : CARDINAL_AND_DIAGONAL) {
            int dx = dir[0];
            int dz = dir[1];
            addCandidate(world, out, node, start, goal, mode, dx, 0, dz, "walk");
            addCandidate(world, out, node, start, goal, mode, dx, 1, dz, "ascend");
            addCandidate(world, out, node, start, goal, mode, dx, -1, dz, "descend");
            for (int drop = 2; drop <= config.maxDropBlocks(); drop++) {
                addCandidate(world, out, node, start, goal, mode, dx, -drop, dz, "drop" + drop);
            }
        }
        return out;
    }

    private void addCandidate(
            World world,
            List<Edge> out,
            NavNode from,
            NavNode start,
            GoalRegion goal,
            SearchMode mode,
            int dx,
            int dy,
            int dz,
            String moveType
    ) {
        NavNode to = new NavNode(from.x() + dx, from.y() + dy, from.z() + dz);
        if (!withinBounds(to, start, goal)) return;
        if (dy > 1 || dy < -config.maxDropBlocks()) return;
        if (dx != 0 && dz != 0 && !diagonalAllowed(world, from, dx, dz, mode)) return;

        TerrainReader.TraversalPlan plan = terrain.planOccupy(world, to, mode);
        if (!plan.passable()) return;

        double horizontal = dx != 0 && dz != 0 ? 1.414 : 1.0;
        double vertical = dy > 0 ? 1.6 * dy : dy < 0 ? 0.4 * -dy : 0.0;
        double modePenalty = mode == SearchMode.ALLOW_BREAK && !plan.actions().isEmpty() ? 20.0 : 0.0;
        double cost = horizontal + vertical + plan.extraCost() + modePenalty;
        out.add(new Edge(to, new NavStep(to, plan.actions(), moveType, cost), cost));
    }

    private boolean diagonalAllowed(World world, NavNode from, int dx, int dz, SearchMode mode) {
        NavNode sideA = new NavNode(from.x() + dx, from.y(), from.z());
        NavNode sideB = new NavNode(from.x(), from.y(), from.z() + dz);
        // Do not clip through a corner. In break mode this still permits planned
        // diagonal movement if one side is openable/breakable, but never through two
        // fully blocked sides.
        return terrain.planOccupy(world, sideA, mode).passable()
                || terrain.planOccupy(world, sideB, mode).passable();
    }

    private boolean withinBounds(NavNode node, NavNode start, GoalRegion goal) {
        double max = config.maxSearchDistance();
        double sx = node.x() - start.x();
        double sy = node.y() - start.y();
        double sz = node.z() - start.z();
        if (sx * sx + sy * sy + sz * sz > max * max) return false;

        Location predicted = goal.predictedTarget();
        double gx = node.x() + 0.5 - predicted.getX();
        double gy = node.y() - predicted.getY();
        double gz = node.z() + 0.5 - predicted.getZ();
        return gx * gx + gy * gy + gz * gz <= (max + goal.radius()) * (max + goal.radius());
    }

    private static double heuristic(NavNode node, GoalRegion goal) {
        Location target = goal.predictedTarget();
        double dx = node.x() + 0.5 - target.getX();
        double dz = node.z() + 0.5 - target.getZ();
        double dy = Math.abs(node.y() - target.getY());
        return Math.sqrt(dx * dx + dz * dz) + dy * 1.4;
    }

    private static NavPath reconstruct(SearchNode end, SearchMode mode, int considered, String reason) {
        List<NavStep> reversed = new ArrayList<>();
        SearchNode current = end;
        while (current != null) {
            if (current.stepFromParent() == null) {
                reversed.add(new NavStep(current.node(), List.of(), "start", 0.0));
            } else {
                reversed.add(current.stepFromParent());
            }
            current = current.parent();
        }
        List<NavStep> steps = new ArrayList<>(reversed.size());
        for (int i = reversed.size() - 1; i >= 0; i--) {
            steps.add(reversed.get(i));
        }
        return new NavPath(mode, steps, considered, reason);
    }

    enum SearchMode {
        NO_BREAK,
        ALLOW_BREAK
    }

    private record Edge(NavNode to, NavStep step, double cost) {
    }

    private record SearchNode(
            NavNode node,
            SearchNode parent,
            NavStep stepFromParent,
            double cost,
            double estimatedTotal,
            int depth
    ) {
    }

    private record GoalRegion(Location target, Location predictedTarget, double radius, double losRadius) {
        static GoalRegion from(LivingEntity target, NavigationConfig config) {
            Location current = target.getLocation();
            Location predicted = current.clone();
            Vector velocity = target.getVelocity().clone().setY(0);
            velocity.multiply(config.targetPredictTicks());
            double maxLead = 4.0;
            if (velocity.lengthSquared() > maxLead * maxLead) {
                velocity.normalize().multiply(maxLead);
            }
            predicted.add(velocity);
            predicted.setY(current.getY());
            return new GoalRegion(current, predicted, config.goalRadius(), config.lineOfSightGoalRadius());
        }

        boolean satisfiedBy(World world, TerrainReader terrain, NavNode node) {
            if (!terrain.canOccupyNow(world, node)) return false;
            Location feet = node.center(world);
            double distanceSq = feet.distanceSquared(target);
            double maxGoal = Math.max(radius, losRadius);
            if (distanceSq > maxGoal * maxGoal) return false;

            Location eye = feet.clone().add(0.0, 1.62, 0.0);
            Location targetEye = target.clone().add(0.0, 1.0, 0.0);
            return LegacyUtils.checkFreeSpace(eye, targetEye);
        }
    }
}
