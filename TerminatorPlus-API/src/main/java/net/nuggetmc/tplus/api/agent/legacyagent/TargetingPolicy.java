package net.nuggetmc.tplus.api.agent.legacyagent;

import net.nuggetmc.tplus.api.Terminator;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

/**
 * Owns target acquisition. Combat and movement consume the selected target but
 * do not search for or validate targets themselves.
 */
interface TargetingPolicy {

    LivingEntity selectTarget(Terminator bot, Location botLocation);
}
