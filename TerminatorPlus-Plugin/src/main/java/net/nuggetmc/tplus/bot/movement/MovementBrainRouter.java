package net.nuggetmc.tplus.bot.movement;

import net.nuggetmc.tplus.TerminatorPlus;
import net.nuggetmc.tplus.api.agent.legacyagent.ai.movement.MovementBrainBank;
import net.nuggetmc.tplus.api.agent.legacyagent.ai.movement.MovementNetwork;
import net.nuggetmc.tplus.bot.Bot;
import net.nuggetmc.tplus.bot.combat.CombatDebugger;
import net.nuggetmc.tplus.bot.combat.CombatIntent;
import net.nuggetmc.tplus.bot.combat.DesiredRangeBand;
import net.nuggetmc.tplus.bot.combat.MovementBranchFamily;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Selects a movement-only brain from the bank. CombatIntent owns the combat
 * meaning; this router only chooses locomotion policy.
 */
public final class MovementBrainRouter {
    private final Map<UUID, String> lastRoute = new ConcurrentHashMap<>();

    public RouteResult route(Bot bot, MovementBrainBank bank) {
        if (bot == null || bank == null) {
            return RouteResult.missing("missing-bot-or-bank");
        }

        CombatIntent intent = bot.getCombatIntent();
        MovementBranchFamily requested = requestedFamily(bot, intent == null ? CombatIntent.DEFAULT : intent);
        MovementBrainBank.Selection selection = bank.select(requested.id());
        MovementNetwork network = selection.network();
        if (network == null) {
            return RouteResult.missing(selection.reason());
        }

        MovementBranchFamily active = MovementBranchFamily.fromId(selection.brain().familyId());
        boolean fallback = selection.fallback() || active == MovementBranchFamily.GENERAL_FALLBACK
                && requested != MovementBranchFamily.GENERAL_FALLBACK;
        logRouteChange(bot, requested, active, selection.brainName(), fallback, selection.reason());
        return new RouteResult(requested, active, selection.brainName(), fallback, selection.reason(), network);
    }

    private MovementBranchFamily requestedFamily(Bot bot, CombatIntent intent) {
        int alive = bot.getAliveTicks();
        if (intent.movementLocked(alive) && intent.lockFamily() != MovementBranchFamily.GENERAL_FALLBACK) {
            return intent.lockFamily();
        }
        if (intent.branchFamily() != MovementBranchFamily.GENERAL_FALLBACK) {
            return intent.branchFamily();
        }

        MovementBranchFamily planned = plannedActionFamily(intent.plannedAction());
        if (planned != MovementBranchFamily.GENERAL_FALLBACK) {
            return planned;
        }

        DesiredRangeBand band = intent.desiredRangeBand();
        if (band == DesiredRangeBand.LONG) return MovementBranchFamily.MOBILITY;
        if (band == DesiredRangeBand.MID || band == DesiredRangeBand.FAR) return MovementBranchFamily.PROJECTILE_RANGED;
        return MovementBranchFamily.GENERAL_FALLBACK;
    }

    private static MovementBranchFamily plannedActionFamily(String plannedAction) {
        if (plannedAction == null || plannedAction.isBlank()) return MovementBranchFamily.GENERAL_FALLBACK;
        String action = plannedAction.toLowerCase(java.util.Locale.ROOT);
        if (action.contains("mace")) return MovementBranchFamily.MACE;
        if (action.contains("trident")) return MovementBranchFamily.TRIDENT_RANGED;
        if (action.contains("spear")) return MovementBranchFamily.SPEAR_MELEE;
        if (action.contains("pearl") || action.contains("elytra") || action.contains("wind")
                || action.contains("mobility")) {
            return MovementBranchFamily.MOBILITY;
        }
        if (action.contains("crystal") || action.contains("anchor") || action.contains("tnt")
                || action.contains("explosive")) {
            return MovementBranchFamily.EXPLOSIVE_SURVIVAL;
        }
        if (action.contains("projectile") || action.contains("arrow") || action.contains("bow")
                || action.contains("splash") || action.contains("firework")) {
            return MovementBranchFamily.PROJECTILE_RANGED;
        }
        if (action.contains("melee") || action.contains("cobweb")) return MovementBranchFamily.MELEE;
        return MovementBranchFamily.GENERAL_FALLBACK;
    }

    private void logRouteChange(
            Bot bot,
            MovementBranchFamily requested,
            MovementBranchFamily active,
            String brainName,
            boolean fallback,
            String reason
    ) {
        String route = requested.id() + "->" + active.id() + "/" + brainName + "/" + fallback;
        String previous = lastRoute.put(bot.getUUID(), route);
        if (route.equals(previous)) return;

        String detail = "requested=" + requested.id()
                + " active=" + active.id()
                + " brain=" + brainName
                + " fallback=" + fallback
                + " reason=" + reason;
        CombatDebugger.log(bot, "route-change", detail);
        TerminatorPlus plugin = TerminatorPlus.getInstance();
        if (plugin != null && plugin.getConfig().getBoolean("ai.movement.bank.debug-logging", false)) {
            plugin.getLogger().info("[movement-route] bot=" + bot.getBotName() + " " + detail);
        }
    }

    public record RouteResult(
            MovementBranchFamily requestedFamily,
            MovementBranchFamily activeFamily,
            String brainName,
            boolean fallback,
            String reason,
            MovementNetwork network
    ) {
        static RouteResult missing(String reason) {
            return new RouteResult(MovementBranchFamily.GENERAL_FALLBACK, MovementBranchFamily.GENERAL_FALLBACK,
                    MovementBrainBank.FALLBACK_BRAIN_NAME, true, reason, null);
        }
    }
}
