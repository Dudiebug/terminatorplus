package net.nuggetmc.tplus.api.agent.legacyagent;

import net.nuggetmc.tplus.api.Terminator;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

/**
 * Applies locomotion for the selected movement mode. Implementations must not
 * select weapons, mutate loadouts, use items, or attack.
 */
interface MovementStrategy {

    void move(
            Terminator bot,
            LivingEntity target,
            Location botLocation,
            Location targetLocation,
            LegacyAgent.MovementMode mode,
            boolean allowMovement
    );
}
