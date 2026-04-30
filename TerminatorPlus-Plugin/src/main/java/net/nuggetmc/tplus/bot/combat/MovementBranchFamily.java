package net.nuggetmc.tplus.bot.combat;

import java.util.Locale;

/**
 * Stable semantic movement families emitted by CombatDirector.
 * Do not route movement brains by raw weapon names or ad hoc debug labels.
 */
public enum MovementBranchFamily {
    GENERAL_FALLBACK("general_fallback"),
    MELEE("melee"),
    MACE("mace"),
    TRIDENT_RANGED("trident_ranged"),
    SPEAR_MELEE("spear_melee"),
    MOBILITY("mobility"),
    EXPLOSIVE_SURVIVAL("explosive_survival"),
    PROJECTILE_RANGED("projectile_ranged");

    private final String id;

    MovementBranchFamily(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static MovementBranchFamily fromId(String id) {
        if (id == null || id.isBlank()) return GENERAL_FALLBACK;
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        for (MovementBranchFamily family : values()) {
            if (family.id.equals(normalized) || family.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return family;
            }
        }
        return GENERAL_FALLBACK;
    }
}
