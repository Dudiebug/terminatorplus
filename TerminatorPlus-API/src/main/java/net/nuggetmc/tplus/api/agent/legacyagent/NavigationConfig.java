package net.nuggetmc.tplus.api.agent.legacyagent;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

record NavigationConfig(
        boolean enabled,
        boolean applyToMovementControllerFallback,
        boolean suppressLegacyFallback,
        boolean allowBreakFallback,
        boolean allowDoors,
        boolean allowTrapdoors,
        boolean allowGates,
        boolean avoidHazards,
        int maxNodes,
        int replanIntervalTicks,
        int targetPredictTicks,
        int maxDropBlocks,
        double maxSearchDistance,
        double goalRadius,
        double lineOfSightGoalRadius
) {
    private static final String ROOT = "ai.navigation.";

    static NavigationConfig load() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("TerminatorPlus");
        if (plugin == null) {
            return defaults();
        }
        return new NavigationConfig(
                bool(plugin, ROOT + "enabled", true),
                bool(plugin, ROOT + "apply-to-movement-controller-fallback", true),
                bool(plugin, ROOT + "suppress-legacy-fallback", true),
                bool(plugin, ROOT + "allow-break-fallback", true),
                bool(plugin, ROOT + "open-doors", true),
                bool(plugin, ROOT + "open-trapdoors", true),
                bool(plugin, ROOT + "open-gates", true),
                bool(plugin, ROOT + "avoid-hazards", true),
                integer(plugin, ROOT + "max-nodes", 1800, 128, 10000),
                integer(plugin, ROOT + "replan-interval-ticks", 8, 1, 80),
                integer(plugin, ROOT + "target-predict-ticks", 6, 0, 20),
                integer(plugin, ROOT + "max-drop-blocks", 3, 1, 8),
                decimal(plugin, ROOT + "max-search-distance", 48.0, 8.0, 160.0),
                decimal(plugin, ROOT + "goal-radius", 3.3, 1.0, 8.0),
                decimal(plugin, ROOT + "line-of-sight-goal-radius", 5.5, 1.0, 12.0)
        );
    }

    static NavigationConfig defaults() {
        return new NavigationConfig(true, true, true, true, true, true, true, true,
                1800, 8, 6, 3, 48.0, 3.3, 5.5);
    }

    private static boolean bool(Plugin plugin, String path, boolean fallback) {
        return plugin.getConfig().getBoolean(path, fallback);
    }

    private static int integer(Plugin plugin, String path, int fallback, int min, int max) {
        int value = plugin.getConfig().getInt(path, fallback);
        return Math.max(min, Math.min(max, value));
    }

    private static double decimal(Plugin plugin, String path, double fallback, double min, double max) {
        double value = plugin.getConfig().getDouble(path, fallback);
        if (!Double.isFinite(value)) return fallback;
        return Math.max(min, Math.min(max, value));
    }
}
