package net.nuggetmc.tplus.api.agent.legacyagent;

import net.nuggetmc.tplus.api.Terminator;
import net.nuggetmc.tplus.api.agent.BotRuntimeSnapshot;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.Objects;
import java.util.UUID;

final class BotRuntime {

    private final Terminator bot;
    private final UUID id;
    private Location centeredLocation;
    private boolean sameBlockXZ;
    private Location towerOrigin;
    private Location stuckLastLocation;
    private int stuckTicks;
    private boolean boatCooldown;
    private boolean fallDamageCooldown;
    private UUID targetId;
    private String targetName = "";
    private double targetDistance = -1;
    private BotRuntimeSnapshot.MovementMode movementMode = BotRuntimeSnapshot.MovementMode.LEGACY;
    private long tickCount;
    private long targetChanges;

    BotRuntime(Terminator bot, UUID id) {
        this.bot = Objects.requireNonNull(bot, "bot");
        this.id = Objects.requireNonNull(id, "id");
    }

    void tick() {
        tickCount++;
    }

    Terminator bot() {
        return bot;
    }

    UUID id() {
        return id;
    }

    Location centeredLocation() {
        return centeredLocation;
    }

    void centeredLocation(Location location) {
        centeredLocation = cloneLocation(location);
    }

    boolean sameBlockXZ() {
        return sameBlockXZ;
    }

    void sameBlockXZ(boolean sameBlockXZ) {
        this.sameBlockXZ = sameBlockXZ;
    }

    Location towerOrigin() {
        return towerOrigin;
    }

    void towerOrigin(Location location) {
        towerOrigin = cloneLocation(location);
    }

    Location stuckLastLocation() {
        return stuckLastLocation;
    }

    void stuckLastLocation(Location location) {
        stuckLastLocation = cloneLocation(location);
    }

    int stuckTicks() {
        return stuckTicks;
    }

    void stuckTicks(int stuckTicks) {
        this.stuckTicks = Math.max(0, stuckTicks);
    }

    boolean boatCooldown() {
        return boatCooldown;
    }

    void boatCooldown(boolean boatCooldown) {
        this.boatCooldown = boatCooldown;
    }

    boolean fallDamageCooldown() {
        return fallDamageCooldown;
    }

    void fallDamageCooldown(boolean fallDamageCooldown) {
        this.fallDamageCooldown = fallDamageCooldown;
    }

    void observeTarget(LivingEntity target, LegacyAgent.MovementMode mode, Location botLocation) {
        UUID nextTargetId = target == null ? null : target.getUniqueId();
        if (!Objects.equals(targetId, nextTargetId)) {
            targetChanges++;
        }
        targetId = nextTargetId;
        targetName = target == null ? "" : target.getName();
        targetDistance = distance(botLocation, target);
        movementMode = BotRuntimeSnapshot.MovementMode.valueOf(mode.name());
    }

    void clearIdleTracking() {
        stuckTicks = 0;
        stuckLastLocation = null;
    }

    void clearTransient() {
        centeredLocation = null;
        sameBlockXZ = false;
        towerOrigin = null;
        clearIdleTracking();
        boatCooldown = false;
        fallDamageCooldown = false;
        targetId = null;
        targetName = "";
        targetDistance = -1;
    }

    BotRuntimeSnapshot snapshot() {
        return new BotRuntimeSnapshot(id, bot.getBotName(), bot.isBotAlive(), targetId,
                targetName, targetDistance, movementMode, stuckTicks, tickCount, targetChanges);
    }

    private static double distance(Location botLocation, LivingEntity target) {
        if (target == null || botLocation == null || botLocation.getWorld() != target.getWorld()) {
            return -1;
        }
        return botLocation.distance(target.getLocation());
    }

    private static Location cloneLocation(Location location) {
        return location == null ? null : location.clone();
    }
}
