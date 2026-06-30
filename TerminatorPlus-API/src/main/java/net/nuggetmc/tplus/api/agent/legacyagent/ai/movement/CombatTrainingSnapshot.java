package net.nuggetmc.tplus.api.agent.legacyagent.ai.movement;

import java.util.Locale;

public record CombatTrainingSnapshot(
        boolean available,
        String loadout,
        String loadoutFamily,
        String activeBranchFamily,
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
        int fakeActionCount,
        int instantConsumeCount,
        int illegalSameTickActionCount,
        int actionInterruptionCount,
        int healCompletionCount,
        int healCancelCount,
        LiveDuelMetricsSnapshot liveMetrics,
        String lastDamageBucket,
        String lastClassificationSource
) {
    public CombatTrainingSnapshot {
        loadout = loadout == null ? "" : loadout.trim().toLowerCase(Locale.ROOT);
        loadoutFamily = MovementTrainingConfig.normalizeFamilyId(loadoutFamily);
        activeBranchFamily = MovementTrainingConfig.normalizeFamilyId(activeBranchFamily);
        liveMetrics = liveMetrics == null ? LiveDuelMetricsSnapshot.unavailable() : liveMetrics;
        lastDamageBucket = lastDamageBucket == null || lastDamageBucket.isBlank() ? "none" : lastDamageBucket;
        lastClassificationSource = lastClassificationSource == null || lastClassificationSource.isBlank()
                ? "none"
                : lastClassificationSource;
    }

    public CombatTrainingSnapshot(
            boolean available,
            String loadout,
            String loadoutFamily,
            String activeBranchFamily,
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
            int fakeActionCount,
            int instantConsumeCount,
            int illegalSameTickActionCount,
            int actionInterruptionCount,
            String lastDamageBucket,
            String lastClassificationSource
    ) {
        this(available, loadout, loadoutFamily, activeBranchFamily, damageDealt, damageTaken,
                swordDamage, axeDamage, maceDamage, tridentDamage, spearDamage, projectileDamage,
                explosiveDamage, directDamageClassifications, heldItemDamageClassifications,
                loadoutFallbackDamageClassifications, fakeActionCount, instantConsumeCount,
                illegalSameTickActionCount, actionInterruptionCount, 0, 0,
                LiveDuelMetricsSnapshot.unavailable(), lastDamageBucket, lastClassificationSource);
    }

    public static CombatTrainingSnapshot unavailable() {
        return new CombatTrainingSnapshot(false, "", "general_fallback", "general_fallback", 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                LiveDuelMetricsSnapshot.unavailable(), "none", "none");
    }

    public double damageDelta() {
        return damageDealt - damageTaken;
    }

    public double damageRatio() {
        return damageTaken <= 0.0 ? damageDealt : damageDealt / damageTaken;
    }

    public static String familyForLoadout(String loadout) {
        if (loadout == null || loadout.isBlank()) return "general_fallback";
        return switch (loadout.trim().toLowerCase(Locale.ROOT)) {
            case "sword", "axe", "smp", "pot" -> "melee";
            case "mace" -> "mace";
            case "spear" -> "spear_melee";
            case "trident" -> "trident_ranged";
            case "projectile", "archer", "bow", "crossbow" -> "projectile_ranged";
            case "windcharge", "skydiver", "hybrid" -> "mobility";
            case "crystalpvp", "anchorbomb", "pvp" -> "explosive_survival";
            default -> "general_fallback";
        };
    }
}
