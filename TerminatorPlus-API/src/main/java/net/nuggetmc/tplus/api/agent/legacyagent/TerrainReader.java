package net.nuggetmc.tplus.api.agent.legacyagent;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Gate;
import org.bukkit.block.data.type.TrapDoor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class TerrainReader {
    private final NavigationConfig config;

    TerrainReader(NavigationConfig config) {
        this.config = config;
    }

    TraversalPlan planOccupy(World world, NavNode node, TerrainPathfinder.SearchMode mode) {
        Block feet = world.getBlockAt(node.x(), node.y(), node.z());
        Block head = world.getBlockAt(node.x(), node.y() + 1, node.z());
        Block below = world.getBlockAt(node.x(), node.y() - 1, node.z());

        if (!canStandOn(below)) {
            return TraversalPlan.blocked("no-floor");
        }

        Clearance feetClearance = clearance(feet, mode, "feet");
        if (!feetClearance.passable()) {
            return TraversalPlan.blocked(feetClearance.reason());
        }
        Clearance headClearance = clearance(head, mode, "head");
        if (!headClearance.passable()) {
            return TraversalPlan.blocked(headClearance.reason());
        }

        Map<BlockKey, NavAction> deduped = new LinkedHashMap<>();
        feetClearance.actions().forEach(action -> deduped.put(action.block(), action));
        headClearance.actions().forEach(action -> deduped.put(action.block(), action));
        return TraversalPlan.passable(new ArrayList<>(deduped.values()), feetClearance.cost() + headClearance.cost());
    }

    boolean canOccupyNow(World world, NavNode node) {
        return planOccupy(world, node, TerrainPathfinder.SearchMode.NO_BREAK).passable();
    }

    boolean actionStillRequired(World world, NavAction action) {
        Block block = action.block().block(world);
        if (action.type() == NavAction.Type.OPEN) {
            BlockData data = block.getBlockData();
            return data instanceof Openable openable && !openable.isOpen();
        }
        if (action.type() == NavAction.Type.BREAK) {
            return !isPassThrough(block) && canBreak(block.getType());
        }
        return false;
    }

    boolean open(Block block) {
        BlockData data = block.getBlockData();
        if (!(data instanceof Openable openable)) return false;
        if (openable.isOpen()) return false;
        openable.setOpen(true);
        block.setBlockData(data, true);
        syncDoorHalf(block, data);
        return true;
    }

    private static void syncDoorHalf(Block block, BlockData data) {
        if (!(data instanceof Door door)) return;
        Block other = door.getHalf() == org.bukkit.block.data.Bisected.Half.TOP
                ? block.getRelative(BlockFace.DOWN)
                : block.getRelative(BlockFace.UP);
        BlockData otherData = other.getBlockData();
        if (otherData instanceof Door otherDoor) {
            otherDoor.setOpen(true);
            other.setBlockData(otherData, true);
        }
    }

    private Clearance clearance(Block block, TerrainPathfinder.SearchMode mode, String slot) {
        if (isHazard(block)) {
            return Clearance.blocked("hazard-" + block.getType().name().toLowerCase(java.util.Locale.ROOT));
        }
        if (isPassThrough(block)) {
            return Clearance.passable(List.of(), passThroughCost(block));
        }
        if (isOpenable(block) && canOpen(block)) {
            return Clearance.passable(List.of(new NavAction(NavAction.Type.OPEN, BlockKey.of(block), "open-" + slot)), 1.2);
        }
        if (mode == TerrainPathfinder.SearchMode.ALLOW_BREAK && canBreak(block.getType())) {
            return Clearance.passable(List.of(new NavAction(NavAction.Type.BREAK, BlockKey.of(block), "break-" + slot)), breakCost(block.getType()));
        }
        return Clearance.blocked("blocked-" + block.getType().name().toLowerCase(java.util.Locale.ROOT));
    }

    private boolean canOpen(Block block) {
        BlockData data = block.getBlockData();
        if (data instanceof Door) return config.allowDoors();
        if (data instanceof TrapDoor) return config.allowTrapdoors();
        if (data instanceof Gate) return config.allowGates();
        return false;
    }

    private boolean isOpenable(Block block) {
        return block.getBlockData() instanceof Openable;
    }

    boolean isHazard(Block block) {
        if (!config.avoidHazards()) return false;
        Material type = block.getType();
        return type == Material.LAVA
                || type == Material.FIRE
                || type == Material.SOUL_FIRE
                || type == Material.CACTUS
                || type == Material.MAGMA_BLOCK
                || type == Material.SWEET_BERRY_BUSH
                || type == Material.POWDER_SNOW
                || type.name().endsWith("_CAMPFIRE");
    }

    boolean isPassThrough(Block block) {
        Material type = block.getType();
        if (type == Material.LAVA) return false;
        if (LegacyMats.BREAK.contains(type)) return true;
        if (block.getBlockData() instanceof Openable openable) {
            return openable.isOpen();
        }
        if (LegacyMats.FENCE.contains(type) || LegacyMats.GATES.contains(type) || LegacyMats.OBSTACLES.contains(type)) {
            return false;
        }
        return !type.isSolid() && !LegacyMats.canStandOn(type);
    }

    boolean canStandOn(Block block) {
        if (isHazard(block)) return false;
        Material type = block.getType();
        if (type == Material.WATER || type == Material.LAVA) return false;
        if (block.getBlockData() instanceof Openable openable && openable.isOpen()) return false;
        return type.isSolid() || LegacyMats.canStandOn(type);
    }

    boolean canBreak(Material type) {
        if (type == null || LegacyMats.BREAK.contains(type)) return false;
        return type != Material.BEDROCK
                && type != Material.BARRIER
                && type != Material.END_PORTAL_FRAME
                && type != Material.STRUCTURE_BLOCK
                && type != Material.COMMAND_BLOCK
                && type != Material.REPEATING_COMMAND_BLOCK
                && type != Material.CHAIN_COMMAND_BLOCK;
    }

    double breakCost(Material type) {
        String name = type.name();
        if (LegacyMats.INSTANT_BREAK.contains(type)) return 8.0;
        if (name.contains("OBSIDIAN")) return 900.0;
        if (name.contains("DEEPSLATE")) return 180.0;
        if (name.contains("STONE") || name.contains("COBBLE") || name.contains("BRICK")) return 120.0;
        if (name.contains("DIRT") || name.contains("GRASS") || name.contains("SAND") || name.contains("GRAVEL")) return 55.0;
        if (name.contains("LOG") || name.contains("PLANK") || name.contains("WOOD") || name.contains("STEM")) return 75.0;
        if (name.contains("WOOL") || name.contains("LEAVES")) return 35.0;
        return type.isSolid() ? 90.0 : 25.0;
    }

    private double passThroughCost(Block block) {
        Material type = block.getType();
        if (type == Material.WATER) return 3.0;
        if (type == Material.COBWEB) return 12.0;
        return 0.0;
    }

    record TraversalPlan(boolean passable, List<NavAction> actions, double extraCost, String reason) {
        TraversalPlan {
            actions = actions == null ? List.of() : List.copyOf(actions);
            reason = reason == null ? "" : reason;
        }

        static TraversalPlan passable(List<NavAction> actions, double extraCost) {
            return new TraversalPlan(true, actions, extraCost, "");
        }

        static TraversalPlan blocked(String reason) {
            return new TraversalPlan(false, List.of(), 0.0, reason);
        }
    }

    private record Clearance(boolean passable, List<NavAction> actions, double cost, String reason) {
        Clearance {
            actions = actions == null ? List.of() : List.copyOf(actions);
            reason = reason == null ? "" : reason;
        }

        static Clearance passable(List<NavAction> actions, double cost) {
            return new Clearance(true, actions, cost, "");
        }

        static Clearance blocked(String reason) {
            return new Clearance(false, List.of(), 0.0, reason);
        }
    }
}
