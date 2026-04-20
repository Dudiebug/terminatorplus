package net.nuggetmc.tplus.bot.gui;

import net.nuggetmc.tplus.TerminatorPlus;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Blocks edits to the decorative filler slots of {@link BotInventoryGUI}
 * and writes changes back to the bot when the viewer closes the GUI.
 */
public final class BotInventoryListener implements Listener {

    private final TerminatorPlus plugin;

    public BotInventoryListener(TerminatorPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        InventoryHolder holder = top.getHolder();
        if (!(holder instanceof BotInventoryGUI gui)) return;

        // Block interaction with decorative slots regardless of which inventory was clicked.
        if (event.getClickedInventory() == top && BotInventoryGUI.isFillerSlot(event.getRawSlot())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        InventoryHolder holder = top.getHolder();
        if (!(holder instanceof BotInventoryGUI gui)) return;

        for (int slot : event.getRawSlots()) {
            if (slot < top.getSize() && BotInventoryGUI.isFillerSlot(slot)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Inventory top = event.getInventory();
        InventoryHolder holder = top.getHolder();
        if (!(holder instanceof BotInventoryGUI gui)) return;

        // Bot might have been removed while the GUI was open.
        if (!gui.getBot().isBotAlive()) return;
        gui.syncToBot();
    }
}
