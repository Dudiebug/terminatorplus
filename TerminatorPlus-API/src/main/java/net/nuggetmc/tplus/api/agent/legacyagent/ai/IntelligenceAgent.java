package net.nuggetmc.tplus.api.agent.legacyagent.ai;

import net.nuggetmc.tplus.api.AIManager;
import net.nuggetmc.tplus.api.BotManager;
import net.nuggetmc.tplus.api.Terminator;
import net.nuggetmc.tplus.api.agent.legacyagent.EnumTargetGoal;
import net.nuggetmc.tplus.api.agent.legacyagent.LegacyAgent;
import net.nuggetmc.tplus.api.agent.legacyagent.ai.movement.MovementNetwork;
import net.nuggetmc.tplus.api.agent.legacyagent.ai.movement.MovementNetworkGenetics;
import net.nuggetmc.tplus.api.agent.legacyagent.ai.movement.MovementTrainingSnapshot;
import net.nuggetmc.tplus.api.utils.ChatUtils;
import net.nuggetmc.tplus.api.utils.MathUtils;
import net.nuggetmc.tplus.api.utils.MojangAPI;
import net.nuggetmc.tplus.api.utils.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class IntelligenceAgent {

    /*
     * export all agent data to the plugin folder as separate folder things
     * commands /ai stop and /ai pause
     * if a session with name already exists keep adding underscores
     * /ai conclude or /ai finish
     * default anchor location, /ai relocateanchor
     */

    private final Plugin plugin;
    private final BotManager manager;
    private final AIManager aiManager;
    private final BukkitScheduler scheduler;

    private LegacyAgent agent;
    private Thread thread;
    private boolean active;

    private final String name;

    private final String botName;
    private final String botSkin;
    private final int cutoff;
    private final TrainingMode trainingMode;

    private final Map<String, Terminator> bots;

    private int populationSize;
    private int generation;

    private Player primary;

    private final Set<CommandSender> users;
    private final Map<Integer, Set<Map<BotNode, Map<BotDataType, Double>>>> genProfiles;
    private final Map<Integer, List<MovementNetwork>> movementGenerations;
    private final Map<Terminator, MovementFitness> movementFitness;
    private final Random random;

    public IntelligenceAgent(AIManager aiManager, int populationSize, String name, String skin, Plugin plugin, BotManager manager) {
        this(aiManager, populationSize, name, skin, plugin, manager, TrainingMode.MOVEMENT_CONTROLLER);
    }

    public IntelligenceAgent(AIManager aiManager, int populationSize, String name, String skin, Plugin plugin, BotManager manager, TrainingMode trainingMode) {
        this.plugin = plugin;
        this.manager = manager;
        this.aiManager = aiManager;
        this.scheduler = Bukkit.getScheduler();
        this.name = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(Calendar.getInstance().getTime());
        this.botName = name;
        this.botSkin = skin;
        this.bots = new HashMap<>();
        this.users = new HashSet<>(Collections.singletonList(Bukkit.getConsoleSender()));
        this.cutoff = 5;
        this.trainingMode = trainingMode == null ? TrainingMode.MOVEMENT_CONTROLLER : trainingMode;
        this.genProfiles = new HashMap<>();
        this.movementGenerations = new HashMap<>();
        this.movementFitness = new HashMap<>();
        this.populationSize = this.trainingMode == TrainingMode.MOVEMENT_CONTROLLER
                ? MovementNetworkGenetics.normalizePopulationSize(populationSize)
                : populationSize;
        this.random = new Random(Objects.hash(this.name, this.botName, this.trainingMode.name()));
        this.active = true;

        scheduler.runTaskAsynchronously(plugin, () -> {
            thread = Thread.currentThread();

            try {
                task();
            } catch (Exception e) {
                print(e);
                print("The thread has been interrupted.");
                print("The session will now close.");
                close();
            }
        });
    }

    private void task() throws InterruptedException {
        setup();
        sleep(1000);

        while (active) {
            runGeneration();
        }

        sleep(5000);
        close();
    }

    private void runGeneration() throws InterruptedException {
        generation++;

        print("Starting generation " + ChatColor.RED + generation + ChatColor.RESET + "...");

        sleep(2000);

        String skinName = botSkin == null ? this.botName : botSkin;

        print("Fetching skin data for " + ChatColor.GREEN + skinName + ChatColor.RESET + "...");

        String[] skinData = MojangAPI.getSkin(skinName);

        String botName = this.botName.endsWith("%") ? this.botName : this.botName + "%";

        print("Creating " + (populationSize == 1 ? "new bot" : ChatColor.RED + NumberFormat.getInstance(Locale.US).format(populationSize) + ChatColor.RESET + " new bots")
                + " with name " + ChatColor.GREEN + botName.replace("%", ChatColor.LIGHT_PURPLE + "%" + ChatColor.RESET)
                + (botSkin == null ? "" : ChatColor.RESET + " and skin " + ChatColor.GREEN + botSkin)
                + ChatColor.RESET + " using " + ChatColor.YELLOW + trainingMode.label()
                + ChatColor.RESET + "...");

        Set<Map<BotNode, Map<BotDataType, Double>>> loadedProfiles = genProfiles.get(generation);
        List<MovementNetwork> loadedMovementNetworks = movementGenerations.get(generation);
        Location loc = PlayerUtils.findAbove(primary.getLocation(), 20);

        scheduler.runTask(plugin, () -> {
            Set<Terminator> bots;

            if (trainingMode == TrainingMode.MOVEMENT_CONTROLLER) {
                List<MovementNetwork> movementNetworks = loadedMovementNetworks == null
                        ? MovementNetworkGenetics.randomPopulation(populationSize, random)
                        : loadedMovementNetworks;
                if (movementNetworks.size() != populationSize) {
                    print("Stored movement population size did not match; regenerating this generation safely.");
                    movementNetworks = MovementNetworkGenetics.randomPopulation(populationSize, random);
                }
                List<NeuralNetwork> networks = new ArrayList<>();
                for (MovementNetwork movementNetwork : movementNetworks) {
                    networks.add(NeuralNetwork.createMovementControllerNetwork(
                            MovementNetworkGenetics.isValid(movementNetwork)
                                    ? MovementNetworkGenetics.copy(movementNetwork)
                                    : MovementNetwork.randomDefault()));
                }

                bots = manager.createBots(loc, botName, skinData, networks);
            } else if (loadedProfiles == null) {
                bots = manager.createBots(loc, botName, skinData, populationSize, NeuralNetwork.RANDOM);
            } else {
                List<NeuralNetwork> networks = new ArrayList<>();
                loadedProfiles.forEach(profile -> networks.add(NeuralNetwork.createNetworkFromProfile(profile)));

                if (populationSize != networks.size()) {
                    print("An exception has occured.");
                    print("The stored population size differs from the size of the stored networks.");
                    close();
                    return;
                }

                bots = manager.createBots(loc, botName, skinData, networks);
            }

            bots.forEach(bot -> {
                String name = bot.getBotName();

                while (this.bots.containsKey(name)) {
                    name += "_";
                }

                this.bots.put(name, bot);
            });
        });

        while (bots.size() != populationSize) {
            sleep(1000);
        }

        sleep(2000);
        print("The bots will now attack each other.");

        agent.setTargetType(EnumTargetGoal.NEAREST_BOT);
        movementFitness.clear();

        while (aliveCount() > 1) {
            if (trainingMode == TrainingMode.MOVEMENT_CONTROLLER) {
                sampleMovementFitness();
            }
            sleep(1000);
        }
        if (trainingMode == TrainingMode.MOVEMENT_CONTROLLER) {
            sampleMovementFitness();
        }

        print("Generation " + ChatColor.RED + generation + ChatColor.RESET + " has ended.");

        if (trainingMode == TrainingMode.MOVEMENT_CONTROLLER) {
            finishMovementGeneration();
        } else {
            finishLegacyGeneration();
        }

        sleep(2000);

        clearBots();

        agent.setTargetType(EnumTargetGoal.NONE);
    }

    private void finishLegacyGeneration() throws InterruptedException {

        HashMap<Terminator, Integer> values = new HashMap<>();

        for (Terminator bot : bots.values()) {
            values.put(bot, bot.getAliveTicks());
        }

        List<Map.Entry<Terminator, Integer>> sorted = MathUtils.sortByValue(values);
        Set<Terminator> winners = new HashSet<>();

        int i = 1;

        for (Map.Entry<Terminator, Integer> entry : sorted) {
            Terminator bot = entry.getKey();
            boolean check = i <= cutoff;
            if (check) {
                print(ChatColor.GRAY + "[" + ChatColor.YELLOW + "#" + i + ChatColor.GRAY + "] " + ChatColor.GREEN + bot.getBotName()
                        + ChatUtils.BULLET_FORMATTED + ChatColor.RED + bot.getKills() + " kills");
                winners.add(bot);
            }

            i++;
        }

        sleep(3000);

        Map<BotNode, Map<BotDataType, List<Double>>> lists = new HashMap<>();

        winners.forEach(bot -> {
            Map<BotNode, Map<BotDataType, Double>> data = bot.getNeuralNetwork().values();

            data.forEach((nodeType, node) -> {
                if (!lists.containsKey(nodeType)) {
                    lists.put(nodeType, new HashMap<>());
                }

                Map<BotDataType, List<Double>> nodeValues = lists.get(nodeType);

                node.forEach((dataType, value) -> {
                    if (!nodeValues.containsKey(dataType)) {
                        nodeValues.put(dataType, new ArrayList<>());
                    }

                    nodeValues.get(dataType).add(value);
                });
            });
        });

        Set<Map<BotNode, Map<BotDataType, Double>>> profiles = new HashSet<>();

        double mutationSize = Math.pow(Math.E, 2); //MathUtils.getMutationSize(generation);

        for (int j = 0; j < populationSize; j++) {
            Map<BotNode, Map<BotDataType, Double>> profile = new HashMap<>();

            lists.forEach((nodeType, map) -> {
                Map<BotDataType, Double> points = new HashMap<>();

                map.forEach((dataType, dataPoints) -> {
                    double value = ((int) (10 * MathUtils.generateConnectionValue(dataPoints, mutationSize))) / 10D;

                    points.put(dataType, value);
                });

                profile.put(nodeType, points);
            });

            profiles.add(profile);
        }

        genProfiles.put(generation + 1, profiles);
    }

    private void finishMovementGeneration() {
        List<MovementNetworkGenetics.ScoredNetwork> scored = new ArrayList<>();
        HashMap<Terminator, Integer> displayValues = new HashMap<>();

        for (Terminator bot : bots.values()) {
            double fitness = movementFitness(bot);
            displayValues.put(bot, (int) Math.round(fitness));
            NeuralNetwork network = bot.getNeuralNetwork();
            MovementNetwork movementNetwork = network == null ? null : network.movementNetwork();
            if (MovementNetworkGenetics.isValid(movementNetwork)) {
                scored.add(new MovementNetworkGenetics.ScoredNetwork(movementNetwork, fitness));
            }
        }

        List<Map.Entry<Terminator, Integer>> sorted = MathUtils.sortByValue(displayValues);
        int rank = 1;
        for (Map.Entry<Terminator, Integer> entry : sorted) {
            if (rank > Math.min(cutoff, sorted.size())) break;
            Terminator bot = entry.getKey();
            print(ChatColor.GRAY + "[" + ChatColor.YELLOW + "#" + rank + ChatColor.GRAY + "] "
                    + ChatColor.GREEN + bot.getBotName()
                    + ChatUtils.BULLET_FORMATTED + ChatColor.RED + entry.getValue() + " fitness"
                    + ChatUtils.BULLET_FORMATTED + ChatColor.RED + bot.getKills() + " kills");
            rank++;
        }

        if (!scored.isEmpty()) {
            double best = scored.stream().mapToDouble(MovementNetworkGenetics.ScoredNetwork::fitness).max().orElse(0.0);
            double average = scored.stream().mapToDouble(MovementNetworkGenetics.ScoredNetwork::fitness).average().orElse(0.0);
            print("Movement generation " + ChatColor.RED + generation + ChatColor.RESET
                    + " best=" + ChatColor.YELLOW + MathUtils.round2Dec(best)
                    + ChatColor.RESET + " avg=" + ChatColor.YELLOW + MathUtils.round2Dec(average)
                    + ChatColor.RESET + ".");
        }

        movementGenerations.put(generation + 1,
                MovementNetworkGenetics.nextGeneration(scored, populationSize, generation, random));
    }

    private void sampleMovementFitness() {
        for (Terminator bot : bots.values()) {
            if (!bot.isBotAlive()) continue;
            Terminator targetBot = nearestAliveBot(bot);
            if (targetBot == null) continue;
            movementFitness.computeIfAbsent(bot, ignored -> new MovementFitness())
                    .sample(bot, targetBot.getBukkitEntity());
        }
    }

    private Terminator nearestAliveBot(Terminator bot) {
        Terminator best = null;
        double bestDistance = Double.MAX_VALUE;
        Location loc = bot.getLocation();
        for (Terminator other : bots.values()) {
            if (other == bot || !other.isBotAlive()) continue;
            Location otherLoc = other.getLocation();
            if (loc.getWorld() != otherLoc.getWorld()) continue;
            double distance = loc.distanceSquared(otherLoc);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = other;
            }
        }
        return best;
    }

    private double movementFitness(Terminator bot) {
        MovementFitness stats = movementFitness.get(bot);
        double fitness = bot.getAliveTicks() * 0.25
                + bot.getKills() * 275.0
                + bot.getBotHealth() * 12.0;
        if (stats != null) {
            fitness += stats.score();
        }
        if (bot.getBotMaxHealth() > 0.0f) {
            fitness -= Math.max(0.0, bot.getBotMaxHealth() - bot.getBotHealth()) * 4.0;
        }
        return fitness;
    }

    private int aliveCount() {
        return (int) bots.values().stream().filter(Terminator::isBotAlive).count();
    }

    private void close() {
        aiManager.clearSession();
        stop(); // safety call
    }

    public void stop() {
        if (this.active) {
            this.active = false;
        }

        if (!thread.isInterrupted()) {
            this.thread.interrupt();
        }
    }

    private void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    public String getName() {
        return name;
    }

    public void addUser(CommandSender sender) {
        if (users.contains(sender)) return;

        users.add(sender);
        print(sender.getName() + " has been added to the userlist.");

        if (primary == null && sender instanceof Player) {
            setPrimary((Player) sender);
        }
    }

    public void setPrimary(Player player) {
        this.primary = player;
        print(player.getName() + " has been set as the primary user.");
    }

    private void print(Object... objects) {
        String message = ChatColor.DARK_GREEN + "[REINFORCEMENT] " + ChatColor.RESET + String.join(" ", Arrays.stream(objects).map(String::valueOf).toArray(String[]::new));
        users.forEach(u -> u.sendMessage(message));
        // log -> ChatColor.stripColor(message);
    }

    private void setup() {
        clearBots();

        if (populationSize < cutoff) {
            populationSize = cutoff;
            print("The input value for the population size is lower than the cutoff (" + ChatColor.RED + cutoff + ChatColor.RESET + ")!"
                    + " The new population size is " + ChatColor.RED + populationSize + ChatColor.RESET + ".");
        }
        if (trainingMode == TrainingMode.MOVEMENT_CONTROLLER) {
            int normalized = MovementNetworkGenetics.normalizePopulationSize(populationSize);
            if (normalized != populationSize) {
                populationSize = normalized;
                print("Movement training population has been clamped to " + ChatColor.RED + populationSize + ChatColor.RESET + ".");
            }
        }

        if (!(manager.getAgent() instanceof LegacyAgent)) {
            print("The AI manager currently only supports " + ChatColor.AQUA + "LegacyAgent" + ChatColor.RESET + ".");
            close();
            return;
        }

        agent = (LegacyAgent) manager.getAgent();
        agent.setTargetType(EnumTargetGoal.NONE);

        print("The bot target goal has been set to " + ChatColor.YELLOW + EnumTargetGoal.NONE.name() + ChatColor.RESET + ".");
        print("Disabling target offsets...");

        agent.offsets = false;

        print("Disabling bot drops...");

        agent.setDrops(false);

        print(ChatColor.GREEN + "Setup is now complete.");
    }

    private void clearBots() {
        if (!bots.isEmpty()) {
            print("Removing all cached bots...");

            bots.values().forEach(Terminator::removeBot);
            bots.clear();
        }

        /*print("Removing all current bots...");

        int size = manager.fetch().size();
        manager.reset();

        String formatted = NumberFormat.getNumberInstance(Locale.US).format(size);
        print("Removed " + ChatColor.RED + formatted + ChatColor.RESET + " entit" + (size == 1 ? "y" : "ies") + ".");

        bots.clear();*/
    }

    public enum TrainingMode {
        MOVEMENT_CONTROLLER("movement-controller"),
        LEGACY_FULL_REPLACEMENT("legacy-full-replacement");

        private final String label;

        TrainingMode(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public static TrainingMode from(String raw) {
            if (raw == null || raw.isBlank()) return MOVEMENT_CONTROLLER;
            String normalized = raw.trim().toLowerCase(Locale.ROOT);
            if (normalized.equals("legacy") || normalized.equals("classic") || normalized.equals("full")
                    || normalized.equals("full-replacement")) {
                return LEGACY_FULL_REPLACEMENT;
            }
            return MOVEMENT_CONTROLLER;
        }
    }

    private static final class MovementFitness {
        private int samples;
        private int stuckSamples;
        private int oscillations;
        private int critSetups;
        private int sprintSetups;
        private int holdCompliant;
        private int holdViolations;
        private int fallbackSamples;
        private int jumpSamples;
        private int sprintSamples;
        private double rangeScore;
        private double urgencyScore;
        private double circlingScore;
        private double retreatScore;
        private double previousForwardSpeed;
        private Location previousLocation;

        void sample(Terminator bot, org.bukkit.entity.LivingEntity target) {
            MovementTrainingSnapshot snap = bot.movementTrainingSnapshot(target);
            if (!snap.available()) return;
            samples++;

            double desired = Math.max(0.75, snap.desiredRange());
            double error = Math.abs(snap.horizontalDistance() - desired);
            rangeScore += Math.max(0.0, 1.0 - error / Math.max(4.0, desired));

            if (snap.rangeUrgency() > 0.35 && snap.horizontalDistance() > desired) {
                urgencyScore += Math.min(1.0, snap.approachSpeed() / 0.42) * snap.rangeUrgency();
                if (snap.isRetreating()) urgencyScore -= snap.rangeUrgency();
            } else if (snap.horizontalDistance() < desired * 0.75 && snap.isRetreating()) {
                retreatScore += 0.5;
            }

            if (snap.isCircling() && error <= 3.0 && !snap.wantsHoldPosition()) {
                circlingScore += 0.6;
            }
            if (snap.wantsCritSetup()) {
                if (snap.legalCritSetup()) critSetups++;
                else if (snap.justJumped()) rangeScore -= 0.35;
            }
            if (snap.wantsSprintHit()) {
                if (snap.legalSprintHitSetup()) sprintSetups++;
                else if (snap.isSprinting()) rangeScore -= 0.15;
            }
            if (snap.wantsHoldPosition()) {
                if (snap.holdPositionCompliant()) holdCompliant++;
                else holdViolations++;
            }
            if (snap.movementFallback()) fallbackSamples++;
            if (snap.justJumped()) jumpSamples++;
            if (snap.isSprinting()) sprintSamples++;

            if (previousForwardSpeed != 0.0 && Math.signum(previousForwardSpeed) != Math.signum(snap.approachSpeed())) {
                oscillations++;
            }
            previousForwardSpeed = snap.approachSpeed();

            Location now = bot.getLocation();
            if (previousLocation != null && now.getWorld() == previousLocation.getWorld()
                    && now.distanceSquared(previousLocation) < 0.0025
                    && snap.rangeUrgency() > 0.25) {
                stuckSamples++;
            }
            previousLocation = now.clone();
        }

        double score() {
            if (samples == 0) return 0.0;
            double score = 0.0;
            score += (rangeScore / samples) * 300.0;
            score += (urgencyScore / samples) * 220.0;
            score += (circlingScore / samples) * 90.0;
            score += (retreatScore / samples) * 70.0;
            score += critSetups * 75.0;
            score += sprintSetups * 45.0;
            score += holdCompliant * 35.0;
            score -= holdViolations * 85.0;
            score -= fallbackSamples * 35.0;
            score -= stuckSamples * 45.0;
            score -= oscillations * 12.0;
            double jumpRate = jumpSamples / (double) samples;
            double sprintRate = sprintSamples / (double) samples;
            if (jumpRate > 0.45 && critSetups == 0) score -= (jumpRate - 0.45) * 180.0;
            if (sprintRate > 0.8 && sprintSetups == 0) score -= (sprintRate - 0.8) * 120.0;
            return score;
        }
    }
}
