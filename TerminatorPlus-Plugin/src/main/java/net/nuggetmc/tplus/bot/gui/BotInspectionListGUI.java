package net.nuggetmc.tplus.bot.gui;

import net.nuggetmc.tplus.api.Terminator;
import net.nuggetmc.tplus.api.agent.Agent;
import net.nuggetmc.tplus.api.agent.BotRuntimeSnapshot;
import net.nuggetmc.tplus.bot.BotManagerImpl;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class BotInspectionListGUI implements InventoryHolder {

    static final int PAGE_SIZE = 45;
    static final int PREVIOUS_SLOT = 45;
    static final int REFRESH_SLOT = 49;
    static final int NEXT_SLOT = 53;

    private final BotManagerImpl manager;
    private final Agent agent;
    private final int page;
    private final List<UUID> botIds;
    private final Inventory inventory;

    public BotInspectionListGUI(BotManagerImpl manager, int requestedPage) {
        this.manager = manager;
        this.agent = manager.getAgent();
        List<Terminator> bots = manager.fetch().stream()
                .filter(Terminator::isBotAlive)
                .sorted(Comparator.comparing(Terminator::getBotName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(bot -> bot.getBukkitEntity().getUniqueId()))
                .toList();
        this.page = clampPage(requestedPage, bots.size());
        this.botIds = bots.stream().map(bot -> bot.getBukkitEntity().getUniqueId()).toList();
        this.inventory = Bukkit.createInventory(this, 54, ChatColor.GOLD + "Loaded bots "
                + ChatColor.GRAY + "(" + (page + 1) + "/" + pageCount(bots.size()) + ")");
        render(bots);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open(Player viewer) {
        viewer.openInventory(inventory);
    }

    int page() {
        return page;
    }

    UUID botIdAt(int rawSlot) {
        int index = startIndex(page) + rawSlot;
        return rawSlot >= 0 && rawSlot < PAGE_SIZE && index < botIds.size() ? botIds.get(index) : null;
    }

    static int pageCount(int totalBots) {
        return Math.max(1, (Math.max(0, totalBots) + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    static int clampPage(int requestedPage, int totalBots) {
        return Math.max(0, Math.min(requestedPage, pageCount(totalBots) - 1));
    }

    static int startIndex(int page) {
        return Math.max(0, page) * PAGE_SIZE;
    }

    private void render(List<Terminator> bots) {
        int start = startIndex(page);
        int end = Math.min(start + PAGE_SIZE, bots.size());
        for (int index = start; index < end; index++) {
            Terminator bot = bots.get(index);
            UUID id = bot.getBukkitEntity().getUniqueId();
            BotRuntimeSnapshot runtime = agent.getRuntimeSnapshot(id).orElse(null);
            String target = runtime == null || runtime.targetId() == null ? "none" : runtime.targetName();
            String movement = runtime == null ? "unknown" : runtime.movementMode().name().toLowerCase(Locale.ROOT);
            inventory.setItem(index - start, item(Material.ARMOR_STAND,
                    ChatColor.GREEN + bot.getBotName(),
                    List.of(
                            ChatColor.GRAY + "HP: " + ChatColor.RED + format(bot.getBotHealth())
                                    + ChatColor.GRAY + "/" + format(bot.getBotMaxHealth()),
                            ChatColor.GRAY + "Target: " + ChatColor.YELLOW + target,
                            ChatColor.GRAY + "Movement: " + ChatColor.AQUA + movement,
                            ChatColor.DARK_GRAY + id.toString(),
                            ChatColor.YELLOW + "Click to inspect"
                    )));
        }

        if (bots.isEmpty()) {
            inventory.setItem(22, item(Material.BARRIER, ChatColor.RED + "No loaded bots",
                    List.of(ChatColor.GRAY + "Create a bot, then refresh.")));
        }
        if (page > 0) {
            inventory.setItem(PREVIOUS_SLOT, item(Material.ARROW, ChatColor.YELLOW + "Previous page", List.of()));
        }
        inventory.setItem(REFRESH_SLOT, item(Material.CLOCK, ChatColor.AQUA + "Refresh", List.of()));
        if (page + 1 < pageCount(bots.size())) {
            inventory.setItem(NEXT_SLOT, item(Material.ARROW, ChatColor.YELLOW + "Next page", List.of()));
        }
    }

    static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(new ArrayList<>(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String format(double value) {
        return String.format(Locale.ENGLISH, "%.1f", value);
    }
}
