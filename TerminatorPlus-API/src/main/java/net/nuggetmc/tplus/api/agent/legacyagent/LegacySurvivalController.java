package net.nuggetmc.tplus.api.agent.legacyagent;

import net.nuggetmc.tplus.api.Terminator;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

/**
 * Compatibility survival controller. It delegates to the same LegacyAgent and
 * LegacyBlockCheck methods that owned these behaviors before extraction.
 */
final class LegacySurvivalController implements SurvivalController {

    private final LegacyAgent legacy;

    LegacySurvivalController(LegacyAgent legacy) {
        this.legacy = legacy;
    }

    @Override
    public void beforeTarget(Terminator bot, Location botLocation) {
        legacy.blockCheck().tryPreMLG(bot, botLocation);
    }

    @Override
    public void onIdle(Terminator bot) {
        legacy.stopMining(bot);
        legacy.clearIdleTracking(bot);
    }

    @Override
    public void beforeMovement(Terminator bot, LivingEntity target) {
        legacy.blockCheck().clutch(bot, target);
        legacy.fallDamageCheck(bot);
        legacy.miscellaneousChecks(bot, target);
    }
}
