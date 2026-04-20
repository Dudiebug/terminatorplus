package net.nuggetmc.tplus.api.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import java.util.UUID;

public class CustomGameProfile {

    public static GameProfile create(UUID uuid, String name, String[] skin) {
        GameProfile profile = new GameProfile(uuid, name);
        if (skin != null) {
            profile.getProperties().put("textures", new Property("textures", skin[0], skin[1]));
        }
        return profile;
    }

    public static GameProfile create(UUID uuid, String name, String skinName) {
        return create(uuid, name, MojangAPI.getSkin(skinName));
    }
}
