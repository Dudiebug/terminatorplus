package net.nuggetmc.tplus.api.agent.legacyagent;

import org.bukkit.Location;
import org.bukkit.World;

record NavNode(int x, int y, int z) {
    static NavNode from(Location location) {
        return new NavNode(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    Location center(World world) {
        return new Location(world, x + 0.5, y, z + 0.5);
    }

    double distanceSquared(Location location) {
        double dx = x + 0.5 - location.getX();
        double dy = y - location.getY();
        double dz = z + 0.5 - location.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    double horizontalDistanceSquared(Location location) {
        double dx = x + 0.5 - location.getX();
        double dz = z + 0.5 - location.getZ();
        return dx * dx + dz * dz;
    }
}
