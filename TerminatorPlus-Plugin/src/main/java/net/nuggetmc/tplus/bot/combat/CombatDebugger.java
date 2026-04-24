package net.nuggetmc.tplus.bot.combat;

import net.nuggetmc.tplus.TerminatorPlus;
import net.nuggetmc.tplus.bot.Bot;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Opt-in per-bot combat/movement tracer. Call {@link #enable(UUID)} for a bot
 * and every event the pipeline emits for it will be logged to
 * {@code plugins/TerminatorPlus/debug/}.
 *
 * <p>When a bot is not enabled, {@link #isOn(Bot)} is a single hash-set
 * read and every {@code log*} method returns early — callers must still
 * avoid building payload strings before the gate, which is why the hot
 * variants take primitive/String args instead of a formatted message.
 */
public final class CombatDebugger {

    private static final Set<UUID> ENABLED = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Integer> LAST_PUNCH_TICK = new ConcurrentHashMap<>();
    private static final ExecutorService FILE_IO = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "tplus-debug-writer");
        thread.setDaemon(true);
        return thread;
    });
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static volatile Path DEBUG_DIR_CACHE;
    private static volatile boolean allEnabled = false;

    private CombatDebugger() {}

    public static void enable(UUID bot) {
        ENABLED.add(bot);
    }

    public static void disable(UUID bot) {
        ENABLED.remove(bot);
        LAST_PUNCH_TICK.remove(bot);
    }

    public static void enableAll() {
        allEnabled = true;
    }

    public static void disableAll() {
        allEnabled = false;
        ENABLED.clear();
        LAST_PUNCH_TICK.clear();
    }

    public static boolean isOn(Bot bot) {
        return allEnabled || ENABLED.contains(bot.getUUID());
    }

    public static int enabledCount() {
        return allEnabled ? -1 : ENABLED.size();
    }

    /** Generic event with no payload. */
    public static void log(Bot bot, String event) {
        if (!isOn(bot)) return;
        emit(bot, event, "");
    }

    /** Event with a single pre-formatted detail string. Caller builds the string only after gate passes in isOn-checked contexts. */
    public static void log(Bot bot, String event, String detail) {
        if (!isOn(bot)) return;
        emit(bot, event, detail);
    }

    public static void dirEntry(Bot bot, double distance, CombatState.Phase phase, boolean grounded, double vy) {
        if (!isOn(bot)) return;
        emit(bot, "dir-entry",
                "dist=" + fmt(distance) + " phase=" + phase + " grounded=" + grounded + " vy=" + fmt(vy));
    }

    public static void weaponPick(Bot bot, String weapon, double distance, boolean cdReady) {
        if (!isOn(bot)) return;
        emit(bot, "weapon-pick", "w=" + weapon + " dist=" + fmt(distance) + " cdReady=" + cdReady);
    }

    public static void dirNoop(Bot bot, double distance, String reason) {
        if (!isOn(bot)) return;
        emit(bot, "dir-noop", "dist=" + fmt(distance) + " reason=" + reason);
    }

    public static void meleeTry(Bot bot, float charge, boolean iframes, double distance) {
        if (!isOn(bot)) return;
        emit(bot, "melee-try", "charge=" + fmt(charge) + " iframes=" + iframes + " dist=" + fmt(distance));
    }

    public static void meleeHit(Bot bot, String weapon) {
        if (!isOn(bot)) return;
        emit(bot, "melee-hit", "w=" + weapon);
    }

    public static void macePhase(Bot bot, CombatState.Phase from, CombatState.Phase to) {
        if (!isOn(bot)) return;
        emit(bot, "mace-phase", "from=" + from + " to=" + to);
    }

    public static void maceCd(Bot bot, int ticksLeft) {
        if (!isOn(bot)) return;
        emit(bot, "mace-cd", "left=" + ticksLeft);
    }

    public static void maceSmash(Bot bot, double vy, boolean iframes, boolean onGround) {
        if (!isOn(bot)) return;
        emit(bot, "mace-smash", "vy=" + fmt(vy) + " iframes=" + iframes + " ground=" + onGround);
    }

    public static void swingBlock(Bot bot, String reason, float value) {
        if (!isOn(bot)) return;
        emit(bot, "swing-block", "reason=" + reason + " val=" + fmt(value));
    }

    /**
     * Full swing-decision trace: logs both passes and blocks, with the exact
     * charge / i-frame inputs used by the gate.
     */
    public static void swingGate(Bot bot, float charge, float minCharge, int iframes, boolean allowed, String reason) {
        if (!isOn(bot)) return;
        emit(bot, "swing-gate",
                "allowed=" + allowed
                        + " reason=" + reason
                        + " charge=" + fmt(charge)
                        + " need=" + fmt(minCharge)
                        + " iframes=" + iframes);
    }

    /**
     * Logs every punch animation with cadence (ticks since prior punch), held
     * item, and a best-effort source method from the current call stack.
     */
    public static void punch(Bot bot) {
        if (!isOn(bot)) return;
        UUID id = bot.getUUID();
        int now = bot.getAliveTicks();
        Integer last = LAST_PUNCH_TICK.put(id, now);
        String cadence = (last == null) ? "first" : String.valueOf(now - last);
        float charge = bot.getAttackStrengthScale(0.0f);
        ItemStack held = bot.getBukkitEntity().getInventory().getItemInMainHand();
        String heldType = (held == null) ? "AIR" : held.getType().name();
        emit(bot, "punch",
                "dt=" + cadence + " held=" + heldType + " charge=" + fmt(charge) + " src=" + inferCaller());
    }

    private static void emit(Bot bot, String event, String detail) {
        String payload = "[tplus-cbt] " + bot.getBotName() + " t=" + bot.getAliveTicks() + " " + event
                + (detail.isEmpty() ? "" : " " + detail);
        String line = TS_FORMAT.format(LocalDateTime.now()) + " " + payload;
        FILE_IO.execute(() -> writeToDebugFiles(bot, line));
    }

    private static String fmt(double d) {
        return String.format("%.2f", d);
    }

    private static String inferCaller() {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        for (StackTraceElement frame : trace) {
            String owner = frame.getClassName();
            if (owner.equals(Thread.class.getName())) continue;
            if (owner.equals(CombatDebugger.class.getName())) continue;
            if (owner.equals(Bot.class.getName()) && frame.getMethodName().equals("punch")) continue;
            String simple = owner.substring(owner.lastIndexOf('.') + 1);
            return simple + "#" + frame.getMethodName();
        }
        return "unknown";
    }

    private static void writeToDebugFiles(Bot bot, String line) {
        try {
            Path dir = resolveDebugDir();
            String safeBotName = sanitize(bot.getBotName());
            Path perBot = dir.resolve("combat-" + safeBotName + "-" + bot.getUUID() + ".log");
            Path all = dir.resolve("combat-all.log");
            appendLine(perBot, line);
            appendLine(all, line);
        } catch (Exception e) {
            // Fallback (rare): if file writes fail, still surface the trace line.
            Bukkit.getLogger().warning("[tplus-cbt] failed to write debug file: " + e.getMessage());
            Bukkit.getLogger().info(line);
        }
    }

    private static Path resolveDebugDir() throws IOException {
        Path cached = DEBUG_DIR_CACHE;
        if (cached != null) {
            return cached;
        }
        TerminatorPlus plugin = TerminatorPlus.getInstance();
        Path dir;
        if (plugin != null) {
            dir = plugin.getDataFolder().toPath().resolve("debug");
        } else {
            dir = Path.of("plugins", "TerminatorPlus", "debug");
        }
        Files.createDirectories(dir);
        DEBUG_DIR_CACHE = dir;
        return dir;
    }

    private static void appendLine(Path file, String line) throws IOException {
        Files.writeString(file, line + System.lineSeparator(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static String sanitize(String value) {
        return value.replaceAll("[^A-Za-z0-9_.-]", "_");
    }
}
