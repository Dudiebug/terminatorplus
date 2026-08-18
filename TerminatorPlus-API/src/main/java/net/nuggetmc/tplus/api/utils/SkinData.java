package net.nuggetmc.tplus.api.utils;

import java.util.Optional;

public record SkinData(String value, String signature) {

    public SkinData {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Skin texture value must not be blank");
        }
        if (signature == null || signature.isBlank()) {
            throw new IllegalArgumentException("Skin texture signature must not be blank");
        }
    }

    public static Optional<SkinData> fromLegacy(String[] skin) {
        if (skin == null || skin.length < 2) return Optional.empty();
        try {
            return Optional.of(new SkinData(skin[0], skin[1]));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public String[] toLegacyArray() {
        return new String[]{value, signature};
    }
}
