package net.nuggetmc.tplus.api.utils;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;

import java.util.UUID;

public class CustomGameProfile {

    public static GameProfile create(UUID uuid, String name, SkinData skin) {
        PropertyMap properties = skin == null
                ? new PropertyMap(ImmutableMultimap.of())
                : new PropertyMap(ImmutableMultimap.of("textures", new Property("textures", skin.value(), skin.signature())));
        return new GameProfile(uuid, name, properties);
    }

    @Deprecated
    public static GameProfile create(UUID uuid, String name, String[] skin) {
        return create(uuid, name, SkinData.fromLegacy(skin).orElse(null));
    }

    @Deprecated
    public static GameProfile create(UUID uuid, String name, String skinName) {
        return create(uuid, name, MojangAPI.getSkin(skinName));
    }
}
