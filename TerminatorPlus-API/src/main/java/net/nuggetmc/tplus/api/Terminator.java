package net.nuggetmc.tplus.api;

import com.mojang.authlib.GameProfile;
import net.nuggetmc.tplus.api.agent.legacyagent.ai.NeuralNetwork;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.UUID;

public interface Terminator {

    String getBotName();

    int getEntityId();

    GameProfile getGameProfile();

    LivingEntity getBukkitEntity();

    NeuralNetwork getNeuralNetwork();

    void setNeuralNetwork(NeuralNetwork neuralNetwork);

    boolean hasNeuralNetwork();

    Location getLocation();

    BoundingBox getBotBoundingBox();

    boolean isBotAlive(); //Has to be named like this because paper re-obfuscates it

    float getBotHealth();

    float getBotMaxHealth();

    boolean isBotOnFire();

    boolean isFalling();

    boolean isBotBlocking();

    void block(int length, int cooldown);

    boolean isBotInWater();

    boolean isBotOnGround();

    List<Block> getStandingOn();

    void setBotPitch(float pitch);

    default void setBotXRot(float pitch) {
        setBotPitch(pitch);
    }

    void jump(Vector velocity);

    void jump();

    void walk(Vector velocity);

    void look(BlockFace face);

    void faceLocation(Location location);

    void attack(Entity target);

    void attemptBlockPlace(Location loc, Material type, boolean down);

    void punch();

    void swim();

    void sneak();

    void stand();

    void addFriction(double factor);

    void removeVisually();

    void removeBot();

    int getKills();

    void incrementKills();

    void setItem(ItemStack item);

    void setItem(ItemStack item, EquipmentSlot slot);

    void setItemOffhand(ItemStack item);

    void setDefaultItem(ItemStack item);

    Vector getOffset();

    Vector getVelocity();

    void setVelocity(Vector velocity);

    void addVelocity(Vector velocity);

    int getAliveTicks();

    int getNoFallTicks();

    boolean tickDelay(int ticks);

    void renderBot(Object packetListener, boolean login);

    UUID getTargetPlayer();

    void setTargetPlayer(UUID target);

    boolean isInPlayerList();

    World.Environment getDimension();

    void setShield(boolean b);

    /**
     * Runs the combat director for this bot against the given target,
     * picking a hotbar weapon and triggering the appropriate behavior
     * (melee, trident, mace, wind charge, elytra+firework, etc).
     *
     * @return true if combat was handled; false if the caller should
     *         fall back to its default attack/targeting logic.
     */
    default boolean combatTick(LivingEntity target) {
        return false;
    }

    default boolean usesMovementController() {
        return false;
    }

    default void planCombat(LivingEntity target) {
    }

    default boolean tryMovementControllerMove(LivingEntity target) {
        return false;
    }

    default boolean executePlannedCombat(LivingEntity target) {
        return combatTick(target);
    }

    /**
     * Vanilla-safe swing gate: true only when the bot's attack-strength charge is
     * fully recharged AND the target isn't deep in an i-frame window. Callers
     * that go through {@link #attack(Entity)} directly (legacy agent, bot agent,
     * etc) should consult this before swinging so the bot doesn't waste 75% of
     * its damage on a partially-charged strike.
     *
     * <p>Default returns true so unimplemented adapters don't silently block
     * attacks; real bot impls ({@link net.nuggetmc.tplus.api.Terminator} → {@code Bot})
     * back this with the actual attack-strength ticker check.
     */
    default boolean canSwingAttack(LivingEntity target) {
        return true;
    }
}
