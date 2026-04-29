package net.nuggetmc.tplus.command.commands;

import net.nuggetmc.tplus.TerminatorPlus;
import net.nuggetmc.tplus.api.AIManager;
import net.nuggetmc.tplus.api.Terminator;
import net.nuggetmc.tplus.api.agent.legacyagent.ai.IntelligenceAgent;
import net.nuggetmc.tplus.api.agent.legacyagent.ai.movement.MovementBrainPersistence;
import net.nuggetmc.tplus.api.agent.legacyagent.ai.movement.MovementNetwork;
import net.nuggetmc.tplus.api.agent.legacyagent.ai.movement.MovementNetworkGenetics;
import net.nuggetmc.tplus.api.agent.legacyagent.ai.movement.MovementTrainingConfig;
import net.nuggetmc.tplus.api.agent.legacyagent.ai.NeuralNetwork;
import net.nuggetmc.tplus.api.utils.ChatUtils;
import net.nuggetmc.tplus.api.utils.MathUtils;
import net.nuggetmc.tplus.bot.BotManagerImpl;
import net.nuggetmc.tplus.command.CommandHandler;
import net.nuggetmc.tplus.command.CommandInstance;
import net.nuggetmc.tplus.command.annotation.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class AICommand extends CommandInstance implements AIManager {

    /*
     * ideas
     * ability to export neural network data to a text file, and also load from them
     * maybe also have a custom extension like .tplus and encrypt it in base64
     */

    private final TerminatorPlus plugin;
    private final BotManagerImpl manager;
    private final BukkitScheduler scheduler;
    private final Random brainRandom;

    private IntelligenceAgent agent;
    private MovementNetwork movementBrain;
    private MovementBrainPersistence.TrainingMetadata movementBrainMetadata;

    public AICommand(CommandHandler handler, String name, String description, String... aliases) {
        super(handler, name, description, aliases);

        this.plugin = TerminatorPlus.getInstance();
        this.manager = plugin.getManager();
        this.scheduler = Bukkit.getScheduler();
        this.brainRandom = new Random();
        loadMovementBrain(null, false);
    }

    @Command
    public void root(CommandSender sender, List<String> args) {
        commandHandler.sendRootInfo(this, sender);
    }

    @Command(
            name = "random",
            desc = "Create bots with random neural networks, collecting feed data."
    )
    public void random(CommandSender sender, List<String> args, @Arg("amount") int amount, @Arg("name") String name, @OptArg("skin") String skin, @OptArg("loc") @TextArg String loc) {
        if (sender instanceof Player && args.size() < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /ai random <amount> <name> [skin] [spawnLoc: [player Player]/[x,y,z]]");
            return;
        }
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
        manager.createBots(sender, name, skin, amount, NeuralNetwork.RANDOM, location);
    }

    @Command(
            name = "reinforcement",
            desc = "Begin an AI training session."
    )
    public void reinforcement(Player sender, @Arg("population-size") int populationSize, @Arg("name") String name, @OptArg("skin") String skin, @OptArg("mode") String mode) {
        //FIXME: Sometimes, bots will become invisible, or just stop working if they're the last one alive, this has been partially fixed (invis part) see Terminator#removeBot, which removes the bot.
        //This seems to fix it for the most part, but its still buggy, as the bot will sometimes still freeze
        //see https://cdn.carbonhost.cloud/6201479d7b237373ab269385/screenshots/javaw_DluMN4m0FR.png
        //Blocks are also not placeable where bots have died
        if (agent != null) {
            sender.sendMessage("A session is already active.");
            return;
        }

        sender.sendMessage("Starting a new session...");

        IntelligenceAgent.TrainingMode trainingMode = IntelligenceAgent.TrainingMode.from(mode);
        MovementNetwork seed = trainingMode == IntelligenceAgent.TrainingMode.MOVEMENT_CONTROLLER
                ? movementBrain
                : null;
        agent = new IntelligenceAgent(this, populationSize, name, skin, plugin, plugin.getManager(),
                trainingMode, seed, this::saveTrainingBrain);
        agent.addUser(sender);
    }

    public IntelligenceAgent getSession() {
        return agent;
    }

    @Command(
            name = "stop",
            desc = "End a currently running AI training session."
    )
    public void stop(CommandSender sender) {
        if (agent == null) {
            sender.sendMessage("No session is currently active.");
            return;
        }

        sender.sendMessage("Stopping the current session...");
        String name = agent.getName();
        clearSession();

        scheduler.runTaskLater(plugin, () -> sender.sendMessage("The session " + ChatColor.YELLOW + name + ChatColor.RESET + " has been closed."), 10);
    }

    @Override
    public void clearSession() {
        if (agent != null) {
            agent.stop();
            agent = null;
        }
    }

    public boolean hasActiveSession() {
        return agent != null;
    }

    @Command(
            name = "brain",
            desc = "Save, load, reset, or show the movement-controller brain.",
            autofill = "brainAutofill"
    )
    public void brain(CommandSender sender, List<String> args) {
        if (args.isEmpty() || args.get(0).equalsIgnoreCase("status")) {
            sendBrainStatus(sender);
            return;
        }

        String action = args.get(0).toLowerCase(Locale.ROOT);
        switch (action) {
            case "load" -> loadMovementBrain(sender, true);
            case "save" -> saveBrainCommand(sender, args.size() >= 2 ? args.get(1) : null);
            case "reset" -> resetMovementBrain(sender);
            default -> sender.sendMessage(ChatColor.RED + "Usage: /ai brain <status|load|save|reset> [bot-name]");
        }
    }

    @Command(
            name = "movement",
            desc = "Create movement-controller bot(s) from the persisted movement brain."
    )
    public void movement(CommandSender sender, List<String> args) {
        if (args.size() < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /ai movement <amount> <name> [skin] [spawnLoc: player|x y z [world]]");
            return;
        }

        int amount;
        try {
            amount = Math.max(1, Integer.parseInt(args.get(0)));
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Amount must be a number.");
            return;
        }

        String name = args.get(1);
        String skin = args.size() >= 3 ? args.get(2) : null;
        String locText = args.size() >= 4 ? String.join(" ", args.subList(3, args.size())) : "";
        Location location = parseLocation(sender, locText);
        if (location == null) return;

        MovementNetwork brain = ensureMovementBrain(sender);
        if (brain == null) return;

        NeuralNetwork network = NeuralNetwork.createMovementControllerNetwork(MovementNetworkGenetics.copy(brain));
        manager.createBots(sender, name, skin, amount, network, location);
        sender.sendMessage("Created movement-controller bot(s) with brain shape "
                + ChatColor.YELLOW + Arrays.toString(brain.layerSizes()) + ChatColor.RESET + ".");
    }

    @Command(
            name = "info",
            desc = "Display neural network information about a bot.",
            autofill = "infoAutofill"
    )
    public void info(CommandSender sender, @Arg("bot-name") String name) {
        sender.sendMessage("Processing request...");

        try {
            Terminator bot = manager.getFirst(name, (sender instanceof Player pl) ? pl.getLocation() : null);

            if (bot == null) {
                sender.sendMessage("Could not find bot " + ChatColor.GREEN + name + ChatColor.RESET + "!");
                return;
            }

            if (!bot.hasNeuralNetwork()) {
                sender.sendMessage("The bot " + ChatColor.GREEN + name + ChatColor.RESET + " does not have a neural network!");
                return;
            }

            NeuralNetwork network = bot.getNeuralNetwork();
            List<String> strings = new ArrayList<>();

            if (network.usesMovementController()) {
                MovementNetwork movementNetwork = network.movementNetwork();
                strings.add(ChatColor.YELLOW + "\"movement-controller\"" + ChatColor.RESET + ":");
                strings.add(ChatUtils.BULLET_FORMATTED + "shape: " + ChatColor.RED
                        + Arrays.toString(movementNetwork.layerSizes()));
                strings.add(ChatUtils.BULLET_FORMATTED + "parameters: " + ChatColor.RED
                        + movementNetwork.parameterCount());
            }

            network.nodes().forEach((nodeType, node) -> {
                strings.add("");
                strings.add(ChatColor.YELLOW + "\"" + nodeType.name().toLowerCase() + "\"" + ChatColor.RESET + ":");
                List<String> values = new ArrayList<>();
                node.getValues().forEach((dataType, value) -> values.add(ChatUtils.BULLET_FORMATTED + "node"
                        + dataType.getShorthand().toUpperCase() + ": " + ChatColor.RED + MathUtils.round2Dec(value)));
                strings.addAll(values);
            });

            sender.sendMessage(ChatUtils.LINE);
            sender.sendMessage(ChatColor.DARK_GREEN + "NeuralNetwork" + ChatUtils.BULLET_FORMATTED + ChatColor.GRAY + "[" + ChatColor.GREEN + name + ChatColor.GRAY + "]");
            strings.forEach(sender::sendMessage);
            sender.sendMessage(ChatUtils.LINE);
        } catch (Exception e) {
            sender.sendMessage(ChatUtils.EXCEPTION_MESSAGE);
        }
    }

    @Autofill
    public List<String> infoAutofill(CommandSender sender, String[] args) {
        return args.length == 2 ? manager.fetchNames() : new ArrayList<>();
    }

    @Autofill
    public List<String> brainAutofill(CommandSender sender, String[] args) {
        if (args.length == 2) return new ArrayList<>(List.of("status", "load", "save", "reset"));
        if (args.length == 3 && args[1].equalsIgnoreCase("save")) return manager.fetchNames();
        return new ArrayList<>();
    }

    private MovementTrainingConfig movementConfig() {
        return MovementTrainingConfig.load(plugin);
    }

    private MovementBrainPersistence.SaveFeedback saveTrainingBrain(
            MovementNetwork network,
            MovementBrainPersistence.TrainingMetadata metadata
    ) {
        MovementTrainingConfig config = movementConfig();
        if (!config.autosaveBestBrain()) {
            return new MovementBrainPersistence.SaveFeedback(false, "");
        }
        if (config.saveOnlyImprovedBrain()
                && movementBrainMetadata != null
                && metadata.bestFitness() <= movementBrainMetadata.bestFitness()) {
            return new MovementBrainPersistence.SaveFeedback(false, "");
        }

        MovementBrainPersistence.SaveResult result = MovementBrainPersistence.save(plugin, config, network, metadata);
        if (!result.saved()) {
            return new MovementBrainPersistence.SaveFeedback(false,
                    "Movement brain autosave failed: " + ChatColor.RED + result.message());
        }

        movementBrain = MovementNetworkGenetics.copy(network);
        movementBrainMetadata = metadata;
        return new MovementBrainPersistence.SaveFeedback(true,
                "Saved movement brain to " + ChatColor.YELLOW + result.path() + ChatColor.RESET + ".");
    }

    private void saveBrainCommand(CommandSender sender, String botName) {
        MovementNetwork brain = movementBrain;
        MovementBrainPersistence.TrainingMetadata metadata = movementBrainMetadata;

        if (botName != null && !botName.isBlank()) {
            Terminator bot = manager.getFirst(botName, sender instanceof Player player ? player.getLocation() : null);
            if (bot == null || !bot.hasNeuralNetwork() || !bot.getNeuralNetwork().usesMovementController()) {
                sender.sendMessage(ChatColor.RED + "That bot does not have a movement-controller brain.");
                return;
            }
            brain = bot.getNeuralNetwork().movementNetwork();
            metadata = MovementBrainPersistence.TrainingMetadata.manual();
        }

        if (!MovementNetworkGenetics.isValid(brain)) {
            sender.sendMessage(ChatColor.RED + "No valid movement-controller brain is loaded.");
            return;
        }

        MovementBrainPersistence.SaveResult result = MovementBrainPersistence.save(plugin, movementConfig(), brain, metadata);
        if (!result.saved()) {
            sender.sendMessage(ChatColor.RED + "Failed to save movement brain: " + result.message());
            return;
        }

        movementBrain = MovementNetworkGenetics.copy(brain);
        movementBrainMetadata = metadata == null ? MovementBrainPersistence.TrainingMetadata.manual() : metadata;
        sender.sendMessage("Saved movement brain to " + ChatColor.YELLOW + result.path() + ChatColor.RESET + ".");
    }

    private void loadMovementBrain(CommandSender sender, boolean reportMissing) {
        MovementTrainingConfig config = movementConfig();
        MovementBrainPersistence.LoadResult result = MovementBrainPersistence.load(plugin, config);
        if (result.loaded()) {
            movementBrain = MovementNetworkGenetics.copy(result.network());
            movementBrainMetadata = result.metadata();
            if (sender != null) {
                sender.sendMessage("Loaded movement brain from " + ChatColor.YELLOW + result.path()
                        + ChatColor.RESET + " shape=" + ChatColor.YELLOW
                        + Arrays.toString(movementBrain.layerSizes()) + ChatColor.RESET + ".");
            }
            return;
        }

        movementBrain = null;
        movementBrainMetadata = null;
        String message = "Movement brain not loaded from " + result.path() + ": " + result.message();
        if (sender != null) {
            if (result.missing() && !reportMissing) return;
            sender.sendMessage((result.missing() ? ChatColor.YELLOW : ChatColor.RED) + message);
            if (result.backupPath() != null) {
                sender.sendMessage("Backup: " + ChatColor.YELLOW + result.backupPath());
            }
        } else if (!result.missing() || config.enabled()) {
            plugin.getLogger().warning(message);
        }
    }

    private void resetMovementBrain(CommandSender sender) {
        MovementBrainPersistence.ResetResult result = MovementBrainPersistence.reset(plugin, movementConfig(), brainRandom);
        if (!result.reset()) {
            sender.sendMessage(ChatColor.RED + "Failed to reset movement brain: " + result.message());
            return;
        }

        movementBrain = MovementNetworkGenetics.copy(result.network());
        movementBrainMetadata = result.metadata();
        sender.sendMessage("Reset movement brain at " + ChatColor.YELLOW + result.path() + ChatColor.RESET + ".");
        if (result.backupPath() != null) {
            sender.sendMessage("Previous brain backed up to " + ChatColor.YELLOW + result.backupPath() + ChatColor.RESET + ".");
        }
    }

    private MovementNetwork ensureMovementBrain(CommandSender sender) {
        if (MovementNetworkGenetics.isValid(movementBrain)) {
            return movementBrain;
        }
        loadMovementBrain(sender, false);
        if (MovementNetworkGenetics.isValid(movementBrain)) {
            return movementBrain;
        }
        sender.sendMessage(ChatColor.YELLOW + "No valid movement brain is available; creating a fresh one.");
        resetMovementBrain(sender);
        return MovementNetworkGenetics.isValid(movementBrain) ? movementBrain : null;
    }

    private void sendBrainStatus(CommandSender sender) {
        MovementTrainingConfig config = movementConfig();
        Path path = config.brainPath(plugin);
        sender.sendMessage(ChatUtils.LINE);
        sender.sendMessage(ChatColor.DARK_GREEN + "Movement Brain" + ChatUtils.BULLET_FORMATTED
                + ChatColor.GRAY + "[" + ChatColor.YELLOW + path + ChatColor.GRAY + "]");
        sender.sendMessage(ChatUtils.BULLET_FORMATTED + "config enabled: " + ChatColor.YELLOW + config.enabled());
        sender.sendMessage(ChatUtils.BULLET_FORMATTED + "mode: " + ChatColor.YELLOW + config.mode());
        sender.sendMessage(ChatUtils.BULLET_FORMATTED + "shape: " + ChatColor.YELLOW
                + Arrays.toString(config.movementLayerShape()));
        sender.sendMessage(ChatUtils.BULLET_FORMATTED + "autosave: " + ChatColor.YELLOW
                + config.autosaveBestBrain() + ChatColor.RESET + ", save-only-improved: "
                + ChatColor.YELLOW + config.saveOnlyImprovedBrain());
        sender.sendMessage(ChatUtils.BULLET_FORMATTED + "loadouts: " + ChatColor.YELLOW + config.loadoutSummary());
        if (MovementNetworkGenetics.isValid(movementBrain)) {
            sender.sendMessage(ChatUtils.BULLET_FORMATTED + "loaded brain: " + ChatColor.GREEN + "yes"
                    + ChatColor.RESET + ", shape=" + ChatColor.YELLOW + Arrays.toString(movementBrain.layerSizes())
                    + ChatColor.RESET + ", parameters=" + ChatColor.YELLOW + movementBrain.parameterCount());
            if (movementBrainMetadata != null) {
                sender.sendMessage(ChatUtils.BULLET_FORMATTED + "generation: " + ChatColor.YELLOW
                        + movementBrainMetadata.generation() + ChatColor.RESET + ", best="
                        + ChatColor.YELLOW + MathUtils.round2Dec(movementBrainMetadata.bestFitness())
                        + ChatColor.RESET + ", avg=" + ChatColor.YELLOW
                        + MathUtils.round2Dec(movementBrainMetadata.averageFitness()));
            }
        } else {
            sender.sendMessage(ChatUtils.BULLET_FORMATTED + "loaded brain: " + ChatColor.RED + "no");
        }
        sender.sendMessage(ChatUtils.BULLET_FORMATTED + "active session: " + ChatColor.YELLOW + (agent != null));
        sender.sendMessage(ChatUtils.LINE);
    }

    private Location parseLocation(CommandSender sender, String loc) {
        Location location = sender instanceof Player player
                ? player.getLocation()
                : new Location(Bukkit.getWorlds().get(0), 0, 0, 0);
        if (loc == null || loc.isBlank()) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Spawning bot at 0, 0, 0 in world " + location.getWorld().getName()
                        + " because no location was specified.");
            }
            return location;
        }

        Player player = Bukkit.getPlayer(loc);
        if (player != null) return player.getLocation();

        String[] split = loc.split(" ");
        if (split.length < 3) {
            sender.sendMessage("The location '" + ChatColor.YELLOW + loc + ChatColor.RESET + "' is not valid!");
            return null;
        }
        try {
            double x = Double.parseDouble(split[0]);
            double y = Double.parseDouble(split[1]);
            double z = Double.parseDouble(split[2]);
            World world = Bukkit.getWorld(split.length >= 4 ? split[3] : location.getWorld().getName());
            if (world == null) {
                sender.sendMessage(ChatColor.RED + "World not found.");
                return null;
            }
            return new Location(world, x, y, z);
        } catch (NumberFormatException e) {
            sender.sendMessage("The location '" + ChatColor.YELLOW + loc + ChatColor.RESET + "' is not valid!");
            return null;
        }
    }
}
