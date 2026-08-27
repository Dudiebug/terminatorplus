package net.nuggetmc.tplus.bot.gui;

import net.nuggetmc.tplus.api.Terminator;
import net.nuggetmc.tplus.api.agent.BotRuntimeSnapshot;
import net.nuggetmc.tplus.bot.Bot;
import net.nuggetmc.tplus.bot.BotManagerImpl;
import net.nuggetmc.tplus.bot.combat.CombatIntent;
import net.nuggetmc.tplus.bot.combat.MovementState;
import net.nuggetmc.tplus.bot.navigation.MovementV2Controller;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class BotInspectionDetailGUI implements InventoryHolder {

    static final int BACK_SLOT = 45;
    static final int REFRESH_SLOT = 49;

    private final BotManagerImpl manager;
    private final UUID botId;
    private final int parentPage;
    private final Inventory inventory;

    public BotInspectionDetailGUI(BotManagerImpl manager, UUID botId, int parentPage) {
        this.manager = manager;
        this.botId = botId;
        this.parentPage = Math.max(0, parentPage);
        Terminator bot = manager.getBot(botId);
        String name = bot == null ? "removed" : bot.getBotName();
        this.inventory = Bukkit.createInventory(this, 54, ChatColor.GOLD + "Inspect: " + ChatColor.YELLOW + name);
        render(bot);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open(Player viewer) {
        viewer.openInventory(inventory);
    }

    UUID botId() {
        return botId;
    }

    int parentPage() {
        return parentPage;
    }

    private void render(Terminator terminator) {
        inventory.setItem(BACK_SLOT, BotInspectionListGUI.item(Material.ARROW, ChatColor.YELLOW + "Back", List.of()));
        inventory.setItem(REFRESH_SLOT, BotInspectionListGUI.item(Material.CLOCK, ChatColor.AQUA + "Refresh", List.of()));
        if (!(terminator instanceof Bot bot) || !bot.isBotAlive()) {
            inventory.setItem(22, BotInspectionListGUI.item(Material.BARRIER, ChatColor.RED + "Bot no longer loaded",
                    List.of(ChatColor.GRAY + botId.toString(), ChatColor.YELLOW + "Use Back to return to the list.")));
            return;
        }

        BotRuntimeSnapshot runtime = manager.getAgent().getRuntimeSnapshot(botId).orElse(null);
        Location location = bot.getLocation();
        inventory.setItem(10, BotInspectionListGUI.item(Material.RED_DYE, ChatColor.RED + "Health",
                List.of(ChatColor.GRAY + format(bot.getBotHealth()) + " / " + format(bot.getBotMaxHealth()),
                        ChatColor.GRAY + "Alive ticks: " + bot.getAliveTicks(),
                        ChatColor.GRAY + "Kills: " + bot.getKills())));

        String targetName = runtime == null || runtime.targetId() == null ? "none" : runtime.targetName();
        String targetDistance = runtime == null || runtime.targetDistance() < 0
                ? "n/a" : format(runtime.targetDistance());
        inventory.setItem(12, BotInspectionListGUI.item(Material.COMPASS, ChatColor.YELLOW + "Target",
                List.of(ChatColor.GRAY + "Name: " + targetName,
                        ChatColor.GRAY + "Distance: " + targetDistance)));

        CombatIntent intent = bot.getCombatIntent();
        inventory.setItem(14, BotInspectionListGUI.item(Material.IRON_SWORD, ChatColor.GOLD + "Combat",
                List.of(ChatColor.GRAY + "Phase: " + bot.getCombatState().getPhase(),
                        ChatColor.GRAY + "Action: " + intent.plannedAction(),
                        ChatColor.GRAY + "Objective: " + intent.movementObjective(),
                        ChatColor.GRAY + "Range error: " + format(intent.rangeErrorSigned()),
                        ChatColor.GRAY + "Lock: " + intent.lockFamily() + " (" + intent.lockTicksRemaining(bot.getAliveTicks()) + "t)")));

        MovementState movement = bot.getMovementState();
        String movementMode = runtime == null ? "unknown" : runtime.movementMode().name().toLowerCase(Locale.ROOT);
        inventory.setItem(16, BotInspectionListGUI.item(Material.FEATHER, ChatColor.AQUA + "Movement",
                List.of(ChatColor.GRAY + "Mode: " + movementMode,
                        ChatColor.GRAY + "Sprinting: " + movement.isSprinting(),
                        ChatColor.GRAY + "Falling: " + movement.isFalling(),
                        ChatColor.GRAY + "Retreating: " + movement.isRetreating(),
                        ChatColor.GRAY + "Circling: " + movement.isCircling())));

        Material held = bot.getBukkitEntity().getInventory().getItemInMainHand().getType();
        inventory.setItem(28, BotInspectionListGUI.item(Material.CHEST, ChatColor.GOLD + "Inventory / loadout",
                List.of(ChatColor.GRAY + "Held: " + held,
                        ChatColor.GRAY + "Selected slot: " + bot.getBotInventory().getSelectedHotbarSlot(),
                        ChatColor.GRAY + "Loadout locked: " + bot.getBotInventory().isRespectingLoadout(),
                        ChatColor.GRAY + "Training loadout: " + display(bot.getTrainingLoadout()))));

        inventory.setItem(30, BotInspectionListGUI.item(Material.REPEATER, ChatColor.LIGHT_PURPLE + "Runtime",
                List.of(ChatColor.GRAY + "Ticks: " + (runtime == null ? "n/a" : runtime.tickCount()),
                        ChatColor.GRAY + "Target changes: " + (runtime == null ? "n/a" : runtime.targetChanges()),
                        ChatColor.GRAY + "Stuck ticks: " + (runtime == null ? "n/a" : runtime.stuckTicks()),
                        ChatColor.GRAY + "Respawn allowed: " + bot.isAutoRespawnAllowed())));

        MovementV2Controller.Status navigation = bot.movementV2Status();
        inventory.setItem(32, BotInspectionListGUI.item(Material.RECOVERY_COMPASS, ChatColor.BLUE + "Navigation V2",
                List.of(ChatColor.GRAY + "Last: " + navigation.lastReason(),
                        ChatColor.GRAY + "Route: " + navigation.routeIndex() + "/" + navigation.routeLength(),
                        ChatColor.GRAY + "Plans/replans: " + navigation.plans() + "/" + navigation.replans(),
                        ChatColor.GRAY + "Fallbacks: " + navigation.fallbacks(),
                        ChatColor.GRAY + "Action failures: " + navigation.actionFailures())));

        inventory.setItem(34, BotInspectionListGUI.item(Material.NAME_TAG, ChatColor.GREEN + bot.getBotName(),
                List.of(ChatColor.GRAY + "UUID: " + botId,
                        ChatColor.GRAY + "World: " + location.getWorld().getName(),
                        ChatColor.GRAY + "Position: " + format(location.getX()) + ", "
                                + format(location.getY()) + ", " + format(location.getZ()),
                        ChatColor.GRAY + "Player list: " + bot.isInPlayerList())));
    }

    private static String display(String value) {
        return value == null || value.isBlank() ? "none" : value;
    }

    private static String format(double value) {
        return String.format(Locale.ENGLISH, "%.2f", value);
    }
}
