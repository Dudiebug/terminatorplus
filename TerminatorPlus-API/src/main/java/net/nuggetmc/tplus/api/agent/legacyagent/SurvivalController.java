package net.nuggetmc.tplus.api.agent.legacyagent;

import net.nuggetmc.tplus.api.Terminator;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

/**
 * Owns non-combat survival and environment reactions. This includes legacy
 * clutch, fall, mining-cleanup, fire, lava, and stuck/idle housekeeping.
 */
interface SurvivalController {

    void beforeTarget(Terminator bot, Location botLocation);

    void onIdle(Terminator bot);

    void beforeMovement(Terminator bot, LivingEntity target);
}
