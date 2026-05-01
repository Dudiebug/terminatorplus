package net.nuggetmc.tplus.bot;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.nuggetmc.tplus.TerminatorPlus;
import net.nuggetmc.tplus.api.BotManager;
import net.nuggetmc.tplus.api.Terminator;
import net.nuggetmc.tplus.api.agent.Agent;
import net.nuggetmc.tplus.api.agent.legacyagent.LegacyAgent;
import net.nuggetmc.tplus.api.agent.legacyagent.ai.NeuralNetwork;
import net.nuggetmc.tplus.api.event.BotDeathEvent;
import net.nuggetmc.tplus.api.utils.MojangAPI;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class BotManagerImpl implements BotManager, Listener {

    private final Agent agent;
    private final Set<Terminator> bots;
    private final NumberFormat numberFormat;

    public boolean joinMessages = false;
    private boolean mobTarget = false;
    private boolean addPlayerList = false;

    public BotManagerImpl() {
        this.agent = new LegacyAgent(this, TerminatorPlus.getInstance());
        this.bots = ConcurrentHashMap.newKeySet(); //should fix concurrentmodificationexception
        this.numberFormat = NumberFormat.getInstance(Locale.US);
    }

    @Override
    public Set<Terminator> fetch() {
        return bots;
    }

    @Override
    public void add(Terminator bot) {
        if (joinMessages) {
            // Bukkit.broadcastMessage(ChatColor.YELLOW + (bot.getBotName() + " joined the game"));
            Bukkit.broadcast(MiniMessage.miniMessage().deserialize("<yellow>" + bot.getBotName() + " joined the game"));
        }

        bots.add(bot);
    }

    public List<Bot> getAllByName(String name) {
        List<Bot> result = new ArrayList<>();
        for (Terminator t : bots) {
            if (t instanceof Bot b && name.equals(b.getBotName())) result.add(b);
        }
        return result;
    }

    @Override
    public Terminator getFirst(String name, Location target) {
        if (target != null) {
            Terminator closest = null;
            for (Terminator bot : bots) {
                if (name.equals(bot.getBotName()) && (closest == null
                        || target.distanceSquared(bot.getLocation()) < target.distanceSquared(closest.getLocation()))) {
                    closest = bot;
                }
            }
            return closest;
        }
        for (Terminator bot : bots) {
            if (name.equals(bot.getBotName())) {
                return bot;
            }
        }

        return null;
    }

    @Override
    public List<String> fetchNames() {
        //return bots.stream().map(Bot::getBotName).map(component -> component.getString()).collect(Collectors.toList());
        return bots.stream().map(terminator -> {
            if (terminator instanceof Bot bot) return bot.getName().getString();
            else return terminator.getBotName();
        }).collect(Collectors.toList());
    }

    @Override
    public Terminator createBot(Location loc, String name, String skin, String sig) {
        return Bot.createBot(loc, name, new String[]{skin, sig});
    }

    @Override
    public Agent getAgent() {
        return agent;
    }

    @Override
    public void createBots(CommandSender sender, String name, String skinName, int n, Location loc) {
        createBots(sender, name, skinName, n, null, loc);
    }

    /**
     * Command-facing async skin lookup path. Spawning still happens on the
     * primary thread; only Mojang API fetch is off-thread.
     */
    public void createBotsAsync(CommandSender sender, String name, String skinName, int n, Location location) {
        long timestamp = System.currentTimeMillis();

        if (n < 1) n = 1;
        int amount = n;

        if (sender != null) {
            String message = "Creating " + (amount == 1 ? "new bot" : "<red>" + numberFormat.format(amount) + "<reset>" + " new bots")
                    + " with name " + "<green>" + name.replace("%", "<light_purple>%" + "<reset>")
                    + (skinName == null ? "" : "<reset>" + " and skin " + "<green>" + skinName)
                    + "<reset>...";
            sender.sendRichMessage(message);
        }

        String requestedSkin = skinName == null ? name : skinName;
        Location spawnLoc = location;
        if (spawnLoc == null) {
            if (sender instanceof Player player) {
                spawnLoc = player.getLocation();
            } else {
                spawnLoc = new Location(Bukkit.getWorlds().get(0), 0, 0, 0);
                if (sender != null) {
                    sender.sendRichMessage("<red>No location specified, defaulting to " + spawnLoc.getX() + ", " + spawnLoc.getY() + ", " + spawnLoc.getZ() + ".");
                }
            }
        }
        final Location finalSpawnLoc = spawnLoc;

        MojangAPI.getSkinAsync(requestedSkin).whenComplete((skin, error) ->
                Bukkit.getScheduler().runTask(TerminatorPlus.getInstance(), () -> {
                    if (error != null && sender != null) {
                        sender.sendRichMessage("<red>Skin lookup failed for <yellow>" + requestedSkin + "<red>. Spawning bot(s) with fallback skin.");
                    }
                    createBots(finalSpawnLoc, name, skin, amount, null);
                    if (sender != null) {
                        sender.sendRichMessage("Process completed (<red>" + ((System.currentTimeMillis() - timestamp) / 1000D) + "s<reset>).");
                    }
                }));
    }

    @Override
    public void createBots(CommandSender sender, String name, String skinName, int n, NeuralNetwork network, Location location) {
        long timestamp = System.currentTimeMillis();

        if (n < 1) n = 1;

        if (sender != null) {
            String message = "Creating " + (n == 1 ? "new bot" : "<red>" + numberFormat.format(n) + "<reset>" + " new bots")
                    + " with name " + "<green>" + name.replace("%", "<light_purple>%" + "<reset>")
                    + (skinName == null ? "" : "<reset>" + " and skin " + "<green>" + skinName)
                    + "<reset>...";
            sender.sendRichMessage(message);
        }

        skinName = skinName == null ? name : skinName;

        if (location != null) {
            createBots(location, name, MojangAPI.getSkin(skinName), n, network);
        } else {
            if (sender instanceof Player player)
                createBots(player.getLocation(), name, MojangAPI.getSkin(skinName), n, network);
            else {
                Location l = new Location(Bukkit.getWorlds().get(0), 0, 0, 0);
                if (sender != null)
                    // sender.sendMessage(ChatColor.RED + "No location specified, defaulting to " + l + ".");
                    sender.sendRichMessage("<red>No location specified, defaulting to " + l.getX() + ", " + l.getY() + ", " + l.getZ() + ".");
                createBots(l, name, MojangAPI.getSkin(skinName), n, network);
            }
        }

        if (sender != null)
            // sender.sendMessage("Process completed (" + ChatColor.RED + ((System.currentTimeMillis() - timestamp) / 1000D) + "s" + ChatColor.RESET + ").");
            sender.sendRichMessage("Process completed (<red>" + ((System.currentTimeMillis() - timestamp) / 1000D) + "s<reset>).");
    }

    @Override
    public Set<Terminator> createBots(Location loc, String name, String[] skin, int n, NeuralNetwork network) {
        List<NeuralNetwork> networks = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            networks.add(network);
        }

        return createBots(loc, name, skin, networks);
    }

    @Override
    public Set<Terminator> createBots(Location loc, String name, String[] skin, List<NeuralNetwork> networks) {
        Set<Terminator> bots = new HashSet<>();
        World world = loc.getWorld();

        int n = networks.size();
        int i = 1;

        double f = n < 100 ? .004 * n : .4;

        // If the user supplied a `%` placeholder (e.g. "bot%"), substitute the
        // index into that. If not and we're spawning more than one bot, append
        // the index so they're distinguishable — was spawning 200 bots all
        // named "bot" otherwise, which made every debug log and command
        // ambiguous.
        boolean hasPlaceholder = name.contains("%");
        for (NeuralNetwork network : networks) {
            String botName = hasPlaceholder
                    ? name.replace("%", String.valueOf(i))
                    : (n > 1 ? name + i : name);
            Bot bot = Bot.createBot(loc, botName, skin);

            if (network != null) {
                bot.setNeuralNetwork(network == NeuralNetwork.RANDOM ? NeuralNetwork.generateRandomNetwork() : network);
                bot.setShield(true);
                bot.setDefaultItem(new ItemStack(Material.WOODEN_AXE));
                //bot.setRemoveOnDeath(false);
            }

            if (network != null) {
                bot.setVelocity(randomVelocity());
            } else if (i > 1) {
                bot.setVelocity(randomVelocity().multiply(f));
            }

            bots.add(bot);
            i++;
        }

        if (world != null) {
            world.spawnParticle(Particle.CLOUD, loc, 100, 1, 1, 1, 0.5);
        }

        return bots;
    }

    private Vector randomVelocity() {
        return new Vector(Math.random() - 0.5, 0.5, Math.random() - 0.5).normalize();
    }

    @Override
    public void remove(Terminator bot) {
        if (bot == null) return;
        try {
            agent.cleanupBot(bot);
        } finally {
            bots.remove(bot);
        }
    }

    @Override
    public void reset() {
        if (!bots.isEmpty()) {
            new HashSet<>(bots).forEach(Terminator::removeBot);
            bots.clear(); // Not always necessary, but a good security measure
        }

        agent.stopAllTasks();
    }


    @Override
    public Terminator getBot(Player player) { // potentially memory intensive
        int id = player.getEntityId();
        return getBot(id);
    }

    @Override
    public Terminator getBot(UUID uuid) {
        Entity entity = Bukkit.getEntity(uuid);
        if (entity == null) return null;
        return getBot(entity.getEntityId());
    }

    @Override
    public Terminator getBot(int entityId) {
        for (Terminator bot : bots) {
            if (bot.getEntityId() == entityId) {
                return bot;
            }
        }
        return null;
    }

    @Override
    public boolean isMobTarget() {
        return mobTarget;
    }

    @Override
    public void setMobTarget(boolean mobTarget) {
        this.mobTarget = mobTarget;
    }

    @Override
    public boolean addToPlayerList() {
        return addPlayerList;
    }

    @Override
    public void setAddToPlayerList(boolean addPlayerList) {
        this.addPlayerList = addPlayerList;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        ServerGamePacketListenerImpl connection = ((CraftPlayer) event.getPlayer()).getHandle().connection;
        bots.forEach(bot -> bot.renderBot(connection, true));
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        LivingEntity bukkitEntity = event.getEntity();
        Terminator bot = getBot(bukkitEntity.getEntityId());
        if (bot != null) {
            agent.onBotDeath(new BotDeathEvent(event, bot));
        }
    }

    @EventHandler
    public void onMobTarget(EntityTargetLivingEntityEvent event) {
        if (mobTarget || event.getTarget() == null)
            return;
        Bot bot = (Bot) getBot(event.getTarget().getUniqueId());
        if (bot != null) {
            event.setCancelled(true);
        }
    }
}
