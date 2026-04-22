package net.nuggetmc.tplus.command.commands;

import net.nuggetmc.tplus.TerminatorPlus;
import net.nuggetmc.tplus.api.Terminator;
import net.nuggetmc.tplus.api.agent.legacyagent.EnumTargetGoal;
import net.nuggetmc.tplus.api.agent.legacyagent.LegacyAgent;
import net.nuggetmc.tplus.api.utils.ChatUtils;
import net.nuggetmc.tplus.bot.Bot;
import net.nuggetmc.tplus.bot.BotManagerImpl;
import net.nuggetmc.tplus.bot.gui.BotInventoryGUI;
import net.nuggetmc.tplus.bot.loadout.BotInventory;
import net.nuggetmc.tplus.bot.preset.BotPreset;
import net.nuggetmc.tplus.bot.preset.PresetManager;
import net.nuggetmc.tplus.command.CommandHandler;
import net.nuggetmc.tplus.command.CommandInstance;
import net.nuggetmc.tplus.command.annotation.*;
import net.nuggetmc.tplus.utils.Debugger;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class BotCommand extends CommandInstance {

    private final TerminatorPlus plugin;
    private final CommandHandler handler;
    private final BotManagerImpl manager;
    private final LegacyAgent agent;
    private final BukkitScheduler scheduler;
    private final DecimalFormat formatter;
    private final Map<String, ItemStack[]> armorTiers;
    private AICommand aiManager;

    public BotCommand(CommandHandler handler, String name, String description, String... aliases) {
        super(handler, name, description, aliases);

        this.handler = commandHandler;
        this.plugin = TerminatorPlus.getInstance();
        this.manager = plugin.getManager();
        this.agent = (LegacyAgent) manager.getAgent();
        this.scheduler = Bukkit.getScheduler();
        this.formatter = new DecimalFormat("0.##");
        this.armorTiers = new HashMap<>();

        this.armorTierSetup();
    }

    @Command
    public void root(CommandSender sender) {
        commandHandler.sendRootInfo(this, sender);
    }

    @Command(
            name = "create",
            desc = "Create a bot."
    )
    public void create(CommandSender sender, @Arg("name") String name, @OptArg("skin") String skin, @TextArg @OptArg("loc") String loc) {
        Location location = (sender instanceof Player) ? ((Player) sender).getLocation() : new Location(Bukkit.getWorlds().get(0), 0, 0, 0);
        if (loc != null && !loc.isEmpty()) {
            Player player = Bukkit.getPlayer(loc);
            if (player != null) {
                location = player.getLocation();
            } else {
                String[] split = loc.split(" ");
                if (split.length >= 3) {
                    try {
                        double x = Double.parseDouble(split[0]);
                        double y = Double.parseDouble(split[1]);
                        double z = Double.parseDouble(split[2]);
                        World world = Bukkit.getWorld(split.length >= 4 ? split[3] : location.getWorld().getName());
                        location = new Location(world, x, y, z);
                    } catch (NumberFormatException e) {
                        sender.sendMessage("The location '" + ChatColor.YELLOW + loc + ChatColor.RESET + "' is not valid!");
                        return;
                    }
                } else {
                    sender.sendMessage("The location '" + ChatColor.YELLOW + loc + ChatColor.RESET + "' is not valid!");
                    return;
                }
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Spawning bot at 0, 0, 0 in world " + location.getWorld().getName() + " because no location was specified.");
            }
        }
        manager.createBots(sender, name, skin, 1, location);
    }

    @Command(
            name = "multi",
            desc = "Create multiple bots at once."
    )
    public void multi(CommandSender sender, @Arg("amount") int amount, @Arg("name") String name, @OptArg("skin") String skin, @TextArg @OptArg("loc") String loc) {
        Location location = (sender instanceof Player) ? ((Player) sender).getLocation() : new Location(Bukkit.getWorlds().get(0), 0, 0, 0);
        if (loc != null && !loc.isEmpty()) {
            Player player = Bukkit.getPlayer(loc);
            if (player != null) {
                location = player.getLocation();
            } else {
                String[] split = loc.split(" ");
                if (split.length >= 3) {
                    try {
                        double x = Double.parseDouble(split[0]);
                        double y = Double.parseDouble(split[1]);
                        double z = Double.parseDouble(split[2]);
                        World world = Bukkit.getWorld(split.length >= 4 ? split[3] : location.getWorld().getName());
                        location = new Location(world, x, y, z);
                    } catch (NumberFormatException e) {
                        sender.sendMessage("The location '" + ChatColor.YELLOW + loc + ChatColor.RESET + "' is not valid!");
                        return;
                    }
                } else {
                    sender.sendMessage("The location '" + ChatColor.YELLOW + loc + ChatColor.RESET + "' is not valid!");
                    return;
                }
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Spawning bot at 0, 0, 0 in world " + location.getWorld().getName() + " because no location was specified.");
            }
        }
        manager.createBots(sender, name, skin, amount, location);
    }

    @Command(
            name = "give",
            desc = "Give an item to bot(s). Usage: /bot give <item> [bot-name] [slot]",
            autofill = "giveAutofill"
    )
    public void give(CommandSender sender, List<String> args) {
        if (args.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Usage: /bot give <item> [bot-name] [slot]");
            return;
        }
        String itemName = args.get(0);
        Material type = Material.matchMaterial(itemName);
        if (type == null) {
            sender.sendMessage("The item " + ChatColor.YELLOW + itemName + ChatColor.RESET + " is not valid!");
            return;
        }
        ItemStack item = new ItemStack(type);

        // Single-arg form: legacy behavior — set default item for all bots.
        if (args.size() == 1) {
            manager.fetch().forEach(bot -> bot.setDefaultItem(item));
            sender.sendMessage("Successfully set the default item to " + ChatColor.YELLOW + item.getType() + ChatColor.RESET + " for all current bots.");
            return;
        }

        String botName = args.get(1);
        Integer slot = null;
        if (args.size() >= 3) {
            try {
                slot = Integer.parseInt(args.get(2));
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Slot must be a number 0-40.");
                return;
            }
        }

        Location viewer = sender instanceof Player p ? p.getLocation() : null;
        Bot bot = findBot(botName, viewer);
        if (bot == null) {
            sender.sendMessage(ChatColor.RED + "Bot not found: " + ChatColor.YELLOW + botName);
            return;
        }
        if (slot == null) {
            // Put it in the first empty hotbar slot, falling back to slot 0.
            int target = 0;
            for (int i = 0; i < 9; i++) {
                if (bot.getBukkitEntity().getInventory().getItem(i) == null) {
                    target = i;
                    break;
                }
            }
            slot = target;
        }
        if (slot < 0 || slot > 40) {
            sender.sendMessage(ChatColor.RED + "Slot must be 0-40 (0-8 hotbar, 9-35 storage, 36 boots, 37 legs, 38 chest, 39 head, 40 offhand).");
            return;
        }
        applyLoadoutSlot(bot, slot, item);
        sender.sendMessage("Gave " + ChatColor.YELLOW + type + ChatColor.RESET + " to "
                + ChatColor.GREEN + bot.getBotName() + ChatColor.RESET + " at slot "
                + ChatColor.BLUE + slot + ChatColor.RESET + ".");
    }

    @Autofill
    public List<String> giveAutofill(CommandSender sender, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 2) {
            String prefix = args[1].toUpperCase();
            for (Material m : Material.values()) {
                if (m.isItem() && m.name().startsWith(prefix)) out.add(m.name());
                if (out.size() >= 40) break;
            }
        } else if (args.length == 3) {
            out.addAll(manager.fetchNames());
        } else if (args.length == 4) {
            for (int i = 0; i <= 40; i++) out.add(String.valueOf(i));
        }
        return out;
    }

    private void armorTierSetup() {
        armorTiers.put("none", new ItemStack[]{
                new ItemStack(Material.AIR),
                new ItemStack(Material.AIR),
                new ItemStack(Material.AIR),
                new ItemStack(Material.AIR),
        });

        armorTiers.put("leather", new ItemStack[]{
                new ItemStack(Material.LEATHER_BOOTS),
                new ItemStack(Material.LEATHER_LEGGINGS),
                new ItemStack(Material.LEATHER_CHESTPLATE),
                new ItemStack(Material.LEATHER_HELMET),
        });

        armorTiers.put("chain", new ItemStack[]{
                new ItemStack(Material.CHAINMAIL_BOOTS),
                new ItemStack(Material.CHAINMAIL_LEGGINGS),
                new ItemStack(Material.CHAINMAIL_CHESTPLATE),
                new ItemStack(Material.CHAINMAIL_HELMET),
        });

        armorTiers.put("gold", new ItemStack[]{
                new ItemStack(Material.GOLDEN_BOOTS),
                new ItemStack(Material.GOLDEN_LEGGINGS),
                new ItemStack(Material.GOLDEN_CHESTPLATE),
                new ItemStack(Material.GOLDEN_HELMET),
        });

        armorTiers.put("iron", new ItemStack[]{
                new ItemStack(Material.IRON_BOOTS),
                new ItemStack(Material.IRON_LEGGINGS),
                new ItemStack(Material.IRON_CHESTPLATE),
                new ItemStack(Material.IRON_HELMET),
        });

        armorTiers.put("diamond", new ItemStack[]{
                new ItemStack(Material.DIAMOND_BOOTS),
                new ItemStack(Material.DIAMOND_LEGGINGS),
                new ItemStack(Material.DIAMOND_CHESTPLATE),
                new ItemStack(Material.DIAMOND_HELMET),
        });

        armorTiers.put("netherite", new ItemStack[]{
                new ItemStack(Material.NETHERITE_BOOTS),
                new ItemStack(Material.NETHERITE_LEGGINGS),
                new ItemStack(Material.NETHERITE_CHESTPLATE),
                new ItemStack(Material.NETHERITE_HELMET),
        });
    }

    @Command(
            name = "armor",
            desc = "Gives all bots an armor set.",
            autofill = "armorAutofill"
    )
    public void armor(CommandSender sender, @Arg("armor-tier") String armorTier) {
        String tier = armorTier.toLowerCase();

        if (!armorTiers.containsKey(tier)) {
            sender.sendMessage(ChatColor.YELLOW + tier + ChatColor.RESET + " is not a valid tier!");
            sender.sendMessage("Available tiers: " + ChatColor.YELLOW + String.join(ChatColor.RESET + ", " + ChatColor.YELLOW, armorTiers.keySet()));
            return;
        }

        ItemStack[] armor = armorTiers.get(tier);

        manager.fetch().forEach(bot -> {
            if (bot.getBukkitEntity() instanceof Player botPlayer) {
                botPlayer.getInventory().setArmorContents(armor);
                botPlayer.updateInventory();

                // packet sending to ensure
                bot.setItem(armor[0], EquipmentSlot.FEET);
                bot.setItem(armor[1], EquipmentSlot.LEGS);
                bot.setItem(armor[2], EquipmentSlot.CHEST);
                bot.setItem(armor[3], EquipmentSlot.HEAD);
            }
        });

        sender.sendMessage("Successfully set the armor tier to " + ChatColor.YELLOW + tier + ChatColor.RESET + " for all current bots.");
    }

    @Autofill
    public List<String> armorAutofill(CommandSender sender, String[] args) {
        return args.length == 2 ? new ArrayList<>(armorTiers.keySet()) : new ArrayList<>();
    }

    @Command(
            name = "info",
            desc = "Information about loaded bots.",
            autofill = "infoAutofill"
    )
    public void info(CommandSender sender, @Arg("bot-name") String name) {
        if (name == null) {
            sender.sendMessage(ChatColor.YELLOW + "Bot GUI coming soon!");
            return;
        }

        sender.sendMessage("Processing request...");

        scheduler.runTaskAsynchronously(plugin, () -> {
            try {
                Terminator bot = manager.getFirst(name, (sender instanceof Player pl) ? pl.getLocation() : null);

                if (bot == null) {
                    sender.sendMessage("Could not find bot " + ChatColor.GREEN + name + ChatColor.RESET + "!");
                    return;
                }

                /*
                 * time created
                 * current life (how long it has lived for)
                 * health
                 * inventory
                 * current target
                 * current kills
                 * skin
                 * neural network values (network name if loaded, otherwise RANDOM)
                 */

                String botName = bot.getBotName();
                String world = ChatColor.YELLOW + bot.getBukkitEntity().getWorld().getName();
                Location loc = bot.getLocation();
                String strLoc = ChatColor.YELLOW + formatter.format(loc.getX()) + ", " + formatter.format(loc.getY()) + ", " + formatter.format(loc.getZ());
                Vector vel = bot.getVelocity();
                String strVel = ChatColor.AQUA + formatter.format(vel.getX()) + ", " + formatter.format(vel.getY()) + ", " + formatter.format(vel.getZ());

                sender.sendMessage(ChatUtils.LINE);
                sender.sendMessage(ChatColor.GREEN + botName);
                sender.sendMessage(ChatUtils.BULLET_FORMATTED + "World: " + world);
                sender.sendMessage(ChatUtils.BULLET_FORMATTED + "Position: " + strLoc);
                sender.sendMessage(ChatUtils.BULLET_FORMATTED + "Velocity: " + strVel);
                sender.sendMessage(ChatUtils.LINE);
            } catch (Exception e) {
                sender.sendMessage(ChatUtils.EXCEPTION_MESSAGE);
            }
        });
    }

    @Autofill
    public List<String> infoAutofill(CommandSender sender, String[] args) {
        return args.length == 2 ? manager.fetchNames() : new ArrayList<>();
    }

    @Command(
            name = "count",
            desc = "Counts the amount of bots on screen by name.",
            aliases = {
                    "list"
            }
    )
    public void count(CommandSender sender) {
        List<String> names = manager.fetchNames();
        Map<String, Integer> freqMap = names.stream().collect(Collectors.toMap(s -> s, s -> 1, Integer::sum));
        List<Entry<String, Integer>> entries = freqMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).collect(Collectors.toList());

        sender.sendMessage(ChatUtils.LINE);
        entries.forEach(en -> sender.sendMessage(ChatColor.GREEN + en.getKey()
                + ChatColor.RESET + " - " + ChatColor.BLUE + en.getValue().toString() + ChatColor.RESET));
        sender.sendMessage("Total bots: " + ChatColor.BLUE + freqMap.values().stream().reduce(0, Integer::sum) + ChatColor.RESET);
        sender.sendMessage(ChatUtils.LINE);
    }

    @Command(
            name = "reset",
            desc = "Remove all loaded bots."
    )
    public void reset(CommandSender sender) {
        sender.sendMessage("Removing every bot...");
        int size = manager.fetch().size();
        manager.reset();
        sender.sendMessage("Removed " + ChatColor.RED + ChatUtils.NUMBER_FORMAT.format(size) + ChatColor.RESET + " entit" + (size == 1 ? "y" : "ies") + ".");

        if (aiManager == null) {
            this.aiManager = (AICommand) handler.getCommand("ai");
        }

        if (aiManager != null && aiManager.hasActiveSession()) {
            Bukkit.dispatchCommand(sender, "ai stop");
        }
    }

    /*
     * EVENTUALLY, we should make a command parent hierarchy system soon too! (so we don't have to do this crap)
     * basically, in the @Command annotation, you can include a "parent" for the command, so it will be a subcommand under the specified parent
     */
    @Command(
            name = "settings",
            desc = "Make changes to the global configuration file and bot-specific settings.",
            aliases = "options",
            autofill = "settingsAutofill"
    )
    public void settings(CommandSender sender, List<String> args) {
        String arg1 = args.isEmpty() ? null : args.get(0);
        String arg2 = args.size() < 2 ? null : args.get(1);

        String extra = ChatColor.GRAY + " [" + ChatColor.YELLOW + "/bot settings" + ChatColor.GRAY + "]";

        if (arg1 == null || (!arg1.equalsIgnoreCase("setgoal") && !arg1.equalsIgnoreCase("mobtarget") && !arg1.equalsIgnoreCase("playertarget")
                && !arg1.equalsIgnoreCase("addplayerlist") && !arg1.equalsIgnoreCase("region"))) {
            sender.sendMessage(ChatUtils.LINE);
            sender.sendMessage(ChatColor.GOLD + "Bot Settings" + extra);
            sender.sendMessage(ChatUtils.BULLET_FORMATTED + ChatColor.YELLOW + "setgoal" + ChatUtils.BULLET_FORMATTED + "Set the global bot target selection method.");
            sender.sendMessage(ChatUtils.BULLET_FORMATTED + ChatColor.YELLOW + "mobtarget" + ChatUtils.BULLET_FORMATTED + "Allow all bots to be targeted by hostile mobs.");
            sender.sendMessage(ChatUtils.BULLET_FORMATTED + ChatColor.YELLOW + "playertarget" + ChatUtils.BULLET_FORMATTED + "Sets a player name for spawned bots to focus on if the goal is PLAYER.");
            sender.sendMessage(ChatUtils.BULLET_FORMATTED + ChatColor.YELLOW + "addplayerlist" + ChatUtils.BULLET_FORMATTED + "Adds newly spawned bots to the player list. This allows the bots to be affected by player selectors like @a and @p.");
            sender.sendMessage(ChatUtils.BULLET_FORMATTED + ChatColor.YELLOW + "region" + ChatUtils.BULLET_FORMATTED + "Sets a region for the bots to prioritize entities inside.");
            sender.sendMessage(ChatUtils.LINE);
            return;
        } else if (arg1.equalsIgnoreCase("setgoal")) {
            if (arg2 == null) {
                sender.sendMessage("The global bot goal is currently " + ChatColor.BLUE + agent.getTargetType() + ChatColor.RESET + ".");
                return;
            }
            EnumTargetGoal goal = EnumTargetGoal.from(arg2);

            if (goal == null) {
                sender.sendMessage(ChatUtils.LINE);
                sender.sendMessage(ChatColor.GOLD + "Goal Selection Types" + extra);
                Arrays.stream(EnumTargetGoal.values()).forEach(g -> sender.sendMessage(ChatUtils.BULLET_FORMATTED + ChatColor.YELLOW + g.name().replace("_", "").toLowerCase()
                        + ChatUtils.BULLET_FORMATTED + g.description()));
                sender.sendMessage(ChatUtils.LINE);
                return;
            }
            agent.setTargetType(goal);
            sender.sendMessage("The global bot goal has been set to " + ChatColor.BLUE + goal.name() + ChatColor.RESET + ".");
        } else if (arg1.equalsIgnoreCase("mobtarget")) {
            if (arg2 == null) {
                sender.sendMessage("Mob targeting is currently " + (manager.isMobTarget() ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled") + ChatColor.RESET + ".");
                return;
            }
            if (!arg2.equals("true") && !arg2.equals("false")) {
                sender.sendMessage(ChatColor.RED + "You must specify true or false!");
                return;
            }
            manager.setMobTarget(Boolean.parseBoolean(arg2));
            sender.sendMessage("Mob targeting is now " + (manager.isMobTarget() ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled") + ChatColor.RESET + ".");
        } else if (arg1.equalsIgnoreCase("playertarget")) {
            if (args.size() < 2) {
                sender.sendMessage(ChatColor.RED + "You must specify a player name!");
                return;
            }
            String playerName = arg2;
            Player player = Bukkit.getPlayer(playerName);
            if (player == null) {
                sender.sendMessage(ChatColor.RED + "Could not find player " + ChatColor.YELLOW + playerName + ChatColor.RED + "!");
                return;
            }
            for (Terminator fetch : manager.fetch()) {
                fetch.setTargetPlayer(player.getUniqueId());
            }
            sender.sendMessage("All spawned bots are now set to target " + ChatColor.BLUE + player.getName() + ChatColor.RESET + ". They will target the closest player if they can't be found.\nYou may need to set the goal to PLAYER.");
        } else if (arg1.equalsIgnoreCase("addplayerlist")) {
            if (arg2 == null) {
                sender.sendMessage("Adding bots to the player list is currently " + (manager.addToPlayerList() ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled") + ChatColor.RESET + ".");
                return;
            }
            if (!arg2.equals("true") && !arg2.equals("false")) {
                sender.sendMessage(ChatColor.RED + "You must specify true or false!");
                return;
            }
            manager.setAddToPlayerList(Boolean.parseBoolean(arg2));
            sender.sendMessage("Adding bots to the player list is now " + (manager.addToPlayerList() ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled") + ChatColor.RESET + ".");
        } else if (arg1.equalsIgnoreCase("region")) {
            if (arg2 == null) {
                if (agent.getRegion() == null) {
                    sender.sendMessage("No region has been set.");
                    return;
                }
                sender.sendMessage("The current region is " + ChatColor.BLUE + agent.getRegion() + ChatColor.RESET + ".");
                if (agent.getRegionWeightX() == 0 && agent.getRegionWeightY() == 0 && agent.getRegionWeightZ() == 0)
                    sender.sendMessage("Entities out of range will not be targeted.");
                else {
                    sender.sendMessage("The region X weight is " + ChatColor.BLUE + agent.getRegionWeightX() + ChatColor.RESET + ".");
                    sender.sendMessage("The region Y weight is " + ChatColor.BLUE + agent.getRegionWeightY() + ChatColor.RESET + ".");
                    sender.sendMessage("The region Z weight is " + ChatColor.BLUE + agent.getRegionWeightZ() + ChatColor.RESET + ".");
                }
                return;
            }
            if (arg2.equalsIgnoreCase("clear")) {
                agent.setRegion(null, 0, 0, 0);
                sender.sendMessage("The region has been cleared.");
                return;
            }
            boolean strict = args.size() == 8 && args.get(7).equalsIgnoreCase("strict");
            if (args.size() != 10 && !strict) {
                sender.sendMessage(ChatUtils.LINE);
                sender.sendMessage(ChatColor.GOLD + "Bot Region Settings" + extra);
                sender.sendMessage(ChatUtils.BULLET_FORMATTED + ChatColor.YELLOW + "<x1> <y1> <z1> <x2> <y2> <z2> <wX> <wY> <wZ>" + ChatUtils.BULLET_FORMATTED
                        + "Sets a region for bots to prioritize entities within.");
                sender.sendMessage(ChatUtils.BULLET_FORMATTED + ChatColor.YELLOW + "<x1> <y1> <z1> <x2> <y2> <z2> strict" + ChatUtils.BULLET_FORMATTED
                        + "Sets a region so that the bots only target entities within the region.");
                sender.sendMessage(ChatUtils.BULLET_FORMATTED + ChatColor.YELLOW + "clear" + ChatUtils.BULLET_FORMATTED
                        + "Clears the region.");
                sender.sendMessage("Without strict mode, the entity distance from the region is multiplied by the weight values if outside the region.");
                sender.sendMessage("The resulting value is added to the entity distance when selecting an entity.");
                sender.sendMessage(ChatUtils.LINE);
                return;
            }
            double x1, y1, z1, x2, y2, z2, wX, wY, wZ;
            try {
                Location loc = sender instanceof Player pl ? pl.getLocation() : null;
                x1 = parseDoubleOrRelative(args.get(1), loc, 0);
                y1 = parseDoubleOrRelative(args.get(2), loc, 1);
                z1 = parseDoubleOrRelative(args.get(3), loc, 2);
                x2 = parseDoubleOrRelative(args.get(4), loc, 0);
                y2 = parseDoubleOrRelative(args.get(5), loc, 1);
                z2 = parseDoubleOrRelative(args.get(6), loc, 2);
                if (strict)
                    wX = wY = wZ = 0;
                else {
                    wX = Double.parseDouble(args.get(7));
                    wY = Double.parseDouble(args.get(8));
                    wZ = Double.parseDouble(args.get(9));
                    if (wX <= 0 || wY <= 0 || wZ <= 0) {
                        sender.sendMessage("The region weights must be positive values!");
                        return;
                    }
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("The region bounds and weights must be valid numbers!");
                sender.sendMessage("Correct syntax: " + ChatColor.YELLOW + "/bot settings region <x1> <y1> <z1> <x2> <y2> <z2> <wX> <wY> <wZ>"
                        + ChatColor.RESET);
                return;
            }
            agent.setRegion(new BoundingBox(x1, y1, z1, x2, y2, z2), wX, wY, wZ);
            sender.sendMessage("The region has been set to " + ChatColor.BLUE + agent.getRegion() + ChatColor.RESET + ".");
            if (wX == 0 && wY == 0 && wZ == 0)
                sender.sendMessage("Entities out of range will not be targeted.");
            else {
                sender.sendMessage("The region X weight is " + ChatColor.BLUE + agent.getRegionWeightX() + ChatColor.RESET + ".");
                sender.sendMessage("The region Y weight is " + ChatColor.BLUE + agent.getRegionWeightY() + ChatColor.RESET + ".");
                sender.sendMessage("The region Z weight is " + ChatColor.BLUE + agent.getRegionWeightZ() + ChatColor.RESET + ".");
            }
        }
    }

    @Autofill
    public List<String> settingsAutofill(CommandSender sender, String[] args) {
        List<String> output = new ArrayList<>();

        // More settings:
        // setitem
        // tpall
        // tprandom
        // hidenametags or nametags <show/hide>
        // sitall
        // lookall

        if (args.length == 2) {
            output.add("setgoal");
            output.add("mobtarget");
            output.add("playertarget");
            output.add("addplayerlist");
            output.add("region");
        } else if (args.length == 3) {
            if (args[1].equalsIgnoreCase("setgoal")) {
                Arrays.stream(EnumTargetGoal.values()).forEach(goal -> output.add(goal.name().replace("_", "").toLowerCase()));
            }
            if (args[1].equalsIgnoreCase("mobtarget")) {
                output.add("true");
                output.add("false");
            }
            if (args[1].equalsIgnoreCase("playertarget")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    output.add(player.getName());
                }
            }
            if (args[1].equalsIgnoreCase("addplayerlist")) {
                output.add("true");
                output.add("false");
            }
        }

        return output;
    }

    @Command(
            name = "debug",
            desc = "Debug plugin code.",
            visible = false,
            autofill = "debugAutofill"
    )
    public void debug(CommandSender sender, @Arg("expression") String expression) {
        new Debugger(sender).execute(expression);
    }

    @Autofill
    public List<String> debugAutofill(CommandSender sender, String[] args) {
        return args.length == 2 ? new ArrayList<>(Debugger.AUTOFILL_METHODS) : new ArrayList<>();
    }

    @Command(
            name = "weapons",
            desc = "Show which combat behaviors each bot's inventory unlocks.",
            autofill = "weaponsAutofill"
    )
    public void weapons(CommandSender sender, @OptArg("bot-name") String botName) {
        Location viewer = sender instanceof Player p ? p.getLocation() : null;

        java.util.List<Bot> bots = new ArrayList<>();
        if (botName != null && !botName.isEmpty()) {
            Bot b = findBot(botName, viewer);
            if (b == null) {
                sender.sendMessage(ChatColor.RED + "Bot not found: " + ChatColor.YELLOW + botName);
                return;
            }
            bots.add(b);
        } else {
            for (Terminator t : manager.fetch()) {
                if (t instanceof Bot b) bots.add(b);
            }
        }
        if (bots.isEmpty()) {
            sender.sendMessage("No bots to report on.");
            return;
        }

        sender.sendMessage(ChatUtils.LINE);
        sender.sendMessage(ChatColor.GOLD + "Bot Weapons");
        for (Bot bot : bots) {
            net.nuggetmc.tplus.bot.loadout.BotInventory inv = bot.getBotInventory();
            StringBuilder sb = new StringBuilder(ChatUtils.BULLET_FORMATTED);
            sb.append(ChatColor.GREEN).append(bot.getBotName()).append(ChatColor.RESET).append(": ");
            appendFlag(sb, "melee", inv.findSword() >= 0 || inv.findAxe() >= 0);
            appendFlag(sb, "mace", inv.hasMace());
            appendFlag(sb, "trident", inv.hasTrident());
            appendFlag(sb, "pearl", inv.hasEnderPearl());
            appendFlag(sb, "windcharge", inv.hasWindCharge());
            appendFlag(sb, "crystal", inv.hasCrystalKit());
            appendFlag(sb, "anchor", inv.hasAnchorKit());
            appendFlag(sb, "cobweb", inv.hasCobweb());
            appendFlag(sb, "totem", inv.hasTotem());
            appendFlag(sb, "elytra", inv.hasElytra());
            appendFlag(sb, "firework", inv.hasFirework());
            sender.sendMessage(sb.toString());
        }
        sender.sendMessage(ChatUtils.LINE);
    }

    private static void appendFlag(StringBuilder sb, String label, boolean on) {
        sb.append(on ? ChatColor.YELLOW : ChatColor.DARK_GRAY).append(label).append(ChatColor.RESET).append(' ');
    }

    @Autofill
    public List<String> weaponsAutofill(CommandSender sender, String[] args) {
        return args.length == 2 ? manager.fetchNames() : new ArrayList<>();
    }

    @Command(
            name = "combatdebug",
            desc = "Toggle verbose combat-decision logging for one bot or all bots.",
            aliases = {"cdbg"},
            autofill = "combatDebugAutofill"
    )
    public void combatDebug(CommandSender sender, @Arg("name-or-all") String target, @Arg("on-off") String state) {
        boolean turnOn;
        if (state.equalsIgnoreCase("on") || state.equalsIgnoreCase("true") || state.equalsIgnoreCase("1")) {
            turnOn = true;
        } else if (state.equalsIgnoreCase("off") || state.equalsIgnoreCase("false") || state.equalsIgnoreCase("0")) {
            turnOn = false;
        } else {
            sender.sendMessage(ChatColor.RED + "Usage: /bot combatdebug <botName|all> <on|off>");
            return;
        }

        if (target.equalsIgnoreCase("all")) {
            if (turnOn) {
                net.nuggetmc.tplus.bot.combat.CombatDebugger.enableAll();
                sender.sendMessage(ChatColor.GREEN + "Combat debug enabled for ALL bots. Output goes to server console / server.log.");
            } else {
                net.nuggetmc.tplus.bot.combat.CombatDebugger.disableAll();
                sender.sendMessage(ChatColor.YELLOW + "Combat debug disabled for all bots.");
            }
            return;
        }

        java.util.List<Bot> matches = manager.getAllByName(target);
        if (matches.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No bots named " + ChatColor.YELLOW + target);
            return;
        }
        for (Bot b : matches) {
            if (turnOn) net.nuggetmc.tplus.bot.combat.CombatDebugger.enable(b.getUUID());
            else net.nuggetmc.tplus.bot.combat.CombatDebugger.disable(b.getUUID());
        }
        sender.sendMessage((turnOn ? ChatColor.GREEN + "Enabled" : ChatColor.YELLOW + "Disabled")
                + ChatColor.RESET + " combat debug for " + matches.size() + " bot(s) named "
                + ChatColor.YELLOW + target + ChatColor.RESET
                + (turnOn ? ". Tail server.log or watch the console for [tplus-cbt] lines." : "."));
    }

    @Autofill
    public List<String> combatDebugAutofill(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> out = new ArrayList<>(manager.fetchNames());
            out.add("all");
            return out;
        }
        if (args.length == 3) return Arrays.asList("on", "off");
        return new ArrayList<>();
    }

    @Command(
            name = "gather",
            desc = "Teleport every living bot to your current location.",
            aliases = {"tpall"}
    )
    public void gather(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be run by a player.");
            return;
        }
        Location dest = player.getLocation();
        int moved = 0;
        for (net.nuggetmc.tplus.api.Terminator t : manager.fetch()) {
            if (!t.isBotAlive()) continue;
            t.getBukkitEntity().teleport(dest);
            moved++;
        }
        sender.sendMessage(ChatColor.YELLOW.toString() + moved + ChatColor.RESET
                + " bot(s) gathered to you.");
    }

    @Command(
            name = "inventory",
            desc = "Opens the inventory editor GUI for a bot.",
            aliases = {"inv"},
            autofill = "inventoryAutofill"
    )
    public void inventory(CommandSender sender, @Arg("bot-name") String name) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be run by a player.");
            return;
        }
        Bot bot = findBot(name, player.getLocation());
        if (bot == null) {
            sender.sendMessage("Could not find bot " + ChatColor.GREEN + name + ChatColor.RESET + "!");
            return;
        }
        new BotInventoryGUI(bot).open(player);
    }

    @Autofill
    public List<String> inventoryAutofill(CommandSender sender, String[] args) {
        return args.length == 2 ? manager.fetchNames() : new ArrayList<>();
    }

    @Command(
            name = "preset",
            desc = "Save, load, list, and delete bot presets.",
            autofill = "presetAutofill"
    )
    public void preset(CommandSender sender, List<String> args) {
        PresetManager presets = plugin.getPresetManager();
        String action = args.isEmpty() ? null : args.get(0);

        if (action == null) {
            sender.sendMessage(ChatUtils.LINE);
            sender.sendMessage(ChatColor.GOLD + "Bot Presets" + ChatColor.GRAY + " [" + ChatColor.YELLOW + "/bot preset" + ChatColor.GRAY + "]");
            sender.sendMessage(ChatUtils.BULLET_FORMATTED + ChatColor.YELLOW + "save <name> <bot-name>" + ChatUtils.BULLET_FORMATTED + "Save a bot's state as a preset.");
            sender.sendMessage(ChatUtils.BULLET_FORMATTED + ChatColor.YELLOW + "apply <name> [bot-name]" + ChatUtils.BULLET_FORMATTED + "Apply a preset to one bot, or to ALL if no name given.");
            sender.sendMessage(ChatUtils.BULLET_FORMATTED + ChatColor.YELLOW + "list" + ChatUtils.BULLET_FORMATTED + "List all saved presets.");
            sender.sendMessage(ChatUtils.BULLET_FORMATTED + ChatColor.YELLOW + "delete <name>" + ChatUtils.BULLET_FORMATTED + "Delete a preset.");
            sender.sendMessage(ChatUtils.LINE);
            return;
        }

        switch (action.toLowerCase()) {
            case "list" -> {
                List<String> names = presets.list();
                if (names.isEmpty()) {
                    sender.sendMessage("No presets saved yet. Use " + ChatColor.YELLOW + "/bot preset save <name> <bot-name>" + ChatColor.RESET + ".");
                    return;
                }
                sender.sendMessage(ChatUtils.LINE);
                sender.sendMessage(ChatColor.GOLD + "Saved Presets (" + names.size() + ")");
                names.forEach(n -> sender.sendMessage(ChatUtils.BULLET_FORMATTED + ChatColor.YELLOW + n));
                sender.sendMessage(ChatUtils.LINE);
            }
            case "save" -> {
                if (args.size() < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /bot preset save <preset-name> <bot-name>");
                    return;
                }
                String presetName = args.get(1);
                String botName = args.get(2);
                Location viewer = sender instanceof Player p ? p.getLocation() : null;
                Bot bot = findBot(botName, viewer);
                if (bot == null) {
                    sender.sendMessage(ChatColor.RED + "Bot not found: " + ChatColor.YELLOW + botName);
                    return;
                }
                try {
                    BotPreset preset = presets.capture(bot, presetName);
                    presets.save(preset);
                    sender.sendMessage("Saved preset " + ChatColor.YELLOW + presetName + ChatColor.RESET + " from " + ChatColor.GREEN + botName + ChatColor.RESET + ".");
                } catch (Exception e) {
                    sender.sendMessage(ChatColor.RED + "Failed to save preset: " + e.getMessage());
                }
            }
            case "load", "apply" -> {
                if (args.size() < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /bot preset apply <preset-name> [bot-name]");
                    return;
                }
                String presetName = args.get(1);
                BotPreset preset = presets.load(presetName);
                if (preset == null) {
                    sender.sendMessage(ChatColor.RED + "Preset not found: " + ChatColor.YELLOW + presetName);
                    return;
                }
                if (args.size() >= 3) {
                    String botName = args.get(2);
                    Location viewer = sender instanceof Player p ? p.getLocation() : null;
                    Bot bot = findBot(botName, viewer);
                    if (bot == null) {
                        sender.sendMessage(ChatColor.RED + "Bot not found: " + ChatColor.YELLOW + botName);
                        return;
                    }
                    presets.apply(preset, bot);
                    sender.sendMessage("Applied preset " + ChatColor.YELLOW + presetName + ChatColor.RESET + " to " + ChatColor.GREEN + botName + ChatColor.RESET + ".");
                } else {
                    int n = presets.applyToAll(preset);
                    sender.sendMessage("Applied preset " + ChatColor.YELLOW + presetName + ChatColor.RESET + " to " + ChatColor.BLUE + n + ChatColor.RESET + " bot(s).");
                }
            }
            case "delete" -> {
                if (args.size() < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /bot preset delete <preset-name>");
                    return;
                }
                String presetName = args.get(1);
                if (presets.delete(presetName)) {
                    sender.sendMessage("Deleted preset " + ChatColor.YELLOW + presetName + ChatColor.RESET + ".");
                } else {
                    sender.sendMessage(ChatColor.RED + "Preset not found: " + ChatColor.YELLOW + presetName);
                }
            }
            default -> sender.sendMessage(ChatColor.RED + "Unknown action. See /bot preset for help.");
        }
    }

    @Autofill
    public List<String> presetAutofill(CommandSender sender, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 2) {
            out.add("save");
            out.add("load");
            out.add("apply");
            out.add("list");
            out.add("delete");
        } else if (args.length == 3) {
            String action = args[1].toLowerCase();
            if (action.equals("load") || action.equals("apply") || action.equals("delete")) {
                out.addAll(plugin.getPresetManager().list());
            } else if (action.equals("save")) {
                out.add("<preset-name>");
            }
        } else if (args.length == 4) {
            String action = args[1].toLowerCase();
            if (action.equals("save") || action.equals("apply")) {
                out.addAll(manager.fetchNames());
            }
        }
        return out;
    }

    @Command(
            name = "loadout",
            desc = "Apply a predefined combat loadout. Usage: /bot loadout <name> [bot-name]",
            autofill = "loadoutAutofill"
    )
    public void loadout(CommandSender sender, @Arg("name") String name, @OptArg("bot-name") String botName) {
        String key = name == null ? "" : name.toLowerCase();
        ItemStack[] kit = buildLoadout(key);
        if (kit == null) {
            sender.sendMessage(ChatColor.RED + "Unknown loadout: " + ChatColor.YELLOW + key);
            sender.sendMessage("Available: " + ChatColor.YELLOW + String.join(", ", LOADOUT_NAMES));
            return;
        }

        // `clear` releases the loadout lock so baseline movement-kit refills resume;
        // every other preset is a deliberate authoritative kit.
        boolean respectAfterApply = !"clear".equals(key);

        if (botName != null && !botName.isEmpty()) {
            Location viewer = sender instanceof Player p ? p.getLocation() : null;
            Bot bot = findBot(botName, viewer);
            if (bot == null) {
                sender.sendMessage(ChatColor.RED + "Bot not found: " + ChatColor.YELLOW + botName);
                return;
            }
            applyLoadoutToBot(bot, kit, respectAfterApply);
            sender.sendMessage("Applied loadout " + ChatColor.YELLOW + key + ChatColor.RESET + " to " + ChatColor.GREEN + bot.getBotName() + ChatColor.RESET + ".");
            return;
        }

        int count = 0;
        for (Terminator t : manager.fetch()) {
            if (!(t instanceof Bot bot)) continue;
            applyLoadoutToBot(bot, kit, respectAfterApply);
            count++;
        }
        sender.sendMessage("Applied loadout " + ChatColor.YELLOW + key + ChatColor.RESET + " to " + ChatColor.BLUE + count + ChatColor.RESET + " bot(s).");
    }

    private static void applyLoadoutToBot(Bot bot, ItemStack[] kit, boolean respectAfterApply) {
        // Clear via NMS too — Bukkit's PlayerInventory.clear hits the same
        // container-transaction path that gets rolled back for bots.
        // Skip writes when the slot is already empty to avoid resetting the
        // attack-strength ticker on the mainhand slot (see the same guard in
        // BotInventoryGUI.syncToBot and BotInventory.setSelectedHotbarSlot).
        net.minecraft.world.entity.player.Inventory nmsInv = bot.getInventory();
        boolean changed = false;
        for (int i = 0; i < 36; i++) {
            if (!nmsInv.getItem(i).isEmpty()) {
                nmsInv.setItem(i, net.minecraft.world.item.ItemStack.EMPTY);
                changed = true;
            }
        }
        if (changed) nmsInv.setChanged();
        bot.setItem(new ItemStack(Material.AIR), EquipmentSlot.HEAD);
        bot.setItem(new ItemStack(Material.AIR), EquipmentSlot.CHEST);
        bot.setItem(new ItemStack(Material.AIR), EquipmentSlot.LEGS);
        bot.setItem(new ItemStack(Material.AIR), EquipmentSlot.FEET);
        bot.setItemOffhand(new ItemStack(Material.AIR));

        for (int i = 0; i < kit.length && i < 41; i++) {
            if (kit[i] == null) continue;
            applyLoadoutSlot(bot, i, kit[i].clone());
        }
        bot.selectHotbarSlot(0);
        // Run autoEquip so any "wind charges in offhand when mace in hand"
        // rule (see BotInventory.autoEquip) takes effect.
        BotInventory inv = bot.getBotInventory();
        inv.autoEquip();
        // Flip the loadout lock so the 40-tick ensureMovementKit refill respects
        // what the preset actually chose (including deliberate omission of pearls /
        // wind charges). The `clear` preset passes false to restore the default refill.
        if (respectAfterApply) {
            inv.markLoadoutApplied();
        } else {
            inv.markLoadoutCleared();
        }
    }

    @Autofill
    public List<String> loadoutAutofill(CommandSender sender, String[] args) {
        if (args.length == 2) return new ArrayList<>(Arrays.asList(LOADOUT_NAMES));
        if (args.length == 3) return manager.fetchNames();
        return new ArrayList<>();
    }

    private static final String[] LOADOUT_NAMES = {
            "sword", "mace", "trident", "windcharge", "skydiver",
            "crystalpvp", "anchorbomb", "pvp", "hybrid",
            // Minecraft Wiki PvP kit taxonomy (cart + UHC intentionally excluded).
            "vanilla", "axe", "smp", "pot", "spear",
            "clear"
    };

    /**
     * Build a 41-slot loadout array for a named preset.
     * 0–8 hotbar, 9–35 storage, 36 boots, 37 legs, 38 chest, 39 head, 40 offhand.
     */
    private static ItemStack[] buildLoadout(String key) {
        ItemStack[] kit = new ItemStack[41];
        switch (key) {
            case "sword" -> {
                kit[0] = new ItemStack(Material.NETHERITE_SWORD);
                kit[36] = new ItemStack(Material.NETHERITE_BOOTS);
                kit[37] = new ItemStack(Material.NETHERITE_LEGGINGS);
                kit[38] = new ItemStack(Material.NETHERITE_CHESTPLATE);
                kit[39] = new ItemStack(Material.NETHERITE_HELMET);
                kit[40] = new ItemStack(Material.SHIELD);
            }
            case "mace" -> {
                kit[0] = new ItemStack(Material.MACE);
                kit[1] = new ItemStack(Material.IRON_SWORD);
                ItemStack windCharges = new ItemStack(Material.WIND_CHARGE);
                windCharges.setAmount(16);
                kit[40] = windCharges; // offhand — wiki Mace-PvP pairing
                kit[36] = new ItemStack(Material.NETHERITE_BOOTS);
                kit[37] = new ItemStack(Material.NETHERITE_LEGGINGS);
                kit[38] = new ItemStack(Material.NETHERITE_CHESTPLATE);
                kit[39] = new ItemStack(Material.NETHERITE_HELMET);
            }
            case "trident" -> {
                kit[0] = new ItemStack(Material.TRIDENT);
                kit[1] = new ItemStack(Material.IRON_SWORD);
                kit[36] = new ItemStack(Material.IRON_BOOTS);
                kit[37] = new ItemStack(Material.IRON_LEGGINGS);
                kit[38] = new ItemStack(Material.IRON_CHESTPLATE);
                kit[39] = new ItemStack(Material.IRON_HELMET);
            }
            case "windcharge" -> {
                ItemStack wc = new ItemStack(Material.WIND_CHARGE);
                wc.setAmount(16);
                kit[0] = wc;
                kit[1] = new ItemStack(Material.IRON_SWORD);
                kit[36] = new ItemStack(Material.IRON_BOOTS);
                kit[37] = new ItemStack(Material.IRON_LEGGINGS);
                kit[38] = new ItemStack(Material.IRON_CHESTPLATE);
                kit[39] = new ItemStack(Material.IRON_HELMET);
            }
            case "skydiver" -> {
                // Elytra kit that swaps to chestplate on the ground (stored in inv).
                kit[0] = new ItemStack(Material.TRIDENT);
                kit[1] = new ItemStack(Material.IRON_SWORD);
                ItemStack rockets = new ItemStack(Material.FIREWORK_ROCKET);
                rockets.setAmount(8);
                kit[2] = rockets;
                kit[9] = new ItemStack(Material.DIAMOND_CHESTPLATE);
                kit[36] = new ItemStack(Material.DIAMOND_BOOTS);
                kit[37] = new ItemStack(Material.DIAMOND_LEGGINGS);
                kit[38] = new ItemStack(Material.ELYTRA);
                kit[39] = new ItemStack(Material.DIAMOND_HELMET);
            }
            case "hybrid" -> {
                kit[0] = new ItemStack(Material.NETHERITE_SWORD);
                kit[1] = new ItemStack(Material.MACE);
                kit[2] = new ItemStack(Material.TRIDENT);
                ItemStack wc = new ItemStack(Material.WIND_CHARGE);
                wc.setAmount(16);
                kit[3] = wc;
                ItemStack apples = new ItemStack(Material.GOLDEN_APPLE);
                apples.setAmount(4);
                kit[8] = apples;
                kit[36] = new ItemStack(Material.NETHERITE_BOOTS);
                kit[37] = new ItemStack(Material.NETHERITE_LEGGINGS);
                kit[38] = new ItemStack(Material.NETHERITE_CHESTPLATE);
                kit[39] = new ItemStack(Material.NETHERITE_HELMET);
                kit[40] = new ItemStack(Material.SHIELD);
            }
            case "crystalpvp" -> {
                kit[0] = new ItemStack(Material.NETHERITE_SWORD);
                ItemStack crystals = new ItemStack(Material.END_CRYSTAL);
                crystals.setAmount(32);
                kit[1] = crystals;
                ItemStack obsidian = new ItemStack(Material.OBSIDIAN);
                obsidian.setAmount(32);
                kit[2] = obsidian;
                ItemStack pearls = new ItemStack(Material.ENDER_PEARL);
                pearls.setAmount(16);
                kit[3] = pearls;
                ItemStack gaps = new ItemStack(Material.GOLDEN_APPLE);
                gaps.setAmount(8);
                kit[4] = gaps;
                kit[7] = new ItemStack(Material.TOTEM_OF_UNDYING);
                kit[36] = new ItemStack(Material.NETHERITE_BOOTS);
                kit[37] = new ItemStack(Material.NETHERITE_LEGGINGS);
                kit[38] = new ItemStack(Material.NETHERITE_CHESTPLATE);
                kit[39] = new ItemStack(Material.NETHERITE_HELMET);
                kit[40] = new ItemStack(Material.TOTEM_OF_UNDYING);
            }
            case "anchorbomb" -> {
                // Nether-focused: respawn anchors + glowstone + pearls to reposition.
                kit[0] = new ItemStack(Material.NETHERITE_SWORD);
                ItemStack anchors = new ItemStack(Material.RESPAWN_ANCHOR);
                anchors.setAmount(16);
                kit[1] = anchors;
                ItemStack glow = new ItemStack(Material.GLOWSTONE);
                glow.setAmount(32);
                kit[2] = glow;
                ItemStack pearls = new ItemStack(Material.ENDER_PEARL);
                pearls.setAmount(16);
                kit[3] = pearls;
                kit[4] = makePotion(Material.POTION, PotionType.FIRE_RESISTANCE);
                kit[7] = new ItemStack(Material.TOTEM_OF_UNDYING);
                kit[36] = new ItemStack(Material.NETHERITE_BOOTS);
                kit[37] = new ItemStack(Material.NETHERITE_LEGGINGS);
                kit[38] = new ItemStack(Material.NETHERITE_CHESTPLATE);
                kit[39] = new ItemStack(Material.NETHERITE_HELMET);
                kit[40] = new ItemStack(Material.TOTEM_OF_UNDYING);
            }
            case "pvp" -> {
                // Everything bagel: tries every behavior.
                kit[0] = new ItemStack(Material.NETHERITE_SWORD);
                kit[1] = new ItemStack(Material.MACE);
                kit[2] = new ItemStack(Material.TRIDENT);
                ItemStack wc = new ItemStack(Material.WIND_CHARGE);
                wc.setAmount(16);
                kit[3] = wc;
                ItemStack pearls = new ItemStack(Material.ENDER_PEARL);
                pearls.setAmount(16);
                kit[4] = pearls;
                ItemStack crystals = new ItemStack(Material.END_CRYSTAL);
                crystals.setAmount(16);
                kit[5] = crystals;
                ItemStack obsidian = new ItemStack(Material.OBSIDIAN);
                obsidian.setAmount(32);
                kit[6] = obsidian;
                ItemStack webs = new ItemStack(Material.COBWEB);
                webs.setAmount(16);
                kit[7] = webs;
                ItemStack gaps = new ItemStack(Material.GOLDEN_APPLE);
                gaps.setAmount(8);
                kit[8] = gaps;
                ItemStack rockets = new ItemStack(Material.FIREWORK_ROCKET);
                rockets.setAmount(16);
                kit[9] = rockets;
                kit[10] = new ItemStack(Material.DIAMOND_CHESTPLATE);
                kit[36] = new ItemStack(Material.NETHERITE_BOOTS);
                kit[37] = new ItemStack(Material.NETHERITE_LEGGINGS);
                kit[38] = new ItemStack(Material.ELYTRA);
                kit[39] = new ItemStack(Material.NETHERITE_HELMET);
                kit[40] = new ItemStack(Material.TOTEM_OF_UNDYING);
            }
            case "vanilla" -> {
                // Vanilla PvP / VPvP — full arsenal minus enchanted apples + elytra.
                // Nether fallback (anchors + glowstone) lives in storage since the hotbar is full.
                kit[0] = new ItemStack(Material.NETHERITE_SWORD);
                kit[1] = new ItemStack(Material.MACE);
                ItemStack crystals = new ItemStack(Material.END_CRYSTAL);
                crystals.setAmount(16);
                kit[2] = crystals;
                ItemStack obsidian = new ItemStack(Material.OBSIDIAN);
                obsidian.setAmount(32);
                kit[3] = obsidian;
                ItemStack wc = new ItemStack(Material.WIND_CHARGE);
                wc.setAmount(16);
                kit[4] = wc;
                ItemStack pearls = new ItemStack(Material.ENDER_PEARL);
                pearls.setAmount(16);
                kit[5] = pearls;
                ItemStack gaps = new ItemStack(Material.GOLDEN_APPLE);
                gaps.setAmount(8);
                kit[6] = gaps;
                ItemStack webs = new ItemStack(Material.COBWEB);
                webs.setAmount(8);
                kit[7] = webs;
                kit[8] = new ItemStack(Material.TOTEM_OF_UNDYING);
                ItemStack anchors = new ItemStack(Material.RESPAWN_ANCHOR);
                anchors.setAmount(16);
                kit[9] = anchors;
                ItemStack glow = new ItemStack(Material.GLOWSTONE);
                glow.setAmount(16);
                kit[10] = glow;
                kit[36] = new ItemStack(Material.NETHERITE_BOOTS);
                kit[37] = new ItemStack(Material.NETHERITE_LEGGINGS);
                kit[38] = new ItemStack(Material.NETHERITE_CHESTPLATE);
                kit[39] = new ItemStack(Material.NETHERITE_HELMET);
                kit[40] = new ItemStack(Material.SHIELD);
            }
            case "axe" -> {
                // Axe PvP — axe disables shields; sword as secondary for follow-ups.
                kit[0] = new ItemStack(Material.NETHERITE_AXE);
                kit[1] = new ItemStack(Material.NETHERITE_SWORD);
                ItemStack gaps = new ItemStack(Material.GOLDEN_APPLE);
                gaps.setAmount(4);
                kit[2] = gaps;
                kit[36] = new ItemStack(Material.NETHERITE_BOOTS);
                kit[37] = new ItemStack(Material.NETHERITE_LEGGINGS);
                kit[38] = new ItemStack(Material.NETHERITE_CHESTPLATE);
                kit[39] = new ItemStack(Material.NETHERITE_HELMET);
                kit[40] = new ItemStack(Material.SHIELD);
            }
            case "smp" -> {
                // SMP / Netherite PvP — sword primary, axe fallback. No mace/crystals/anchors (explosive-banned).
                kit[0] = new ItemStack(Material.NETHERITE_SWORD);
                kit[1] = new ItemStack(Material.NETHERITE_AXE);
                ItemStack gaps = new ItemStack(Material.GOLDEN_APPLE);
                gaps.setAmount(4);
                kit[2] = gaps;
                kit[36] = new ItemStack(Material.NETHERITE_BOOTS);
                kit[37] = new ItemStack(Material.NETHERITE_LEGGINGS);
                kit[38] = new ItemStack(Material.NETHERITE_CHESTPLATE);
                kit[39] = new ItemStack(Material.NETHERITE_HELMET);
                kit[40] = new ItemStack(Material.SHIELD);
            }
            case "pot" -> {
                // Pot PvP — splash healing self-heals are the core mechanic. No shield per spec;
                // pearls added per user for practical bot combat (reposition / gap-close).
                // Splash potions are non-stackable, so each gets its own slot.
                kit[0] = new ItemStack(Material.NETHERITE_SWORD);
                kit[1] = makePotion(Material.SPLASH_POTION, PotionType.STRONG_HEALING);
                kit[2] = makePotion(Material.SPLASH_POTION, PotionType.STRONG_HEALING);
                kit[3] = makePotion(Material.SPLASH_POTION, PotionType.STRONG_HEALING);
                kit[4] = makePotion(Material.SPLASH_POTION, PotionType.STRONG_HEALING);
                ItemStack pearls = new ItemStack(Material.ENDER_PEARL);
                pearls.setAmount(4);
                kit[5] = pearls;
                ItemStack gaps = new ItemStack(Material.GOLDEN_APPLE);
                gaps.setAmount(4);
                kit[6] = gaps;
                kit[36] = new ItemStack(Material.NETHERITE_BOOTS);
                kit[37] = new ItemStack(Material.NETHERITE_LEGGINGS);
                kit[38] = new ItemStack(Material.NETHERITE_CHESTPLATE);
                kit[39] = new ItemStack(Material.NETHERITE_HELMET);
                // No offhand per spec.
            }
            case "spear" -> {
                // Spear PvP — trident only. Note: "spear" is community slang for a trident
                // used as a melee weapon in vanilla; there's no separate spear item.
                // Explicitly excludes mace / wind charges / elytra / fireworks per spec.
                kit[0] = new ItemStack(Material.TRIDENT);
                ItemStack gaps = new ItemStack(Material.GOLDEN_APPLE);
                gaps.setAmount(4);
                kit[1] = gaps;
                kit[36] = new ItemStack(Material.NETHERITE_BOOTS);
                kit[37] = new ItemStack(Material.NETHERITE_LEGGINGS);
                kit[38] = new ItemStack(Material.NETHERITE_CHESTPLATE);
                kit[39] = new ItemStack(Material.NETHERITE_HELMET);
                kit[40] = new ItemStack(Material.SHIELD);
            }
            case "clear" -> {
                // Empty array → everything clears.
            }
            default -> {
                return null;
            }
        }
        return kit;
    }

    /**
     * Build a potion item (regular / splash / lingering) with the given base {@link PotionType}.
     * Used by kits that need specific potion effects baked in (fire-res, strong healing, etc).
     */
    private static ItemStack makePotion(Material container, PotionType type) {
        ItemStack it = new ItemStack(container);
        if (it.getItemMeta() instanceof PotionMeta pm) {
            pm.setBasePotionType(type);
            it.setItemMeta(pm);
        }
        return it;
    }

    private static void applyLoadoutSlot(Bot bot, int slot, ItemStack item) {
        if (slot < 36) {
            // Bypass Bukkit's container-transaction system. Paper 26.x rolls
            // back PlayerInventory.setItem on the next tick when MockConnection
            // never ACKs the slot packet, which is why the mace (always placed
            // at the selected slot 0 in mace-containing kits) was vanishing
            // shortly after /bot loadout. Write straight into the NMS inventory
            // and mark it dirty.
            net.minecraft.world.entity.player.Inventory nmsInv = bot.getInventory();
            net.minecraft.world.item.ItemStack nms = (item == null || item.getType() == Material.AIR)
                    ? net.minecraft.world.item.ItemStack.EMPTY
                    : org.bukkit.craftbukkit.inventory.CraftItemStack.asNMSCopy(item);
            nmsInv.setItem(slot, nms);
            nmsInv.setChanged();
        } else if (slot == 36) {
            bot.setItem(item, EquipmentSlot.FEET);
        } else if (slot == 37) {
            bot.setItem(item, EquipmentSlot.LEGS);
        } else if (slot == 38) {
            bot.setItem(item, EquipmentSlot.CHEST);
        } else if (slot == 39) {
            bot.setItem(item, EquipmentSlot.HEAD);
        } else if (slot == 40) {
            bot.setItem(item, EquipmentSlot.OFF_HAND);
        }
    }

    private Bot findBot(String name, Location near) {
        Terminator t = manager.getFirst(name, near);
        if (t instanceof Bot b) return b;
        return null;
    }

    private double parseDoubleOrRelative(String pos, Location loc, int type) {
        if (loc == null || pos.length() == 0 || pos.charAt(0) != '~')
            return Double.parseDouble(pos);
        double relative = pos.length() == 1 ? 0 : Double.parseDouble(pos.substring(1));
        switch (type) {
            case 0:
                return relative + Math.round(loc.getX() * 1000) / 1000D;
            case 1:
                return relative + Math.round(loc.getY() * 1000) / 1000D;
            case 2:
                return relative + Math.round(loc.getZ() * 1000) / 1000D;
            default:
                return 0;
        }
    }
}
