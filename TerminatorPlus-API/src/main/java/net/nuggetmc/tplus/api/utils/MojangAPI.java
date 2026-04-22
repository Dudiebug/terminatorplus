package net.nuggetmc.tplus.api.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    private static final Map<String, String[]> CACHE = new ConcurrentHashMap<>();

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
}
