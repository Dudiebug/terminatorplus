package net.nuggetmc.tplus.api.agent.legacyagent;

import net.nuggetmc.tplus.api.Terminator;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

/**
 * Compatibility movement strategy. It preserves full replacement NN behavior,
 * keeps movement-controller NN as the first choice, and replaces the old direct
 * legacy chase fallback with terrain-aware navigation.
 */
final class LegacyMovementStrategy implements MovementStrategy {

    private final LegacyAgent legacy;
    private final NavigationController navigation;

    LegacyMovementStrategy(LegacyAgent legacy) {
        this.legacy = legacy;
        this.navigation = new NavigationController(legacy);
    }

    @Override
    public void move(
            Terminator bot,
            LivingEntity target,
            Location botLocation,
            Location targetLocation,
            LegacyAgent.MovementMode mode,
            boolean allowMovement
    ) {
        if (mode == LegacyAgent.MovementMode.FULL_REPLACEMENT_NN) {
            legacy.move(bot, target, botLocation, targetLocation, mode, allowMovement);
            return;
        }

        if (mode == LegacyAgent.MovementMode.MOVEMENT_CONTROLLER_NN) {
            if (allowMovement && bot.tryMovementControllerMove(target)) {
                return;
            }
            if (navigation.handlesMovementControllerFallback()
                    && navigation.move(bot, target, botLocation, targetLocation, mode, allowMovement)) {
                return;
            }
            legacy.move(bot, target, botLocation, targetLocation, LegacyAgent.MovementMode.LEGACY, allowMovement);
            return;
        }

        if (navigation.move(bot, target, botLocation, targetLocation, mode, allowMovement)) {
            return;
        }
        legacy.move(bot, target, botLocation, targetLocation, mode, allowMovement);
    }
}
