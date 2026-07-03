package net.nuggetmc.tplus.api.agent.legacyagent;

import net.nuggetmc.tplus.api.Terminator;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Gate;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class NavigationController {
    private static final double ARRIVAL_DISTANCE_SQ = 0.42 * 0.42;
    private static final long PLAN_TTL_NANOS = 300_000_000_000L;

    private final LegacyAgent legacy;
    private final Map<UUID, ActivePlan> activePlans = new HashMap<>();

    NavigationController(LegacyAgent legacy) {
        this.legacy = legacy;
    }

    boolean handlesMovementControllerFallback() {
        return NavigationConfig.load().applyToMovementControllerFallback();
    }

    boolean move(Terminator bot, LivingEntity target, Location botLocation, Location targetLocation,
                 LegacyAgent.MovementMode mode, boolean allowMovement) {
        NavigationConfig config = NavigationConfig.load();
        if (!config.enabled() || !allowMovement || bot == null || target == null || !target.isValid()) return false;
        if (mode == LegacyAgent.MovementMode.FULL_REPLACEMENT_NN) return false;
        if (mode == LegacyAgent.MovementMode.MOVEMENT_CONTROLLER_NN && !config.applyToMovementControllerFallback()) return false;

        prunePlans();
        UUID id = bot.getBukkitEntity().getUniqueId();
        TerrainReader terrain = new TerrainReader(config);
        TerrainPathfinder pathfinder = new TerrainPathfinder(terrain, config);

        ActivePlan plan = activePlans.get(id);
        if (shouldReplan(bot, target, terrain, plan, config)) {
            Optional<NavPath> next = pathfinder.findBestPath(bot, target);
            plan = next.map(path -> new ActivePlan(path, target.getLocation(), bot.getAliveTicks()))
                    .orElseGet(() -> ActivePlan.failed(target.getLocation(), bot.getAliveTicks()));
            activePlans.put(id, plan);
        }
        plan.lastTouchNanos = System.nanoTime();

        if (plan.path == null || plan.path.empty()) {
            if (config.suppressLegacyFallback()) {
                exploratoryRecovery(bot, target, botLocation);
                return true;
            }
            return false;
        }

        if (!plan.path.breakEnabled()) legacy.stopMining(bot);
        return followPath(bot, target, terrain, plan);
    }

    private boolean shouldReplan(Terminator bot, LivingEntity target, TerrainReader terrain,
                                 ActivePlan plan, NavigationConfig config) {
        if (plan == null || plan.path == null) return true;
        if (!sameWorld(plan.targetSnapshot, target.getLocation())) return true;
        if (plan.targetSnapshot.distanceSquared(target.getLocation()) > 6.25) return true;
        if (bot.getAliveTicks() - plan.createdTick >= config.replanIntervalTicks()) return true;
        if (plan.path.empty()) return false;
        if (plan.finished()) return true;

        NavStep next = plan.nextStep();
        if (next == null) return true;
        World world = bot.getBukkitEntity().getWorld();
        if (!next.actions().isEmpty()) return false;
        return !terrain.canOccupyNow(world, next.node());
    }

    private boolean followPath(Terminator bot, LivingEntity target, TerrainReader terrain, ActivePlan plan) {
        World world = bot.getBukkitEntity().getWorld();
        Location loc = bot.getLocation();
        plan.advancePastReached(loc);
        if (plan.finished()) {
            bot.faceLocation(target.getLocation());
            bot.walk(new Vector(0, 0, 0));
            return true;
        }

        NavStep step = plan.nextStep();
        for (NavAction action : step.actions()) {
            if (!terrain.actionStillRequired(world, action)) continue;
            executeAction(bot, terrain, world, action, plan.path.breakEnabled());
            return true;
        }

        Location waypoint = step.node().center(world);
        Vector flat = waypoint.toVector().subtract(loc.toVector()).setY(0.0);
        if (flat.lengthSquared() <= ARRIVAL_DISTANCE_SQ) {
            plan.index++;
            return true;
        }

        Vector direction = flat.clone();
        if (direction.lengthSquared() > 1.0e-9) direction.normalize();
        double distanceToTarget = loc.distance(target.getLocation());
        double speed = distanceToTarget > 5.0 ? 0.40 : 0.30;
        Vector move = direction.multiply(speed);
        boolean ascending = waypoint.getBlockY() > loc.getBlockY();

        if (bot.tickDelay(4)) bot.faceLocation(distanceToTarget <= 5.0 ? target.getLocation() : waypoint);
        if (!bot.isBotOnGround()) {
            bot.walk(move);
            return true;
        }

        bot.stand();
        if (ascending) {
            move.setY(0.42);
            bot.jump(move);
        } else {
            bot.walk(move);
        }
        return true;
    }

    private void executeAction(Terminator bot, TerrainReader terrain, World world, NavAction action, boolean breakEnabled) {
        Block block = action.block().block(world);
        bot.faceLocation(action.block().center(world));
        if (action.type() == NavAction.Type.OPEN) {
            if (terrain.open(block)) world.playSound(block.getLocation(), soundForOpen(block), SoundCategory.BLOCKS, 0.8f, 1.0f);
            return;
        }
        if (action.type() == NavAction.Type.BREAK && breakEnabled) {
            if (!terrain.canBreak(block.getType())) return;
            bot.punch();
            Sound sound = LegacyUtils.breakBlockSound(block);
            if (sound != null) world.playSound(block.getLocation(), sound, SoundCategory.BLOCKS, 0.7f, 1.0f);
            block.breakNaturally();
        }
    }

    private static Sound soundForOpen(Block block) {
        BlockData data = block.getBlockData();
        if (data instanceof Gate) return Sound.BLOCK_FENCE_GATE_OPEN;
        if (data instanceof TrapDoor) return Sound.BLOCK_WOODEN_TRAPDOOR_OPEN;
        return Sound.BLOCK_WOODEN_DOOR_OPEN;
    }

    private void exploratoryRecovery(Terminator bot, LivingEntity target, Location botLocation) {
        legacy.stopMining(bot);
        Vector toTarget = target.getLocation().toVector().subtract(botLocation.toVector()).setY(0.0);
        if (toTarget.lengthSquared() < 1.0e-9) toTarget = botLocation.getDirection().setY(0.0);
        if (toTarget.lengthSquared() < 1.0e-9) toTarget = new Vector(1, 0, 0);
        toTarget.normalize();
        double side = bot.tickDelay(20) ? 1.0 : -1.0;
        Vector sidestep = new Vector(-toTarget.getZ(), 0, toTarget.getX()).multiply(0.32 * side);
        Vector forward = toTarget.multiply(0.12);
        Vector move = forward.add(sidestep);
        bot.faceLocation(target.getLocation());
        if (bot.isBotOnGround()) {
            move.setY(0.42);
            bot.jump(move);
        } else {
            move.setY(0.0);
            bot.walk(move);
        }
    }

    private static boolean sameWorld(Location a, Location b) {
        return a != null && b != null && a.getWorld() == b.getWorld();
    }

    private void prunePlans() {
        long now = System.nanoTime();
        Iterator<Map.Entry<UUID, ActivePlan>> iterator = activePlans.entrySet().iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().getValue().lastTouchNanos > PLAN_TTL_NANOS) iterator.remove();
        }
    }

    private static final class ActivePlan {
        final NavPath path;
        final Location targetSnapshot;
        final int createdTick;
        int index;
        long lastTouchNanos;

        ActivePlan(NavPath path, Location targetSnapshot, int createdTick) {
            this.path = path;
            this.targetSnapshot = targetSnapshot.clone();
            this.createdTick = createdTick;
            this.index = path.steps().size() > 1 ? 1 : 0;
            this.lastTouchNanos = System.nanoTime();
        }

        static ActivePlan failed(Location targetSnapshot, int createdTick) {
            return new ActivePlan(new NavPath(TerrainPathfinder.SearchMode.NO_BREAK, java.util.List.of(), 0, "failed"), targetSnapshot, createdTick);
        }

        boolean finished() {
            return index >= path.steps().size();
        }

        NavStep nextStep() {
            return finished() ? null : path.steps().get(index);
        }

        void advancePastReached(Location loc) {
            while (!finished() && nextStep().node().distanceSquared(loc) <= ARRIVAL_DISTANCE_SQ) index++;
        }
    }
}
