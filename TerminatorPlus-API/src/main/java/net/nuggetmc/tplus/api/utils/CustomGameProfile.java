package net.nuggetmc.tplus.api.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import java.lang.reflect.Method;
import java.util.UUID;

public class CustomGameProfile {

    public static GameProfile create(UUID uuid, String name, String[] skin) {
        GameProfile profile = new GameProfile(uuid, name);
        if (skin != null) {
            applySkin(profile, skin[0], skin[1]);
        }
        return profile;
    }

    public static GameProfile create(UUID uuid, String name, String skinName) {
        return create(uuid, name, MojangAPI.getSkin(skinName));
    }

    private static void applySkin(GameProfile profile, String value, String signature) {
        // Newer authlib (Paper 26.x): GameProfile is a record, accessor is properties()
        // Older authlib (Paper 1.21.x): accessor is getProperties()
        Object propertyMap = invokeFirst(profile, "properties", "getProperties");
        if (propertyMap == null) return;

        Property prop = buildProperty(value, signature);
        if (prop == null) return;

        try {
            propertyMap.getClass().getMethod("put", Object.class, Object.class)
                    .invoke(propertyMap, "textures", prop);
        } catch (Exception ignored) {}
    }

    private static Object invokeFirst(Object target, String... methodNames) {
        for (String name : methodNames) {
            try {
                Method m = target.getClass().getMethod(name);
                return m.invoke(target);
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static Property buildProperty(String value, String signature) {
        // Try 3-arg constructor (name, value, signature) — both old and new authlib
        try {
            return new Property("textures", value, signature);
        } catch (Exception ignored) {}
        // Fallback: 2-arg constructor (name, value) — drop signature
        try {
            return Property.class.getConstructor(String.class, String.class)
                    .newInstance("textures", value);
        } catch (Exception ignored) {}
        return null;
    }
}
