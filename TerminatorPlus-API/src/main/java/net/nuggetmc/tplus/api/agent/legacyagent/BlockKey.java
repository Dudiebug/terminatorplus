package net.nuggetmc.tplus.api.agent.legacyagent;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

record BlockKey(int x, int y, int z) {
    static BlockKey of(Block block) {
        return new BlockKey(block.getX(), block.getY(), block.getZ());
    }

    Block block(World world) {
        return world.getBlockAt(x, y, z);
    }

    Location center(World world) {
        return new Location(world, x + 0.5, y + 0.5, z + 0.5);
    }
}
