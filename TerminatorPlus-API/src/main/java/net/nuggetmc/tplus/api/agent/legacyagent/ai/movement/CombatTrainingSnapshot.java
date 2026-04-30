package net.nuggetmc.tplus.api.agent.legacyagent.ai.movement;

/**
 * Read-only combat outcome counters for movement-controller GA fitness.
 * Movement networks still do not execute combat; these values only tell the
 * trainer whether movement helped CombatDirector produce real damage.
 */
public record CombatTrainingSnapshot(
        boolean available,
        String loadout,
        String loadoutFamily,
        double damageDealt,
        double damageTaken,
        double swordDamage,
        double axeDamage,
        double maceDamage,
        double tridentDamage,
        double spearDamage,
        double projectileDamage,
        double explosiveDamage,
        int directDamageClassifications,
        int heldItemDamageClassifications,
        int loadoutFallbackDamageClassifications,
        String lastDamageBucket,
        String lastDamageClassificationSource
) {
    public CombatTrainingSnapshot {
        loadout = normalize(loadout);
        loadoutFamily = normalize(loadoutFamily);
        if (loadoutFamily.isEmpty()) {
            loadoutFamily = familyForLoadout(loadout);
        }
        lastDamageBucket = normalize(lastDamageBucket);
        if (lastDamageBucket.isEmpty()) {
            lastDamageBucket = "none";
        }
        lastDamageClassificationSource = normalize(lastDamageClassificationSource);
        if (lastDamageClassificationSource.isEmpty()) {
            lastDamageClassificationSource = "none";
        }
        directDamageClassifications = Math.max(0, directDamageClassifications);
        heldItemDamageClassifications = Math.max(0, heldItemDamageClassifications);
        loadoutFallbackDamageClassifications = Math.max(0, loadoutFallbackDamageClassifications);
    }

    public static CombatTrainingSnapshot unavailable() {
        return new CombatTrainingSnapshot(
                false,
                "",
                "",
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0,
                0,
                0,
                "none",
                "none"
        );
    }

    public static String familyForLoadout(String loadout) {
        return switch (normalize(loadout)) {
            case "crystalpvp", "anchorbomb" -> "explosive";
            case "trident", "windcharge", "skydiver" -> "ranged";
            case "hybrid", "pvp", "vanilla" -> "hybrid";
            case "sword", "axe", "smp", "mace", "spear", "pot" -> "melee";
            default -> "unknown";
        };
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
