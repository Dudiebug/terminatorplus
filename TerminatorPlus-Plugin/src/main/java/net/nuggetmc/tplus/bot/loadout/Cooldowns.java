package net.nuggetmc.tplus.bot.loadout;

import java.util.HashMap;
import java.util.Map;

/**
 * Tick-based cooldown registry keyed by string. Compares against a caller-
 * supplied {@code aliveTicks} counter so it cooperates with the existing
 * {@link net.nuggetmc.tplus.bot.Bot#getAliveTicks()} clock.
 */
public final class Cooldowns {

    private final Map<String, Integer> readyAt = new HashMap<>();

    public boolean ready(String key, int aliveTicks) {
        Integer at = readyAt.get(key);
        return at == null || aliveTicks >= at;
    }

    public void set(String key, int ticks, int aliveTicks) {
        readyAt.put(key, aliveTicks + Math.max(0, ticks));
    }

    public int remaining(String key, int aliveTicks) {
        Integer at = readyAt.get(key);
        if (at == null) return 0;
        return Math.max(0, at - aliveTicks);
    }

    public void clear(String key) {
        readyAt.remove(key);
    }
}
