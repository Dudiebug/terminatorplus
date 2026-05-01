package net.nuggetmc.tplus.api.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MojangAPI {

    // Cache positive Mojang API lookups so repeated /bot create <name> / loadouts
    // don't re-hit the network every time. The previous cache was disabled
    // (CACHE_ENABLED=false) because it also cached nulls from transient API
    // failures, turning a momentary hiccup into a permanent "no skin" result;
    // this rewrite only caches successful pulls.
    //
    // TODO(B-13): pullFromAPI is still blocking and is called from the main thread
    // via Bot.createBot(String)/BotManagerImpl.createBots — a Mojang API latency
    // spike still freezes the server for the first lookup. The full fix is an
    // async CompletableFuture API and hopping back to main for the caller; that's
    // a signature change through every caller so it lands in a separate commit.
    private static final int MAX_CACHE_ENTRIES = 256;
    private static final Map<String, String[]> CACHE = Collections.synchronizedMap(
            new LinkedHashMap<String, String[]>(128, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, String[]> eldest) {
                    return size() > MAX_CACHE_ENTRIES;
                }
            });
    private static final Map<String, CompletableFuture<String[]>> IN_FLIGHT = new ConcurrentHashMap<>();
    private static final ExecutorService LOOKUP_EXECUTOR = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "tplus-mojang-skin");
        thread.setDaemon(true);
        return thread;
    });

    public static String[] getSkin(String name) {
        if (name == null) return null;
        String[] cached = CACHE.get(name);
        if (cached != null) return cached;

        String[] values = pullFromAPI(name);
        if (values != null) {
            CACHE.put(name, values);
        }
        return values;
    }

    /**
     * Non-blocking skin lookup for command paths. Reuses the positive-result cache
     * and deduplicates concurrent requests for the same name.
     */
    public static CompletableFuture<String[]> getSkinAsync(String name) {
        if (name == null) {
            return CompletableFuture.completedFuture(null);
        }
        String[] cached = CACHE.get(name);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        if (LOOKUP_EXECUTOR.isShutdown()) {
            return CompletableFuture.completedFuture(getSkin(name));
        }
        return IN_FLIGHT.computeIfAbsent(name, key ->
                CompletableFuture.supplyAsync(() -> getSkin(key), LOOKUP_EXECUTOR)
                        .whenComplete((result, error) -> IN_FLIGHT.remove(key)));
    }

    // CATCHING NULL ILLEGALSTATEEXCEPTION BAD!!!! eventually fix from the getAsJsonObject thingy
    public static String[] pullFromAPI(String name) {
        try {
            String uuid = new JsonParser().parse(new InputStreamReader(new URL("https://api.mojang.com/users/profiles/minecraft/" + name)
                    .openStream())).getAsJsonObject().get("id").getAsString();
            JsonObject property = new JsonParser()
                    .parse(new InputStreamReader(new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false")
                            .openStream())).getAsJsonObject().get("properties").getAsJsonArray().get(0).getAsJsonObject();
            return new String[] {property.get("value").getAsString(), property.get("signature").getAsString()};
        } catch (IOException | IllegalStateException e) {
            return null;
        }
    }

    public static void shutdown() {
        IN_FLIGHT.values().forEach(future -> future.cancel(true));
        IN_FLIGHT.clear();
        CACHE.clear();
        LOOKUP_EXECUTOR.shutdownNow();
    }
}
