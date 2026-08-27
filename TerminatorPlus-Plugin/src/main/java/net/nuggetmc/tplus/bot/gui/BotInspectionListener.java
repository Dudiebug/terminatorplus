package net.nuggetmc.tplus.bot.gui;

import net.nuggetmc.tplus.TerminatorPlus;
import net.nuggetmc.tplus.api.Terminator;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public final class BotInspectionListener implements Listener {

    private final TerminatorPlus plugin;

    public BotInspectionListener(TerminatorPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof BotInspectionListGUI)
                && !(top.getHolder() instanceof BotInspectionDetailGUI)) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getRawSlot() < 0 || event.getRawSlot() >= top.getSize()) return;

        if (top.getHolder() instanceof BotInspectionListGUI list) {
            if (event.getRawSlot() == BotInspectionListGUI.PREVIOUS_SLOT) {
                new BotInspectionListGUI(plugin.getManager(), list.page() - 1).open(player);
            } else if (event.getRawSlot() == BotInspectionListGUI.NEXT_SLOT) {
                new BotInspectionListGUI(plugin.getManager(), list.page() + 1).open(player);
            } else if (event.getRawSlot() == BotInspectionListGUI.REFRESH_SLOT) {
                new BotInspectionListGUI(plugin.getManager(), list.page()).open(player);
            } else {
                UUID botId = list.botIdAt(event.getRawSlot());
                if (botId != null) {
                    Terminator bot = plugin.getManager().getBot(botId);
                    if (bot != null && bot.isBotAlive()) {
                        new BotInspectionDetailGUI(plugin.getManager(), botId, list.page()).open(player);
                    } else {
                        new BotInspectionListGUI(plugin.getManager(), list.page()).open(player);
                    }
                }
            }
            return;
        }

        BotInspectionDetailGUI detail = (BotInspectionDetailGUI) top.getHolder();
        if (event.getRawSlot() == BotInspectionDetailGUI.BACK_SLOT) {
            new BotInspectionListGUI(plugin.getManager(), detail.parentPage()).open(player);
        } else if (event.getRawSlot() == BotInspectionDetailGUI.REFRESH_SLOT) {
            new BotInspectionDetailGUI(plugin.getManager(), detail.botId(), detail.parentPage()).open(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top.getHolder() instanceof BotInspectionListGUI
                || top.getHolder() instanceof BotInspectionDetailGUI) {
            event.setCancelled(true);
        }
    }
}
