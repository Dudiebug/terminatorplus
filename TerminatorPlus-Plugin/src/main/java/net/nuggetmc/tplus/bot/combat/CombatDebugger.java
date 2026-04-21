package net.nuggetmc.tplus.bot.combat;

import net.nuggetmc.tplus.bot.Bot;
import org.bukkit.Bukkit;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Opt-in per-bot combat-path tracer. Call {@link #enable(UUID)} for a bot
 * and every event the combat pipeline emits for it will be logged through
 * Bukkit's logger.
 *
 * <p>When a bot is not enabled, {@link #isOn(Bot)} is a single hash-set
 * read and every {@code log*} method returns early — callers must still
 * avoid building payload strings before the gate, which is why the hot
 * variants take primitive/String args instead of a formatted message.
 */
public final class CombatDebugger {

    private static final Set<UUID> ENABLED = ConcurrentHashMap.newKeySet();
    private static volatile boolean allEnabled = false;

    private CombatDebugger() {}

    public static void enable(UUID bot) {
        ENABLED.add(bot);
    }

    public static void disable(UUID bot) {
        ENABLED.remove(bot);
    }

    public static void enableAll() {
        allEnabled = true;
    }

    public static void disableAll() {
        allEnabled = false;
        ENABLED.clear();
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

    private static void emit(Bot bot, String event, String detail) {
        String prefix = "[tplus-cbt] " + bot.getBotName() + " t=" + bot.getAliveTicks() + " " + event;
        if (detail.isEmpty()) {
            Bukkit.getLogger().info(prefix);
        } else {
            Bukkit.getLogger().info(prefix + " " + detail);
        }
    }

    private static String fmt(double d) {
        return String.format("%.2f", d);
    }
}
