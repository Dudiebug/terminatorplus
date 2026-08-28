package net.nuggetmc.tplus.bot.navigation;

import net.nuggetmc.tplus.TerminatorPlus;
import net.nuggetmc.tplus.bot.Bot;

/** Runtime feature gate shared by commands, UI, and bots. */
public final class MovementV2Settings {

    public static final String CONFIG_PATH = "ai.movement.v2.enabled";

    private MovementV2Settings() {
    }

    public static boolean isEnabled(TerminatorPlus plugin) {
        return plugin.getConfig().getBoolean(CONFIG_PATH, false);
    }

    public static void setEnabled(TerminatorPlus plugin, boolean enabled) {
        plugin.getConfig().set(CONFIG_PATH, enabled);
        plugin.saveConfig();
        if (!enabled) {
            plugin.getManager().fetch().stream()
                    .filter(Bot.class::isInstance)
                    .map(Bot.class::cast)
                    .forEach(bot -> bot.cancelMovementV2Action("movement-v2-disabled"));
        }
    }
}
