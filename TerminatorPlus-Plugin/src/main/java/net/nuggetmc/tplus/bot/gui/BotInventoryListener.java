package net.nuggetmc.tplus.bot.gui;

import net.nuggetmc.tplus.TerminatorPlus;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        InventoryHolder holder = top.getHolder();
        if (!(holder instanceof BotInventoryGUI gui)) return;

        // Filler slots: lock down hard.
        if (event.getClickedInventory() == top && BotInventoryGUI.isFillerSlot(event.getRawSlot())) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
            return;
        }

        // For every other slot in our GUI, force-allow the click in case another plugin
        // (or Paper's protection layer) tried to cancel it.
        event.setCancelled(false);
        event.setResult(Event.Result.ALLOW);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        InventoryHolder holder = top.getHolder();
        if (!(holder instanceof BotInventoryGUI gui)) return;

        for (int slot : event.getRawSlots()) {
            if (slot < top.getSize() && BotInventoryGUI.isFillerSlot(slot)) {
                event.setCancelled(true);
                event.setResult(Event.Result.DENY);
                return;
            }
        }

        event.setCancelled(false);
        event.setResult(Event.Result.ALLOW);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Inventory top = event.getView().getTopInventory();
        InventoryHolder holder = top.getHolder();
        if (!(holder instanceof BotInventoryGUI gui)) return;

        if (!gui.getBot().isBotAlive()) return;
        gui.syncToBot();

        // Propagate the same inventory to every other bot with the same name.
        String name = gui.getBot().getBotName();
        net.nuggetmc.tplus.bot.Bot source = gui.getBot();
        plugin.getManager().getAllByName(name).stream()
                .filter(b -> b != source && b.isBotAlive())
                .forEach(b -> source.getBotInventory().copyInventoryTo(b));
    }
}
